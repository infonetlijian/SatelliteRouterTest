/**
* Copyright (C), 2016-2020, USTC.
* FileName: Test.java
* 类的详细说明
*
* @author 类创建者姓名
    * @Date    创建日期
* @version 1.00
*/

package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.util.MessageTransferAcceptPolicy;
import routing.util.RoutingInfo;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimError;

public class TestRouter extends ActiveRouter{
		
		/**自己定义的变量和映射等
		 * 
		 */
		private Map<DTNHost,Coord> coordDataMap=new HashMap<DTNHost,Coord>();//全局数据库
		private double lastUpdateClock=0;
		
		private static final double MESSAGESIZE = 1024;
		int[] predictLabel = new int[2000];
		double[] transmitDelay = new double[2000];//1000代表总的节点数
		HashMap<DTNHost, List<Integer>> routerTable = new HashMap<DTNHost, List<Integer>>();
		
		public TestRouter(Settings s){
			super(s);
		}
		protected TestRouter(TestRouter r) {
			super(r);
		}
		
	
		@Override
		public void update() {
			super.update();
			if (isTransferring() || !canStartTransfer()) {//判断能否进行传输
				return; // can't start a new transfer
			}
			
			// Try only the messages that can be delivered to final recipient
			/*if (exchangeDeliverableMessages() != null) {//若有目的节点就在本节点或者邻居节点的消息
				return; // started a transfer
			}*/
			
			List<Connection> connections = this.getHost().getConnections();  //取得所有邻居节点
			List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
				
			if (messages.size() != 0 && connections.size() != 0){
				updateRouterTable();//要传输之前，先更新路由表
				//System.out.println(this.getHost()+"  "+this.routerTable);
				for (Message m : messages){
					Tuple<Message, Connection> t = findRouterPath(m, connections);//按待发送消息顺序找路径，并尝试发送
					if (t == null){
						//记住因为可能指定的下一跳是即将到来的邻居，所以需要有一个事先判断
						if (m.getWaitLabel())
							System.out.println("wait!");//如果确实是需要等待未来的一个节点就等，先传下一个
					}
					else{
						tryMessageToConnection(t);//列表第一个元素从0指针开始！！！	
						break;//只要成功传一次，就跳出循环
					}
				}
			}
		}
		
		public Tuple<Message, Connection> findRouterPath(Message message, List<Connection> connections){		
			Connection con;
			if (message.getRouterPath() != null){//先查看信息有没有路径信息，如果有就按照已有路径信息发送，没有则查找路由表进行发送
				
				List<Integer> routerPath = message.getRouterPath();
				int thisAddress = this.getHost().getAddress();
				int nextHopAddress = -1;
				for (int i = 0; i<routerPath.size(); i++){
					if (routerPath.get(i) == thisAddress){
						nextHopAddress = routerPath.get(i+1);//找到下一跳节点地址
						break;//跳出循环
					}
				}
				if (nextHopAddress > -1){
					if (findConnection(nextHopAddress) == null){//能找到路径信息，但是却没能找到连接
						message.changeWaitLabel(true);
						System.out.println("Prediction hop!");
						return null;
					}
					else{
						con = findConnection(nextHopAddress);
					/*	if (con != null){
							Tuple<Message, Connection> tu= new Tuple<Message, Connection>(message, findConnection(nextHopAddress));
							return tu;
						}
						else
							if (predictLabel[routerPath.get(0)] == 1)
								System.out.println("Prediction!");
							throw new SimError("No such connection: "+nextHopAddress + 
									" at " + this);*/
					}
				}
			}
			else{
				if (this.routerTable.containsKey(message.getTo())){
					List<Integer> routerPath = this.routerTable.get(message.getTo());
					message.changeRouterPath(routerPath);
					
					Connection path = findConnection(routerPath.get(0));
					if (path != null){
						Tuple<Message, Connection> t= new Tuple<Message, Connection>(message, path);
						return t;
					}
					else{			
						System.out.println(this.getHost()+"  "+this.getHost().getAddress()+"  "+this.getHost().getConnections());
						System.out.println(routerPath);
						System.out.println(this.routerTable);
						System.out.println(this.getHost().getNeighbors().getNeighbors());
						System.out.println(this.getHost().getNeighbors().getNeighborsLiveTime());
						if (this.predictLabel[routerPath.get(0)] == 1)
							System.out.println("Prediction!");
						//this.routerTable.remove(message.getTo());
						
						throw new SimError("No such connection: "+ routerPath.get(0) + 
								" at routerTable " + this);					
					}
				}			
			}
			return null;//没从路由表中找到合适路径，返回空			
		}

