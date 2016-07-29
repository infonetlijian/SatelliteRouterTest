package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import util.Tuple;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimError;

public class ClusterRouter extends ActiveRouter{
	/**�Լ�����ı�����ӳ���
	 * 
	 */
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	public static final String MSG_ROUTERPATH = "routerPath";  //�����ֶ����ƣ�����ΪMSG_MY_PROPERTY
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	private double	transmitRange;//���õĿ�ͨ�о�����ֵ
	private static final double SPEEDOFLIGHT = 299792458;//����3*10^8m/s
	private static final double MESSAGESIZE = 1024000;//1MB
	int[] predictionLabel = new int[2000];
	double[] transmitDelay = new double[2000];//1000�����ܵĽڵ���
	double[] liveTime = new double[2000];//��·������ʱ�䣬��ʼ��ʱ�Զ���ֵΪ0
	HashMap<DTNHost, List<Integer>> routerTable = new HashMap<DTNHost, List<Integer>>();
	private List<DTNHost> hosts;//ȫ�ֽڵ��б�
	private HashMap<DTNHost, Double> helloInterval =new HashMap<DTNHost, Double>();
	private static final double  HELLOINTERVAL = 30;//hello�����ͼ��
	private boolean msgPathLabel;//�˱�ʶָʾ�Ƿ�����Ϣͷ���б�ʶ·��·��
	
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();
	protected HashMap<DTNHost,HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost,HashMap<DTNHost, double[]>>();//����ȫ�������ڵ��ھ���·����ʱ����Ϣ
	protected HashMap<DTNHost, double[]> predictList = new HashMap<DTNHost, double[]>();
	
	public ClusterRouter(Settings s){
		super(s);
		

	}
	protected ClusterRouter(ClusterRouter r) {
		super(r);
	}
	@Override
	public MessageRouter replicate() {
		return new ClusterRouter(this);
	}
	
	@Override
	public void update() {
		super.update();
		
		/*���Դ��룬��֤neighbors��connections��һ����*/
		List<DTNHost> conNeighbors = new ArrayList<DTNHost>();
		for (Connection con : this.getConnections()){
			conNeighbors.add(con.getOtherNode(this.getHost()));
		}
		/*for (DTNHost host : this.getHost().getNeighbors().getNeighbors()){
			assert conNeighbors.contains(host) : "connections is not the same as neighbors";
		}
		*/
		this.getHost().getNeighbors().changeNeighbors(conNeighbors);
		this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.getConnections());//�����ھӽڵ����ݿ�
		/*���Դ��룬��֤neighbors��connections��һ����*/
		
		this.hosts = this.getHost().getNeighbors().getHosts();
		List<Connection> connections = this.getConnections();  //ȡ�������ھӽڵ�
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		
		Settings s = new Settings(GROUPNAME_S);
		this.msgPathLabel = s.getBoolean(MSG_PATHLABEL);//�������ļ��ж�ȡ��������
		
		
		
		//System.out.println(this.getHost()+" router protocol "+connections+"  "+messages);
		
		if (isTransferring()) {//�ж���·�Ƿ�ռ��
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//���ھ�ʱ��Ҫ����hello������Э��
			//helloProtocol();//ִ��hello����ά������
		}
		if (!canStartTransfer())//�Ƿ����ֽܽڵ�������Ϣ��Ҫ����
			return;
		
