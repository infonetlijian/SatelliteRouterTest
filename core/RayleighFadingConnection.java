/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.Random;

import routing.MessageRouter;

/**
 * A constant bit-rate connection between two DTN nodes.
 */
public class RayleighFadingConnection extends Connection {
	private int speed;
	private double transferDoneTime;
	private boolean failToTransmit;
	/**
	 * Creates a new connection between nodes and sets the connection
	 * state to "up".
	 * @param fromNode The node that initiated the connection
	 * @param fromInterface The interface that initiated the connection
	 * @param toNode The node in the other side of the connection
	 * @param toInterface The interface in the other side of the connection
	 * @param connectionSpeed Transfer speed of the connection (Bps) when 
	 *  the connection is initiated
	 */
	public RayleighFadingConnection(DTNHost fromNode, NetworkInterface fromInterface, 
			DTNHost toNode,	NetworkInterface toInterface, int connectionSpeed) {
		super(fromNode, fromInterface, toNode, toInterface);
		this.speed = connectionSpeed;
		this.transferDoneTime = 0;

	}

	/**
	 * Sets a message that this connection is currently transferring. If message
	 * passing is controlled by external events, this method is not needed
	 * (but then e.g. {@link #finalizeTransfer()} and 
	 * {@link #isMessageTransferred()} will not work either). Only a one message
	 * at a time can be transferred using one connection.
	 * @param from The host sending the message
	 * @param m The message
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int startTransfer(DTNHost from, Message m) {
		assert this.msgOnFly == null : "Already transferring " + 
			this.msgOnFly + " from " + this.msgFromNode + " to " + 
			this.getOtherNode(this.msgFromNode) + ". Can't " + 
			"start transfer of " + m + " from " + from;

		this.msgFromNode = from;
		Message newMessage = m.replicate();
		int retVal = getOtherNode(from).receiveMessage(newMessage, from);

		if (retVal == MessageRouter.RCV_OK) {
			this.msgOnFly = newMessage;
			this.transferDoneTime = SimClock.getTime() + 
			(1.0*m.getSize()) / this.speed;
		}
		transmissionSuccessfulorNot();//添加信道模型，对传输有一个估计
		return retVal;
	}
	
	/**
	 * 返回这次传输是否成功，根据瑞利衰落的模型和设定的门限值
	 * @return
	 */
	public boolean transmissionSuccessfulorNot(){
		/**添加瑞利衰落的判断**/
		Random random = new Random();
		random.nextGaussian();//实现标准正态分布
		double errorThreshold_E;
		double transmitPower_P;
		double SNR = 20;//信道的信噪比假设为20db
		double sigma = SNR/transmitPower_P;
		double lamda;
		this.failToTransmit = false;
		double noise_N = sigma*random.nextGaussian();//实现标准正态分布;
		double fadingCoefficient_h = -(1 / lamda) * Math.log(Math.random());
		int channelBandwidth_B;
		double channelCapacity_C = channelBandwidth_B*(Math.log(2)/Math.log((transmitPower_P*fadingCoefficient_h^2)/noise_N));//利用了换底公式logx(y) =loge(x) / loge(y)，换底公式
		int transmitSpeed_R = this.speed;
		if (channelCapacity_C < transmitSpeed_R)
			this.failToTransmit = true;
		/**添加瑞利衰落的判断**/
		return !this.failToTransmit;
	}
	/**
	 * Aborts the transfer of the currently transferred message.
	 */
	public void abortTransfer() {
		assert msgOnFly != null : "No message to abort at " + msgFromNode;
		getOtherNode(msgFromNode).messageAborted(this.msgOnFly.getId(),
				msgFromNode,getRemainingByteCount());
		clearMsgOnFly();
		this.transferDoneTime = 0;
	}

	/**
	 * Gets the transferdonetime
	 */
	public double getTransferDoneTime() {
		return transferDoneTime;
	}
	
	/**
	 * Returns true if the current message transfer is done.
	 * @return True if the transfer is done, false if not
	 */
	public boolean isMessageTransferred() {
		return getRemainingByteCount() == 0;
	}

	/**
	 * returns the current speed of the connection
	 */
	public double getSpeed() {
		return this.speed;
	}

	/**
	 * Returns the amount of bytes to be transferred before ongoing transfer
	 * is ready or 0 if there's no ongoing transfer or it has finished
	 * already
	 * @return the amount of bytes to be transferred
	 */
	public int getRemainingByteCount() {
		int remaining;

		if (msgOnFly == null) {
			return 0;
		}

		remaining = (int)((this.transferDoneTime - SimClock.getTime()) 
				* this.speed);

		return (remaining > 0 ? remaining : 0);
	}

	/**
	 * Returns a String presentation of the connection.
	 */
	public String toString() {
		return super.toString() + (isTransferring() ?  
				" until " + String.format("%.2f", this.transferDoneTime) : "");
	}

}