		public Connection findConnection(int address){//根据节点地址找到，与此节点相连的连接
			List<Connection> connections = this.getHost().getConnections();
			for (Connection c : connections){
				if (c.getOtherNode(this.getHost()).getAddress() == address){
					return c;
				}
			}
			return null;//没有在已有连接中找到通过指定节点的路径
		}
		public int getSmallerNumber(int i,int j){
			if (i > j)
				return j;
			else
				return i;
		}
		
		public boolean compareAbiggerB(double i,double j){
			if (i > j)
				return true;
			else
				return false;
		}
		
		public void updateRouterTable(){//每次更新路由表
			List<Connection> connections = this.getHost().getConnections();//得到其邻居节点的连接列表
			
			this.routerTable.clear();//更新前先把路由表清空，然后重新算
			
			for (Connection c : connections){	//先把一跳内的路由添加进路由表当中
				if (c.getOtherNode(this.getHost()) != this.getHost()){
					int address = c.getOtherNode(this.getHost()).getAddress();
					DTNHost ho = c.getOtherNode(this.getHost());
					List<Integer> path =new ArrayList<Integer>();//一定要每次新new一个path，才能放进routerTable中，否则会造成引用地址相同的情况！！！
					path.add(address);
					this.routerTable.put(ho, path);//先增加邻居的路由表
					this.transmitDelay[address] = 
							MESSAGESIZE/c.getOtherNode(this.getHost()).getInterface(1).getTransmitSpeed();//以自身传输速度做一个大概估计
					
				}
			}
			
			HashMap<DTNHost, double[]> neighborsLiveTime;
			HashMap<DTNHost, double[]> potentialneighborsStartTime;
			Collection<DTNHost> hosts;
			double exist,nextHop;
			
			List<DTNHost> allHosts = this.getHost().getNeighbors().getHosts();//全部节点列表
			List<DTNHost> checkedHosts = new ArrayList<DTNHost>();
			
			Collection<DTNHost> neighborHosts = this.getHost().getNeighbors().getNeighbors();//邻居节点列表
			List<DTNHost> nextNeighborHosts = new ArrayList<DTNHost>();		
			
			nextNeighborHosts.add(this.getHost());
			checkedHosts.add(this.getHost());
			
			//while(checkedHosts.size() != (allHosts.size())){
			for (int i=0; i < 1000; i++){//修改！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
				if (nextNeighborHosts.isEmpty())
					break;
				nextNeighborHosts.clear();
				
				//System.out.println(this.getHost()+"  runing  "+i+"  "+this.routerTable);
				for (DTNHost host : neighborHosts){
					List<Connection> con = host.getConnections();//得到其邻居节点的连接列表
					for (Connection c : con){	
						if (c.getOtherNode(host) != this.getHost()){//保证不会把自身节点添加进路由表当中
							DTNHost next2Host = c.getOtherNode(host);
							if (this.routerTable.containsKey(next2Host)){//查看是否已有路由
						
								nextHop = this.transmitDelay[host.getAddress()] + 
										MESSAGESIZE/c.getSpeed();
								exist = this.transmitDelay[next2Host.getAddress()];
								if (compareAbiggerB(exist, nextHop)){//为真代表路由表已存的到h节点的时延不是最短的，需要更新，否则不用更新就什么也不用做
									List<Integer> path =new ArrayList<Integer>();
									path.addAll(this.routerTable.get(host));	//顺序不能反	
									path.add(next2Host.getAddress());				
									this.routerTable.put(next2Host, path);
									this.transmitDelay[next2Host.getAddress()] = nextHop;
									System.out.println(this.getHost()+"  contain  "+i+"  "+next2Host);
								}
							}
							else{
								List<Integer> path =new ArrayList<Integer>();
								path.addAll(this.routerTable.get(host));
								path.add(next2Host.getAddress());
								this.routerTable.put(next2Host, path);
								this.transmitDelay[next2Host.getAddress()] = this.transmitDelay[host.getAddress()] + 
										MESSAGESIZE/c.getSpeed();
								//System.out.println(this.getHost()+" not contain  "+i+"  "+host+"  "+next2Host);//host是当前节点的邻居，next2Host是当前节点邻居的邻居（2跳外的节点）
								nextNeighborHosts.add(next2Host);
							}
						}
					}
				}			
				checkedHosts.addAll(neighborHosts);
				neighborHosts.clear();
				neighborHosts.addAll(nextNeighborHosts);
			}
			
			potentialneighborsStartTime = this.getHost().getNeighbors().getPotentialNeighborsStartTime();
			hosts = this.getHost().getNeighbors().getPotentialNeighborsStartTime().keySet();
			
			for (DTNHost h : hosts){
				if (this.routerTable.containsKey(h)){	//已有到此节点的路由，就比较一下哪个用时短
					nextHop = potentialneighborsStartTime.get(h)[0] - SimClock.getTime() + 
							MESSAGESIZE/getSmallerNumber(this.getHost().getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
					exist = this.transmitDelay[h.getAddress()];
					if (compareAbiggerB(exist, nextHop)){
						List<Integer> path =new ArrayList<Integer>();
						path.add(h.getAddress());
						this.routerTable.put(h, path);
						predictLabel[h.getAddress()] = 1;
						this.transmitDelay[h.getAddress()] = nextHop;
					}
				}
				else{//没有到此跳的路由就直接添加
					List<Integer> path =new ArrayList<Integer>();
					path.add(h.getAddress());
					this.routerTable.put(h, path);
					predictLabel[h.getAddress()] = 1;
					this.transmitDelay[h.getAddress()] = potentialneighborsStartTime.get(h)[0] - SimClock.getTime() +
							MESSAGESIZE/getSmallerNumber(this.getHost().getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
				}
			}			
			
			if (this.routerTable.containsKey(this.getHost()))//保护措施
				this.routerTable.remove(this.getHost());
		}
	

			
		/*	
			//hosts = potentialneighborsStartTime.keySet();
			for (DTNHost host : hosts){
				
			}
			
			while (this.routerTable.size() != 10){//开始循环计算除了当前邻居节点的其它节点到路由表当中
				if (waitForCheckHosts.isEmpty()){
					break;
				}
				else{
					for (DTNHost host : waitForCheckHosts){//沿着邻居的邻居进行查找路径
						neighborsLiveTime = host.getNeighbors().getNeighborsLiveTime();
						potentialneighborsStartTime = host.getNeighbors().getPotentialNeighborsStartTime();
						hosts = neighborsLiveTime.keySet();
						for (DTNHost h : hosts){
							if (h != this.getHost()){//自己的节点不用加入路由表
								if (this.routerTable.containsKey(h)){							
									nextHop = this.transmitDelay[host.getAddress()] + 
											MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
									exist = this.transmitDelay[h.getAddress()];
									if (compareAbiggerB(exist, nextHop)){//为真代表路由表已存的到h节点的时延不是最短的，需要更新，否则不用更新就什么也不用做
										List<Integer> path =new ArrayList<Integer>();
										path = this.routerTable.get(host);	
										path.add(h.getAddress());
										this.routerTable.put(h, path);
										this.transmitDelay[h.getAddress()] = nextHop;
									}
								}
								else{//路由表中还没添加此节点的路由信息
									List<Integer> path =new ArrayList<Integer>();
									path = this.routerTable.get(host);
									path.add(h.getAddress());
									this.routerTable.put(h, path);
									this.transmitDelay[h.getAddress()] = this.transmitDelay[host.getAddress()] + 
											MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
								}
							}
			
						}
						hosts = potentialneighborsStartTime.keySet();//现在判断预测的即将到来的节点
						for (DTNHost h : hosts){
							if (this.routerTable.containsKey(h)){	
								nextHop = this.transmitDelay[host.getAddress()] + potentialneighborsStartTime.get(h)[0] + 
										MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
								exist = this.transmitDelay[h.getAddress()];
								if (compareAbiggerB(exist, nextHop)){
									List<Integer> path =new ArrayList<Integer>();
									path = this.routerTable.get(host);	
									path.add(h.getAddress());
									this.routerTable.put(h, path);
									predictLabel[h.getAddress()] = 1;
									this.transmitDelay[h.getAddress()] = nextHop;
								}
							}
							else{
								List<Integer> path =new ArrayList<Integer>();
								path = this.routerTable.get(host);
								path.add(h.getAddress());
								this.routerTable.put(h, path);
								predictLabel[h.getAddress()] = 1;
								this.transmitDelay[h.getAddress()] = this.transmitDelay[host.getAddress()] + potentialneighborsStartTime.get(h)[0] +
										MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
							}
						}						
					}
				}
			}
				
			
		}
		*/
		public Message tryMessageToConnection(Tuple<Message, Connection> t){//发送一个信息到特定的下一跳
			if (t == null)
				throw new SimError("No such tuple: " + 
						" at " + this);
			Message m = t.getKey();
			Connection con = t.getValue();
			int retVal = startTransfer(m, con);
			 if (retVal == RCV_OK) {  //accepted a message, don't try others
		            return m;     
		        } else if (retVal > 0) { //系统定义，只有TRY_LATER_BUSY大于0，即为1
		            return null;          // should try later -> don't bother trying others
		        }
			 return null;
		}
		
		
		
		public boolean checkIsConnected(DTNHost host){
			return this.getHost().getNeighbors().getNeighborsLiveTime().containsKey(host);
		}
		
		
		public Tuple<Message, Connection> getNextHopofMessage(
				List<Tuple<Message, Connection>> tuples) {
			//将待传输消息列表依次检查，尝试传输出去，只要有一个成功(意味着该信道被占用)，就返回。
			if (tuples.size() == 0) {
				return null;
			}
			
			for (Tuple<Message, Connection> t : tuples) {
				Message m = t.getKey();
				Connection con = t.getValue();
				if (startTransfer(m, con) == RCV_OK) {//RCV_OK==0
					return t;
				}
			}
			
			return null;
		}
		
		public void routingTable(List<DTNHost> hosts, boolean transmitSpeedisSame){
			HashMap<DTNHost,double[]> neighborsLiveTime = this.getHost().getNeighbors().getNeighborsLiveTime();//获取邻居列表和生成时间
			int nrofHosts = hosts.size();//全局节点总数
			int min = 10000000;
			int messageSize = 1024;//1024*1kB = 1mB
			int transmitTime, transmitSpeed1, transmitSpeed2;
			int minHost;
			

			List<Integer> routerPath = new ArrayList<Integer>();
			for (int i=0; i< neighborsLiveTime.size(); i++){//先只添加邻居节点到路由表当中
				routerPath.add(hosts.get(i).getAddress());
				routerTable.put(hosts.get(i), routerPath);
				routerPath.clear();
			}
			
			
			
			
			if (transmitSpeedisSame == true){
				for (int i=0; i< nrofHosts; i++){
					
				}
			}
			else{
				
				for (int i=0; i< nrofHosts; i++){
					transmitSpeed1 = hosts.get(i).getInterface(1).getTransmitSpeed();
					transmitSpeed2 = this.getHost().getInterface(1).getTransmitSpeed();
					transmitTime = messageSize/(transmitSpeed1 >= transmitSpeed2 ? transmitSpeed1 : transmitSpeed2);//连接两端的通信速率由较小的决定
					if (transmitTime < min){//标准大小数据包传输速率
						min = transmitTime;
						minHost = i;
					}
				}
				
			}
		}
		/**
		 * Start sending a message to another host.
		 * @param id Id of the message to send
		 * @param to The host to send the message to
		 */
		@Override
		public void sendMessage(String id, DTNHost to) {
			Message m = getMessage(id);
			Message m2;
			
			if (m == null) throw new SimError("no message for id " +
					id + " to send at " + this.getHost());
			assert this.getHost().getInterfaces().size() != 1 : "Over one NetworkInterface in"+this.getHost();
			assert !(this.getHost().getInterfaces().get(1).getConnections().retainAll(to.getInterfaces().get(1).getConnections())) : "The destination of message"+id
					+"is not a neighborhood of"+this.getHost();
			assert true : "test!!!";
			//System.out.print("relay!!!");
			m2 = m.replicate();	// send a replicate of the message
			to.receiveMessage(m2, this.getHost());
		}
		
		@Override
		public void transferDone(Connection con){
	
		}
		
		@Override
		public void changedConnection(Connection con) {
			// -"-
		}

		@Override
		public MessageRouter replicate() {
			return new TestRouter(this);
		}
}