		for (Message msg : messages){//���Է��Ͷ��������Ϣ	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true)
				return;
		}
		/*	if (m.getFrom().getAddress() == this.getHost().getAddress()){//������Ϣ�Ƿ��Ǳ��ڵ��������Ϣ?
				if (findPathToSend(m, connections) == false)//����ʧ����ѭ���ҵ�һ���ܷ���ȥ����ϢΪֹ
					continue;
				else
					return;
			}else{//����Ϣ��ת����Ϣ��ֱ�Ӷ�ȡȥд���·��
				if (relayMessage(m, connections) == false)//ִ���м̲���
					continue;//����ʧ����ѭ���ҵ�һ���ܷ���ȥ����ϢΪֹ
				else
					return;
			}*/		
	}
	public boolean checkBusyLabelForNextHop(Message msg){
		if (this.busyLabel.containsKey(msg.getId())){
			System.out.println(this.getHost()+"  "+SimClock.getTime()+"  "+msg+"  is busy until  " + this.busyLabel.get(msg.getId()));
			if (this.busyLabel.get(msg.getId()) < SimClock.getTime()){
				this.busyLabel.remove(msg.getId());
				return false;
			}else
				return true;
		}
		return false;
	}
	/**
	 * ����·�ɱ�Ѱ��·��������ת����Ϣ
	 * @param m
	 * @param connections
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections, boolean msgPathLabel){
		if (msgPathLabel == true){//�����������Ϣ��д��·����Ϣ
			if (msg.getFrom() == this.getHost()){
				Tuple<Message, Connection> t = findPathFromRouterTabel(msg, connections, msgPathLabel, false);
				return sendMsg(t);
			}
			else{//������м̽ڵ㣬�ͼ����Ϣ������·����Ϣ
				Tuple<Message, Connection> t = findPathFromMessage(msg);
				assert t != null: "��ȡ·����Ϣʧ�ܣ�";
				return sendMsg(t);
			}
		}else{//��������Ϣ��д��·����Ϣ��ÿһ������Ҫ���¼���·��
			Tuple<Message, Connection> t = findPathFromRouterTabel(msg, connections, msgPathLabel, false);//����������Ϣ˳����·���������Է���
			return sendMsg(t);
		}
	}
	
	public Tuple<Message, Connection> findPathFromMessage(Message msg){
		assert msg.getProperty(MSG_ROUTERPATH) != null : "message don't have routerPath";//�Ȳ鿴��Ϣ��û��·����Ϣ������оͰ�������·����Ϣ���ͣ�û�������·�ɱ���з���
		List<Integer> routerPath = (List<Integer>)msg.getProperty(MSG_ROUTERPATH);
		int thisAddress = this.getHost().getAddress();
		assert msg.getTo().getAddress() != thisAddress : "���ڵ�����Ŀ�Ľڵ㣬���մ�����̴���";
		int nextHopAddress = -1;
		
		System.out.print(this.getHost()+"  "+msg+" "+routerPath);
		
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i) == thisAddress){
				nextHopAddress = routerPath.get(i+1);//�ҵ���һ���ڵ��ַ
				break;//����ѭ��
			}
		}
		if (nextHopAddress > -1){
			if (findConnection(nextHopAddress) == null){//���ҵ�·����Ϣ������ȴû���ҵ�����
				System.out.print(this.getHost()+"  "+msg+" ָ��·��ʧЧ");
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, this.getConnections(), true, true);
						//�������Ϊ����·��ʧЧ������·�ɣ�����Ҫ�ɸ���λ��λ���������message.addProperty�ᱨ��
				return t;
			}
		}
		return null;	
	}


	public Tuple<Message, Connection> findPathFromRouterTabel(Message message, List<Connection> connections, boolean msgPathLabel, boolean updateLabel){
		if (updateRouterTable(message) == false){//�ڴ���֮ǰ���ȸ���·�ɱ�
			return null;
		}
		if (this.routerTable.containsKey(message.getTo())){
			List<Integer> routerPath = this.routerTable.get(message.getTo());

			
			if (msgPathLabel == true){//���д��·����Ϣ��־λ�棬��д��·����Ϣ
				if (updateLabel == true)
					message.updateProperty(MSG_ROUTERPATH, routerPath);
				else
					message.updateProperty(MSG_ROUTERPATH, routerPath);//����Ϣ�����·����Ϣ
			}
			
			
			Connection path = findConnection(routerPath.get(0));
			if (path != null){
				Tuple<Message, Connection> t= new Tuple<Message, Connection>(message, path);
				return t;
			}
			else{			
				System.out.println(message);
				System.out.println(this.getHost()+"  "+this.getHost().getAddress()+"  "+this.getHost().getConnections());
				System.out.println(routerPath);
				System.out.println(this.routerTable);
				System.out.println(this.getHost().getNeighbors().getNeighbors());
				System.out.println(this.getHost().getNeighbors().getNeighborsLiveTime());
				if (this.predictionLabel[routerPath.get(0)] == 1)
					System.out.println("Prediction!");
				//this.routerTable.remove(message.getTo());
				
				throw new SimError("No such connection: "+ routerPath.get(0) + 
						" at routerTable " + this);					
			}
		}
		return null;
	}
	/**
	 * �ɽڵ��ַ�ҵ���Ӧ�Ľڵ�DTNHost
	 * @param address
	 * @return
	 */
	public DTNHost findHostByAddress(int address){
		for (DTNHost host : this.hosts){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}
	/**
	 * ����һ���ڵ��ַѰ�Ҷ�Ӧ���ھ�����
	 * @param address
	 * @return
	 */
	public Connection findConnectionByAddress(int address){
		for (Connection con : this.getHost().getConnections()){
			if (con.getOtherNode(this.getHost()).getAddress() == address)
				return con;
		}
		return null;
	}
	/**
	 * ����ȫ�ֹ��Ԥ��͵�ǰ��·����
	 */
	public void updateGlobalInfo(){
		List<DTNHost> hosts = this.getHost().getNeighbors().getHosts();
	
		for (DTNHost host : hosts){//����������ڵ���ھӺ�Ԥ��
			if (host != this.getHost()){
				host.getNeighbors().updateNeighbors(host, host.getConnections());
				this.neighborsList.put(host, host.getNeighbors().getNeighborsLiveTime());
			}
		}
		this.predictList = this.getHost().getNeighbors().getPotentialNeighborsStartTime();
	}
	/**
	 * HelloЭ��
	 */
	public void helloProtocol(){
		for (Connection con : this.getHost().getInterface(1).getConnections()){
			if (con.getOtherNode(this.getHost()) != this.getHost()){
				double nextTime = helloInterval.get(con.getOtherNode(this.getHost()));
				if (SimClock.getTime() >= nextTime){
					Message m = new Message(this.getHost(), con.getOtherNode(this.getHost()), "hello", 1000);//����һ��hello��Ϣ
					Tuple<Message, Connection> t = new Tuple<Message, Connection>(m, con);
					if (tryMessageToConnection(t) != null)
						helloInterval.put(con.getOtherNode(this.getHost()), nextTime + HELLOINTERVAL);
					
				}
			}
		}
	}
	/**
	 * ����·�ɱ�����1������������·��·����2������ȫ��Ԥ��
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){

		if (!this.getHost().getNeighbors().getNeighbors().isEmpty()){//������ڵ㲻���ڹ���״̬��������ھӽڵ��·�ɸ���
			
			updateGlobalInfo();//����ȫ��Ԥ����Ϣ			
			this.routerTable.clear();
			
			updateNeighborsRouter(msg);//���µ�ǰ�ѽ������ӵ�·��
			
			/*System.out.println(this.getHost() + "  " + this.routerTable);
			for (int i = 0; i < 10; i++){
				System.out.print(this.transmitDelay[i]+"  ");
			}
			System.out.println("");
			
			for (int i = 0; i < 10; i++){
				System.out.print(this.liveTime[i]+"  ");
			}
			System.out.println("");*/
			
			updatePredictionRouter(msg);//��Ҫ����Ԥ��
			if (this.routerTable.containsKey(msg.getTo())){//Ԥ��Ҳ�Ҳ�������Ŀ�Ľڵ��·������·��ʧ��
				//m.changeRouterPath(this.routerTable.get(m.getTo()));//�Ѽ��������·��ֱ��д����Ϣ����
				return true;//�ҵ���·��
			}else
				return false;
		}
		else
			return false;
		
	}
	/**
	 * ��·�ɱ�������µı���
	 * @param host
	 * @param path
	 * @param Delay
	 * @param predictionLable������˽ڵ��Ƿ���Ԥ�⵽����ھӽڵ�
	 */
	public void addRouterTable(DTNHost host, List<Integer> path, double Delay, double liveTime, boolean predictionLable){
		this.routerTable.put(host, path);
		this.transmitDelay[host.getAddress()] = Delay;
		this.liveTime[host.getAddress()] = liveTime;
		if (predictionLable)
			predictionLabel[host.getAddress()] = 1;//��Ԥ��λ��λ
		else
			predictionLabel[host.getAddress()] = 0;//��Ԥ��λ��λ
	}
	/**
	 * ��û��ֱ��������·�Ľڵ����Ԥ��
	 */
	public void updatePredictionRouter(Message msg){
		int msgSize = msg.getSize();
		Settings s = new Settings(INTERFACENAME_S);
		this.transmitRange = s.getDouble(TRANSMIT_RANGE_S);//�������ļ��ж�ȡ��������
		
		Collection<DTNHost> itsPotentialNeighbors = this.predictList.keySet();
		for (DTNHost host : itsPotentialNeighbors){
			double[] startTime = this.predictList.get(host);
			double waitTime = calculatePredictionDelay(msgSize, startTime[0], host);//����ȴ��˽ڵ㵽����Ҫ�೤ʱ��
			if (waitTime > 0){
				List<Integer> path = new ArrayList<Integer>();
				path.add(host.getAddress());
				if (this.transmitDelay[host.getAddress()] != 0){//����˽ڵ����ҵ���һ��·��������ͨ����������host�ڵ�
					if (waitTime < this.transmitDelay[host.getAddress()]){//��ͨ����������ڵ�host����ʱ��ȵȴ�host��Ϊ�ھ�����ʱ�仹Ҫ��
						if (startTime[1] != startTime[0]){//����Ⱦ�˵���˽ڵ��п��ܳ�Ϊδ�����ھӣ�����飡��������������������������������������������������������������������������������
							addRouterTable(host, path, waitTime, startTime[1], true);//���������·�ɱ�,ͬʱ��Ԥ��λΪ1
						}

					}
				}else{//˵������˽ڵ�ֻ�е�δ�����߽ӽ����п���ͨ��									
					if (startTime[1] != startTime[0])//����Ⱦ�˵���˽ڵ��п��ܳ�Ϊδ�����ھӣ�����飡��������������������������������������������������������������������������������
						addRouterTable(host, path, waitTime, startTime[1], true);//���������·�ɱ�,ͬʱ��Ԥ��λΪ1
				}
			}else{
				assert false : "�����Ԥ��ʱ�������ϣ�";
			}
		}
	}
	public int transmitFeasible(DTNHost destination){//���������,�ж��ǲ������е�Ŀ�Ľڵ��·����ͬʱ��Ҫ��֤��·���Ĵ���ʱ����ڴ�������ʱ��
		if (this.routerTable.containsKey(destination)){
			if (this.transmitDelay[destination.getAddress()] > this.liveTime[destination.getAddress()])
				return 0;
			else
				return 1;//ֻ�д�ʱ���ҵ���ͨ��Ŀ�Ľڵ��·����ͬʱ·���ϵ���·����ʱ��������㴫����ʱ
		}
		return 2;
		
	}
	public void updateNeighborsRouter(Message msg){
		int msgSize = msg.getSize();
		HashMap<DTNHost,HashMap<DTNHost, double[]>> totalNeighborsList = this.neighborsList;//�������������ӵĽڵ�

		List<DTNHost> neighbors = this.getHost().getNeighbors().getNeighbors();	
		
		for (DTNHost host : neighbors){//����һ���Ľڵ�
			List<Integer> path = new ArrayList<Integer>();
			path.add(host.getAddress());
			double liveTime = this.getHost().getNeighbors().getNeighborsLiveTime().get(host)[1] - SimClock.getTime();
			//double liveTime = calculateExistTime(100000, this.getHost(), path);
			addRouterTable(host, path, calculateNeighborsDelay(msgSize, host), liveTime, false);//���������·�ɱ�,false������Ԥ�⵽����ھӽڵ�
		}
		
		if (transmitFeasible(msg.getTo()) == 1)
			return;//�ж��Ƿ��ܹ���������·�д����ȥ
		if (transmitFeasible(msg.getTo()) == 0)//˵���ھ���·���Ͼ�Ҫ�ߣ���ʱӦ��������һ���µĵ���˽ڵ��·��
			this.routerTable.remove(msg.getTo());//��ֱ�ӵ���˽ڵ��һ��·��ɾ�����ú����㷨�������µ�·��
		
		Collection<DTNHost> restHosts = totalNeighborsList.keySet();//�����Լ������������еĽڵ�
		List<Integer> minPath = new ArrayList<Integer>();
		DTNHost minFromHost = null, minHost = null;
		double transmitTime, minLiveTime = -2, minTransmitTime = 100000000;
		int size = restHosts.size();
		
		//System.out.println(restHosts);
		
		int iteratorTimes = 0;
		boolean hasNeighborsLabel = true;
		boolean updateLabel = true;
		
		while(true){//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
			if (iteratorTimes >= size || hasNeighborsLabel == false || updateLabel == false || restHosts.isEmpty())
				break;
			hasNeighborsLabel = false;//����ָʾĳһ�ڵ���ĩ�˽ڵ�(��ֻ��һ����·)
			updateLabel = false;
			restHosts = this.routerTable.keySet();
			for (DTNHost host : restHosts){
				if (!host.getConnections().isEmpty()){//���ĳ���ڵ㱻�����ˣ���û���ھӽڵ��������������				
					if (this.routerTable.containsKey(host)){//���Ѿ����ھӽڵ�Ľڵ㣬�൱�ڴӽڵ��ھӵ��ھӲ������ų�ȥ
						Collection<DTNHost> itsNeighbors = totalNeighborsList.get(host).keySet();
						if (!itsNeighbors.isEmpty()){
							for (DTNHost nei : itsNeighbors){//host�ڵ�������ھӽڵ�
								if (!this.routerTable.containsKey(nei) && nei.getAddress() != this.getHost().getAddress()){//������������·����
									hasNeighborsLabel = true;
									transmitTime = calculateDelay(msgSize, nei , host);//�������ڵ�֮��Ĵ�����ʱ��˳���ܷ���hostΪ����·�ɱ��ڵ����·���ڵ㣬nei��host���ھ�
									
									List<Integer> path = new ArrayList<Integer>();
									path.addAll(this.routerTable.get(host));
									path.add(nei.getAddress());//ע��˳��
									
									double existTime = totalNeighborsList.get(host).get(nei)[1] - SimClock.getTime();
									existTime = (liveTime[host.getAddress()] > existTime) ? existTime : liveTime[host.getAddress()];
									
									//double existTime = calculateExistTime(liveTime[host.getAddress()], host, path);
									//System.out.println(getDistance(nei, host)+" "+nei.getLocation()+"  "+host.getLocation());//������
									if (transmitTime < minTransmitTime && existTime >= transmitTime){//����飬������·����ʱ��ʹ���ʱ��֮����жϣ�������������������������
										updateLabel = true;
										minTransmitTime = transmitTime;
										minPath = path;
										minHost = nei;
										minFromHost = host;
										minLiveTime = existTime;
									}	
								}
							}
						}
					}
				}			
			}
				//System.out.print(minFromHost+"  "+minHost+"  ");//������
				//System.out.println(minPath);//������
			if (minHost == null){
				assert false : "minHost is empty!!! in "+this.getHost();
				break;
			}else{
				addRouterTable(minHost, minPath, minTransmitTime, minLiveTime, false);//���������·�ɱ�,false������Ԥ�⵽����ھӽڵ�
				if (transmitFeasible(msg.getTo()) == 1)//�ж��Ƿ��ܹ���������·�д����ȥ
					return;//�����;�Ѿ��ҵ���·������ֱ�ӷ��ز��ü���Ѱ��
				//if (transmitFeasible(msg.getTo()) == 0)//�ж��Ƿ��ܹ���������·�д����ȥ
				//	return;//�����;�Ѿ��ҵ���·������ֱ�ӷ��ز��ü���Ѱ��
				minTransmitTime = 100000000;
				minFromHost = null;
				minHost = null;
			}	
			iteratorTimes++;
		}
		/*//������нڵ�û�ҵ�·����һ���Ǵ��ڹ���״̬,����Ԥ��
		if (!restHosts.isEmpty()){
			System.out.println("can't find path  "+ restHosts);
		}*/	
	}
	public List<DTNHost> getHostListFromPath(List<Integer> path){
		List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
		for (int i = 0; i < path.size(); i++){
			hostsOfPath.add(this.getHostFromAddress(path.get(i)));//���ݽڵ��ַ�ҵ�DTNHost 
		}
		return hostsOfPath;
	}
	/**
	 * ����·�ɱ�ʱ��Ԥ��ָ��·���ϵ���·����ʱ��
	 * @param host ָ�����һ��
	 * @param path
	 * @return
	 */
	public double calculateExistTime(double formerLiveTime, DTNHost host, List<Integer> path){
		DTNHost formerHost, nextHost;
		double existTime , minTime;

		nextHost = this.getHostFromAddress(path.get(0));
		//System.out.println(host+"  "+host.getNeighbors().getNeighborsLiveTime()+"  "+this.neighborsList.get(host)+"  "+host.getNeighbors().getNeighborsLiveTime().get(nextHost)[1]+"  "+path+" "+nextHost);

		existTime = this.neighborsList.get(host).get(nextHost)[1] - SimClock.getTime();
		minTime = formerLiveTime > existTime ? existTime : formerLiveTime;			
		if (path.size() > 1){//���ٳ���Ϊ2
			for (int i = 1; i < path.size() - 1; i++){
				if (i > path.size() -1)//�������ȣ��Զ�����
					return minTime;
				formerHost = nextHost;
				nextHost = this.getHostFromAddress(path.get(i));
				existTime = this.neighborsList.get(formerHost).get(nextHost)[1] - SimClock.getTime();
				if (existTime < minTime)
					minTime = existTime;
			}
		}				
	
	return minTime;
	}
	public DTNHost getHostFromAddress(int address){
		for (DTNHost host : this.hosts){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}
	public double calculatePredictionDelay(int msgSize, double startTime, DTNHost host){
		if (startTime >= SimClock.getTime()){
			double waitTime;
			waitTime = startTime - SimClock.getTime() + MESSAGESIZE/((this.getHost().getInterface(1).getTransmitSpeed() > 
									host.getInterface(1).getTransmitSpeed()) ? host.getInterface(1).getTransmitSpeed() : 
										this.getHost().getInterface(1).getTransmitSpeed()) + this.transmitRange*1000/SPEEDOFLIGHT;//ȡ���߽�С�Ĵ�������;
			return waitTime;
		}
		else{
			assert false :"Ԥ����ʧЧ ";
			return -1;
		}
	}
	public double calculateDelay(int msgSize, DTNHost nei , DTNHost host){
		double transmitDelay = msgSize/((nei.getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : nei.getInterface(1).getTransmitSpeed()) + 
				this.transmitDelay[host.getAddress()] + getDistance(nei, host)*1000/SPEEDOFLIGHT;//ȡ���߽�С�Ĵ�������
		return transmitDelay;
	}
	public double calculateNeighborsDelay(int msgSize, DTNHost host){//����һ���ھӵĴ�����ʱ
		double transmitDelay = msgSize/((this.getHost().getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : this.getHost().getInterface(1).getTransmitSpeed()) + getDistance(this.getHost(), host)*1000/SPEEDOFLIGHT;//ȡ���߽�С�Ĵ�������
		return transmitDelay;
	}
	
	/**
	 * ���������ڵ�֮��ľ���
	 * @param a
	 * @param b
	 * @return
	 */
	public double getDistance(DTNHost a, DTNHost b){
		double ax = a.getLocation().getX();
		double ay = a.getLocation().getY();
		double az = a.getLocation().getZ();
		double bx = a.getLocation().getX();
		double by = a.getLocation().getY();
		double bz = a.getLocation().getZ();
		
		double distance = (ax - bx)*(ax - bx) + (ay - by)*(ay - by) + (az - bz)*(az - bz);
		distance = Math.sqrt(distance);
		
		return distance;
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
	/**
	 * �����ж���һ���ڵ��Ƿ��ڷ��ͻ����״̬
	 * @param t
	 * @return
	 */
	public boolean hostIsBusyOrNot(Tuple<Message, Connection> t){
		List<Connection> connections = t.getValue().getOtherNode(this.getHost()).getConnections();
		for (Connection con : connections){
			if (con.isTransferring()){
				busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
				System.out.println(this.getHost()+"  "+t.getValue().getOtherNode(this.getHost())+" "+con+"  "+busyLabel.get(t.getKey().getId()));			
				return true;//˵��Ŀ�Ľڵ���æ
			}
		}
		return false;
	}
	/**
	 * �Ӹ�����Ϣ��ָ����·�����Է�����Ϣ
	 * @param t
	 * @return
	 */
	public boolean sendMsg(Tuple<Message, Connection> t){
		if (t == null){	
			//assert message.getProperty(MSG_WAITLABEL) == null : "wait!";//���ȷʵ����Ҫ�ȴ�δ����һ���ڵ�͵ȣ��ȴ���һ��,���޸�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			return false;
		}
		else{
			if (hostIsBusyOrNot(t) == true)//����Ŀ�Ľڵ㴦��æ��״̬
				return false;//����ʧ�ܣ���Ҫ�ȴ�
			if (tryMessageToConnection(t) != null)//�б��һ��Ԫ�ش�0ָ�뿪ʼ������	
				return true;//ֻҪ�ɹ���һ�Σ�������ѭ��
			else
				return false;
		}
	}
	/**
	 * Returns true if this router is transferring something at the moment or
	 * some transfer has not been finalized.
	 * @return true if this router is transferring something
	 */
	@Override
	public boolean isTransferring() {
		//�жϸýڵ��ܷ���д�����Ϣ�������������һ�����ϵģ�ֱ�ӷ��أ�������,�������ŵ��ѱ�ռ�ã�
		//����1�����ڵ��������⴫��
		if (this.sendingConnections.size() > 0) {//protected ArrayList<Connection> sendingConnections;
			return true; // sending something
		}
		
		List<Connection> connections = getConnections();
		//����2��û���ھӽڵ�
		if (connections.size() == 0) {
			return false; // not connected
		}
		//����3�����ھӽڵ㣬����������Χ�ڵ����ڴ���
		//ģ�������߹㲥��·�����ھӽڵ�֮��ͬʱֻ����һ�Խڵ㴫������!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//��Ҫ�޸�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			if (!con.isReadyForTransfer()) {//isReadyForTransfer����false���ʾ���ŵ��ڱ�ռ�ã���˶��ڹ㲥�ŵ����Բ��ܴ���
				return true;	// a connection isn't ready for new transfer
			}
		}
		
		return false;		
	}
	/**
	 * ����д������֤�ڴ������֮��Դ�ڵ����Ϣ��messages������ɾ��
	 */
	@Override
	protected void transferDone(Connection con){
		String msgId = con.getMessage().getId();
		removeFromMessages(msgId);
	}

	
	/*public boolean createNewMessage(Message msg){
		List<Integer> routerPath = new ArrayList<Integer>();	

	    makeRoomForNewMessage(msg.getSize());
	    
	    msg.setTtl(this.msgTtl);
	    msg.addProperty(MSG_ROUTERPATH, routerPath); //�������һ��
	    addToMessages(msg, true);
	    return true;
	}*/
}
