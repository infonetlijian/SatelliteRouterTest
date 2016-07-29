/**
* Copyright (C), 2016-2020, USTC.
* FileName: Test.java
* �����ϸ˵��
*
* @author �ഴ��������
    * @Date    ��������
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
		
		/**�Լ�����ı�����ӳ���
		 * 
		 */
		private Map<DTNHost,Coord> coordDataMap=new HashMap<DTNHost,Coord>();//ȫ�����ݿ�
		private double lastUpdateClock=0;
		
		private static final double MESSAGESIZE = 1024;
		int[] predictLabel = new int[2000];
		double[] transmitDelay = new double[2000];//1000�����ܵĽڵ���
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
			if (isTransferring() || !canStartTransfer()) {//�ж��ܷ���д���
				return; // can't start a new transfer
			}
			
			// Try only the messages that can be delivered to final recipient
			/*if (exchangeDeliverableMessages() != null) {//����Ŀ�Ľڵ���ڱ��ڵ�����ھӽڵ����Ϣ
				return; // started a transfer
			}*/
			
			List<Connection> connections = this.getHost().getConnections();  //ȡ�������ھӽڵ�
			List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
				
			if (messages.size() != 0 && connections.size() != 0){
				updateRouterTable();//Ҫ����֮ǰ���ȸ���·�ɱ�
				//System.out.println(this.getHost()+"  "+this.routerTable);
				for (Message m : messages){
					Tuple<Message, Connection> t = findRouterPath(m, connections);//����������Ϣ˳����·���������Է���
					if (t == null){
						//��ס��Ϊ����ָ������һ���Ǽ����������ھӣ�������Ҫ��һ�������ж�
						if (m.getWaitLabel())
							System.out.println("wait!");//���ȷʵ����Ҫ�ȴ�δ����һ���ڵ�͵ȣ��ȴ���һ��
					}
					else{
						tryMessageToConnection(t);//�б��һ��Ԫ�ش�0ָ�뿪ʼ������	
						break;//ֻҪ�ɹ���һ�Σ�������ѭ��
					}
				}
			}
		}
		
		public Tuple<Message, Connection> findRouterPath(Message message, List<Connection> connections){		
			Connection con;
			if (message.getRouterPath() != null){//�Ȳ鿴��Ϣ��û��·����Ϣ������оͰ�������·����Ϣ���ͣ�û�������·�ɱ���з���
				
				List<Integer> routerPath = message.getRouterPath();
				int thisAddress = this.getHost().getAddress();
				int nextHopAddress = -1;
				for (int i = 0; i<routerPath.size(); i++){
					if (routerPath.get(i) == thisAddress){
						nextHopAddress = routerPath.get(i+1);//�ҵ���һ���ڵ��ַ
						break;//����ѭ��
					}
				}
				if (nextHopAddress > -1){
					if (findConnection(nextHopAddress) == null){//���ҵ�·����Ϣ������ȴû���ҵ�����
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
			return null;//û��·�ɱ����ҵ�����·�������ؿ�			
		}

		public Connection findConnection(int address){//���ݽڵ��ַ�ҵ�����˽ڵ�����������
			List<Connection> connections = this.getHost().getConnections();
			for (Connection c : connections){
				if (c.getOtherNode(this.getHost()).getAddress() == address){
					return c;
				}
			}
			return null;//û���������������ҵ�ͨ��ָ���ڵ��·��
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
		
		public void updateRouterTable(){//ÿ�θ���·�ɱ�
			List<Connection> connections = this.getHost().getConnections();//�õ����ھӽڵ�������б�
			
			this.routerTable.clear();//����ǰ�Ȱ�·�ɱ���գ�Ȼ��������
			
			for (Connection c : connections){	//�Ȱ�һ���ڵ�·����ӽ�·�ɱ���
				if (c.getOtherNode(this.getHost()) != this.getHost()){
					int address = c.getOtherNode(this.getHost()).getAddress();
					DTNHost ho = c.getOtherNode(this.getHost());
					List<Integer> path =new ArrayList<Integer>();//һ��Ҫÿ����newһ��path�����ܷŽ�routerTable�У������������õ�ַ��ͬ�����������
					path.add(address);
					this.routerTable.put(ho, path);//�������ھӵ�·�ɱ�
					this.transmitDelay[address] = 
							MESSAGESIZE/c.getOtherNode(this.getHost()).getInterface(1).getTransmitSpeed();//���������ٶ���һ����Ź���
					
				}
			}
			
			HashMap<DTNHost, double[]> neighborsLiveTime;
			HashMap<DTNHost, double[]> potentialneighborsStartTime;
			Collection<DTNHost> hosts;
			double exist,nextHop;
			
			List<DTNHost> allHosts = this.getHost().getNeighbors().getHosts();//ȫ���ڵ��б�
			List<DTNHost> checkedHosts = new ArrayList<DTNHost>();
			
			Collection<DTNHost> neighborHosts = this.getHost().getNeighbors().getNeighbors();//�ھӽڵ��б�
			List<DTNHost> nextNeighborHosts = new ArrayList<DTNHost>();		
			
			nextNeighborHosts.add(this.getHost());
			checkedHosts.add(this.getHost());
			
			//while(checkedHosts.size() != (allHosts.size())){
			for (int i=0; i < 1000; i++){//�޸ģ���������������������������������������������������������������������
				if (nextNeighborHosts.isEmpty())
					break;
				nextNeighborHosts.clear();
				
				//System.out.println(this.getHost()+"  runing  "+i+"  "+this.routerTable);
				for (DTNHost host : neighborHosts){
					List<Connection> con = host.getConnections();//�õ����ھӽڵ�������б�
					for (Connection c : con){	
						if (c.getOtherNode(host) != this.getHost()){//��֤���������ڵ���ӽ�·�ɱ���
							DTNHost next2Host = c.getOtherNode(host);
							if (this.routerTable.containsKey(next2Host)){//�鿴�Ƿ�����·��
						
								nextHop = this.transmitDelay[host.getAddress()] + 
										MESSAGESIZE/c.getSpeed();
								exist = this.transmitDelay[next2Host.getAddress()];
								if (compareAbiggerB(exist, nextHop)){//Ϊ�����·�ɱ��Ѵ�ĵ�h�ڵ��ʱ�Ӳ�����̵ģ���Ҫ���£������ø��¾�ʲôҲ������
									List<Integer> path =new ArrayList<Integer>();
									path.addAll(this.routerTable.get(host));	//˳���ܷ�	
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
								//System.out.println(this.getHost()+" not contain  "+i+"  "+host+"  "+next2Host);//host�ǵ�ǰ�ڵ���ھӣ�next2Host�ǵ�ǰ�ڵ��ھӵ��ھӣ�2����Ľڵ㣩
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
				if (this.routerTable.containsKey(h)){	//���е��˽ڵ��·�ɣ��ͱȽ�һ���ĸ���ʱ��
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
				else{//û�е�������·�ɾ�ֱ�����
					List<Integer> path =new ArrayList<Integer>();
					path.add(h.getAddress());
					this.routerTable.put(h, path);
					predictLabel[h.getAddress()] = 1;
					this.transmitDelay[h.getAddress()] = potentialneighborsStartTime.get(h)[0] - SimClock.getTime() +
							MESSAGESIZE/getSmallerNumber(this.getHost().getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
				}
			}			
			
			if (this.routerTable.containsKey(this.getHost()))//������ʩ
				this.routerTable.remove(this.getHost());
		}
	

			
		/*	
			//hosts = potentialneighborsStartTime.keySet();
			for (DTNHost host : hosts){
				
			}
			
			while (this.routerTable.size() != 10){//��ʼѭ��������˵�ǰ�ھӽڵ�������ڵ㵽·�ɱ���
				if (waitForCheckHosts.isEmpty()){
					break;
				}
				else{
					for (DTNHost host : waitForCheckHosts){//�����ھӵ��ھӽ��в���·��
						neighborsLiveTime = host.getNeighbors().getNeighborsLiveTime();
						potentialneighborsStartTime = host.getNeighbors().getPotentialNeighborsStartTime();
						hosts = neighborsLiveTime.keySet();
						for (DTNHost h : hosts){
							if (h != this.getHost()){//�Լ��Ľڵ㲻�ü���·�ɱ�
								if (this.routerTable.containsKey(h)){							
									nextHop = this.transmitDelay[host.getAddress()] + 
											MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
									exist = this.transmitDelay[h.getAddress()];
									if (compareAbiggerB(exist, nextHop)){//Ϊ�����·�ɱ��Ѵ�ĵ�h�ڵ��ʱ�Ӳ�����̵ģ���Ҫ���£������ø��¾�ʲôҲ������
										List<Integer> path =new ArrayList<Integer>();
										path = this.routerTable.get(host);	
										path.add(h.getAddress());
										this.routerTable.put(h, path);
										this.transmitDelay[h.getAddress()] = nextHop;
									}
								}
								else{//·�ɱ��л�û��Ӵ˽ڵ��·����Ϣ
									List<Integer> path =new ArrayList<Integer>();
									path = this.routerTable.get(host);
									path.add(h.getAddress());
									this.routerTable.put(h, path);
									this.transmitDelay[h.getAddress()] = this.transmitDelay[host.getAddress()] + 
											MESSAGESIZE/getSmallerNumber(host.getInterface(1).getTransmitSpeed(), h.getInterface(1).getTransmitSpeed());
								}
							}
			
						}
						hosts = potentialneighborsStartTime.keySet();//�����ж�Ԥ��ļ��������Ľڵ�
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
		public Message tryMessageToConnection(Tuple<Message, Connection> t){//����һ����Ϣ���ض�����һ��
			if (t == null)
				throw new SimError("No such tuple: " + 
						" at " + this);
			Message m = t.getKey();
			Connection con = t.getValue();
			int retVal = startTransfer(m, con);
			 if (retVal == RCV_OK) {  //accepted a message, don't try others
		            return m;     
		        } else if (retVal > 0) { //ϵͳ���壬ֻ��TRY_LATER_BUSY����0����Ϊ1
		            return null;          // should try later -> don't bother trying others
		        }
			 return null;
		}
		
		
		
		public boolean checkIsConnected(DTNHost host){
			return this.getHost().getNeighbors().getNeighborsLiveTime().containsKey(host);
		}
		
		
		public Tuple<Message, Connection> getNextHopofMessage(
				List<Tuple<Message, Connection>> tuples) {
			//����������Ϣ�б����μ�飬���Դ����ȥ��ֻҪ��һ���ɹ�(��ζ�Ÿ��ŵ���ռ��)���ͷ��ء�
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
			HashMap<DTNHost,double[]> neighborsLiveTime = this.getHost().getNeighbors().getNeighborsLiveTime();//��ȡ�ھ��б������ʱ��
			int nrofHosts = hosts.size();//ȫ�ֽڵ�����
			int min = 10000000;
			int messageSize = 1024;//1024*1kB = 1mB
			int transmitTime, transmitSpeed1, transmitSpeed2;
			int minHost;
			

			List<Integer> routerPath = new ArrayList<Integer>();
			for (int i=0; i< neighborsLiveTime.size(); i++){//��ֻ����ھӽڵ㵽·�ɱ���
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
					transmitTime = messageSize/(transmitSpeed1 >= transmitSpeed2 ? transmitSpeed1 : transmitSpeed2);//�������˵�ͨ�������ɽ�С�ľ���
					if (transmitTime < min){//��׼��С���ݰ���������
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
