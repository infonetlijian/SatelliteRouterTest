package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import movement.MovementModel;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.Neighbors;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimError;

public class EASRRouter extends ActiveRouter{
	/**�Լ�����ı�����ӳ���
	 * 
	 */
	public static final String MSG_WAITLABEL = "waitLabel";
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	public static final String MSG_ROUTERPATH = "routerPath";  //�����ֶ����ƣ�����ΪMSG_MY_PROPERTY
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";

	private static final double SPEEDOFLIGHT = 299792458;//���٣�����3*10^8m/s
	private static final double MESSAGESIZE = 1024000;//1MB
	private static final double  HELLOINTERVAL = 30;//hello�����ͼ��
	
	int[] predictionLabel = new int[2000];
	double[] transmitDelay = new double[2000];//1000�����ܵĽڵ���
	//double[] liveTime = new double[2000];//��·������ʱ�䣬��ʼ��ʱ�Զ���ֵΪ0
	double[] endTime = new double[2000];//��·������ʱ�䣬��ʼ��ʱ�Զ���ֵΪ0
	
	private boolean msgPathLabel;//�˱�ʶָʾ�Ƿ�����Ϣͷ���б�ʶ·��·��
	private double	transmitRange;//���õĿ�ͨ�о�����ֵ
	private List<DTNHost> hosts;//ȫ�ֽڵ��б�
	
	HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();//�ڵ��·�ɱ�
	private HashMap<DTNHost, Double> helloInterval =new HashMap<DTNHost, Double>();
	private HashMap<Integer, Double> waitLabel = new HashMap<Integer, Double>();//����Ԥ���ھӵĵȴ�ʱ���Integer��ʾ�ڵ��ַ��Double��ʾ�ȴ������ʱ��
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//ָʾ��һ���ڵ㴦��æ��״̬����Ҫ�ȴ�
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//����ȫ�������ڵ��ھ���·����ʱ����Ϣ
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	/**
	 * ��ʼ��
	 * @param s
	 */
	public EASRRouter (Settings s){
		super(s);
	}
	/**
	 * ��ʼ��
	 * @param r
	 */
	protected EASRRouter(EASRRouter  r) {
		super(r);
	}
	/**
	 * ���ƴ�router��
	 */
	@Override
	public MessageRouter replicate() {
		return new EASRRouter(this);
	}
	/**
	 * ·�ɸ��£�ÿ�ε���·�ɸ���ʱ�������
	 */
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
		//this.getHost().getNeighbors().changeNeighbors(conNeighbors);
		//this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.getConnections());//�����ھӽڵ����ݿ�
		/*���Դ��룬��֤neighbors��connections��һ����*/
		
		this.hosts = this.getHost().getNeighbors().getHosts();
		List<Connection> connections = this.getConnections();  //ȡ�������ھӽڵ�
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		
		Settings s = new Settings(GROUPNAME_S);
		this.msgPathLabel = s.getBoolean(MSG_PATHLABEL);//�������ļ��ж�ȡ��������
		
		if (isTransferring()) {//�ж���·�Ƿ�ռ��
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//���ھ�ʱ��Ҫ����hello������Э��
			//helloProtocol();//ִ��hello����ά������
		}
		if (!canStartTransfer())//�Ƿ����ֽܽڵ�������Ϣ��Ҫ����
			return;
		
		//���ȫ����·״̬�����ı䣬����Ҫ���¼�������·��
		/*boolean linkStateChange = false;
		if (linkStateChange == true){
			this.busyLabel.clear();
			this.routerTable.clear();
		}*/
		if (messages.isEmpty())
			return;
		for (Message msg : messages){//���Է��Ͷ��������Ϣ	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true)
				return;
		}
	}
	/**
	 * ���˴�����Ϣmsg�Ƿ���Ҫ�ȴ����ȴ�ԭ�������1.Ŀ�Ľڵ����ڱ�ռ�ã�2.·�ɵõ���·����Ԥ��·������һ���ڵ���Ҫ�ȴ�һ��ʱ����ܵ���
	 * @param msg
	 * @return �Ƿ���Ҫ�ȴ�
	 */
	public boolean checkBusyLabelForNextHop(Message msg){
		if (this.busyLabel.containsKey(msg.getId())){
			System.out.println(this.getHost()+"  "+SimClock.getTime()+
					"  "+msg+"  is busy until  " + this.busyLabel.get(msg.getId()));
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
	 * @param msg
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections, boolean msgPathLabel){
		if (msgPathLabel == true){//�����������Ϣ��д��·����Ϣ
			if (msg.getProperty(MSG_ROUTERPATH) == null){//ͨ����ͷ�Ƿ���д��·����Ϣ���ж��Ƿ���Ҫ��������·��(ͬʱҲ������Ԥ��Ŀ���)
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, connections, msgPathLabel);
				return sendMsg(t);
			}
			else{//������м̽ڵ㣬�ͼ����Ϣ������·����Ϣ
				Tuple<Message, Connection> t = 
						findPathFromMessage(msg);
				assert t != null: "��ȡ·����Ϣʧ�ܣ�";
				return sendMsg(t);
			}
		}else{//��������Ϣ��д��·����Ϣ��ÿһ������Ҫ���¼���·��
			Tuple<Message, Connection> t = 
					findPathFromRouterTabel(msg, connections, msgPathLabel);//����������Ϣ˳����·���������Է���
			return sendMsg(t);
		}
	}
	/**
	 * ͨ����ȡ��Ϣmsgͷ�����·����Ϣ������ȡ·��·�������ʧЧ������Ҫ��ǰ�ڵ����¼���·��
	 * @param msg
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromMessage(Message msg){
		assert msg.getProperty(MSG_ROUTERPATH) != null : 
			"message don't have routerPath";//�Ȳ鿴��Ϣ��û��·����Ϣ������оͰ�������·����Ϣ���ͣ�û�������·�ɱ���з���
		List<Tuple<Integer, Boolean>> routerPath = (List<Tuple<Integer, Boolean>>)msg.getProperty(MSG_ROUTERPATH);
		
		int thisAddress = this.getHost().getAddress();
		assert msg.getTo().getAddress() != thisAddress : "���ڵ�����Ŀ�Ľڵ㣬���մ�����̴���";
		int nextHopAddress = -1;
		
		//System.out.println(this.getHost()+"  "+msg+" "+routerPath);
		boolean waitLable = false;
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i).getKey() == thisAddress){
				nextHopAddress = routerPath.get(i+1).getKey();//�ҵ���һ���ڵ��ַ
				waitLable = routerPath.get(i+1).getValue();//�ҵ���һ���Ƿ���Ҫ�ȴ��ı�־λ
				break;//����ѭ��
			}
		}
				
		if (nextHopAddress > -1){
			Connection nextCon = findConnection(nextHopAddress);
			if (nextCon == null){//���ҵ�·����Ϣ������ȴû���ҵ�����
				if (!waitLable){//����ǲ�����Ԥ���ھ���·
					System.out.println(this.getHost()+"  "+msg+" ָ��·��ʧЧ");
					msg.removeProperty(this.MSG_ROUTERPATH);//���ԭ��·����Ϣ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					Tuple<Message, Connection> t = 
							findPathFromRouterTabel(msg, this.getConnections(), true);//���ԭ��·����Ϣ֮��������Ѱ·
					return t;
				}
			}else{
				Tuple<Message, Connection> t = new 
						Tuple<Message, Connection>(msg, nextCon);
				return t;
			}
		}
		return null;	
	}

	/**
	 * ͨ������·�ɱ��ҵ���ǰ��ϢӦ��ת������һ���ڵ㣬���Ҹ���Ԥ�����þ����˼���õ���·����Ϣ�Ƿ���Ҫд����Ϣmsgͷ������
	 * @param message
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromRouterTabel(Message message, List<Connection> connections, boolean msgPathLabel){
		
		if (updateRouterTable(message) == false){//�ڴ���֮ǰ���ȸ���·�ɱ�
			return null;//��û�з���˵��һ���ҵ��˶�Ӧ·��
		}
		List<Tuple<Integer, Boolean>> routerPath = this.routerTable.get(message.getTo());
		
		if (msgPathLabel == true){//���д��·����Ϣ��־λ�棬��д��·����Ϣ
			message.updateProperty(MSG_ROUTERPATH, routerPath);
		}
					
		Connection path = findConnection(routerPath.get(0).getKey());//ȡ��һ���Ľڵ��ַ
		if (path != null){
			Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, path);//�ҵ����һ���ڵ������
			return t;
		}
		else{			
			if (routerPath.get(0).getValue()){
				System.out.println("��һ��Ԥ��");
				return null;
				//DTNHost nextHop = this.getHostFromAddress(routerPath.get(0).getKey()); 
				//this.busyLabel.put(message.getId(), startTime);//����һ���ȴ�
			}
			else{
				System.out.println(message+"  "+message.getProperty(MSG_ROUTERPATH));
				System.out.println(this.getHost()+"  "+this.getHost().getAddress()+"  "+this.getHost().getConnections());
				System.out.println(routerPath);
				System.out.println(this.routerTable);
				System.out.println(this.getHost().getNeighbors().getNeighbors());
				System.out.println(this.getHost().getNeighbors().getNeighborsLiveTime());
				throw new SimError("No such connection: "+ routerPath.get(0) + 
						" at routerTable " + this);		
			//this.routerTable.remove(message.getTo());	
			}
		}
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
	 * ����·�ɱ�����1������������·��·����2������ȫ��Ԥ��
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){
		
		this.routerTable.clear();
		PathSearch(msg);
		
		/*System.out.println(this.getHost() + "  " +SimClock.getTime()+ "  " + this.routerTable);
		for (int i = 0; i < 10; i++){
			System.out.print(this.transmitDelay[i]+"  ");
		}
		System.out.println("");
		
		for (int i = 0; i < 10; i++){
			System.out.print(this.endTime[i]+"  ");
		}
		System.out.println("");*/
		
		//updatePredictionRouter(msg);//��Ҫ����Ԥ��
		if (this.routerTable.containsKey(msg.getTo())){//Ԥ��Ҳ�Ҳ�������Ŀ�Ľڵ��·������·��ʧ��
			//m.changeRouterPath(this.routerTable.get(m.getTo()));//�Ѽ��������·��ֱ��д����Ϣ����
			return true;//�ҵ���·��
		}else{
			System.out.println("Ѱ·ʧ�ܣ�����");
			return false;
		}
		
		//if (!this.getHost().getNeighbors().getNeighbors().isEmpty())//������ڵ㲻���ڹ���״̬��������ھӽڵ��·�ɸ���
		//	;	
	}
	
	public void PathSearch(Message msg){
		Neighbors nei = this.getHost().getNeighbors();
		/*nei.updateNeighbors(this.getHost(), connections);//�����ھ��б�
		
		/*�����·��̽�⵽��һ���ھӣ�������·�ɱ�*/
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//��ʼʱֻ�б��ڵ�
		
		for (Connection con : this.getHost().getConnections()){//�����·��̽�⵽��һ���ھӣ�������·�ɱ�
			DTNHost neiHost = con.getOtherNode(this.getHost());
			sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�		
			Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
			List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
			Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
			path.add(hop);//ע��˳��
			arrivalTime.put(neiHost, time);
			routerTable.put(neiHost, path);
		}
		/*�����·��̽�⵽��һ���ھӣ�������·�ɱ�*/
		
		int iteratorTimes = 0;
		int size = this.hosts.size();
		double minTime = 100000;
		DTNHost minHost =null;
		boolean updateLabel = true;
		boolean predictLable = false;
		List<Tuple<Integer, Boolean>> minPath = new ArrayList<Tuple<Integer, Boolean>>();

		
		arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ��
		
		while(true){//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
			if (iteratorTimes >= size || updateLabel == false)
				break; 
			updateLabel = false;
			minTime = 100000;
			HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>> 
						predictionList = new HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, 
						HashMap<DTNHost, List<Double>>>>();//�洢���ƣ����֮ǰ�Ѿ�����Ͳ������ظ�������
			
			for (DTNHost host : sourceSet){
				Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime;//�ھӽڵ㵽��ʱ����뿪ʱ��Ķ�Ԫ�����
				HashMap<DTNHost, List<Double>> startTime;
				HashMap<DTNHost, List<Double>> leaveTime;
				
				if (predictionList.containsKey(host)){//�洢���ƣ����֮ǰ�Ѿ�����Ͳ������ظ�������
					predictTime = predictionList.get(host);
				}
				else{
					List<DTNHost> neiList = nei.getNeighbors(host, arrivalTime.get(host));//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)
					assert neiList==null:"û�ܳɹ���ȡ��ָ��ʱ����ھ�";
					predictTime = nei.getFutureNeighbors(neiList, host, arrivalTime.get(host));
				}
				startTime = predictTime.getKey();
				leaveTime = predictTime.getValue();
				if (startTime.keySet().isEmpty())
					continue;
				for (DTNHost neiHost : startTime.keySet()){//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
					if (sourceSet.contains(neiHost))
						continue;
					if (arrivalTime.get(host) >= SimClock.getTime() + msgTtl)//����ʱ����Ѿ�����TTLԤ��ʱ��Ļ���ֱ���ų�
						continue;
					double waitTime = startTime.get(neiHost).get(0) - arrivalTime.get(host);
					if (waitTime <= 0){
						predictLable = false;
						//System.out.println("waitTime = "+ waitTime);
						waitTime = 0;
					}
					if (waitTime > 0)
						predictLable = true;
					double time = arrivalTime.get(host) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(host))
						path.addAll(this.routerTable.get(host));
					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), predictLable);
					path.add(hop);//ע��˳��
					if (time > SimClock.getTime() + msgTtl)
						continue;
					if (time < minTime){
						minTime = time;
						minHost = neiHost;
						minPath = path;
						updateLabel = true;	
					}					
				}
			}
			if (updateLabel == true){
				arrivalTime.put(minHost, minTime);
				routerTable.put(minHost, minPath);
			}
			iteratorTimes++;
			sourceSet.clear();
			sourceSet.addAll(routerTable.keySet());//���µ���̽ڵ����
			if (routerTable.containsKey(msg.getTo()))
				break;//�����;�ҵ�����ֱ���˳�����
		}
		System.out.println(this.getHost()+" table: "+routerTable+" time : "+SimClock.getTime());
	}
	

	public int transmitFeasible(DTNHost destination){//���������,�ж��ǲ������е�Ŀ�Ľڵ��·����ͬʱ��Ҫ��֤��·���Ĵ���ʱ����ڴ�������ʱ��
		if (this.routerTable.containsKey(destination)){
			if (this.transmitDelay[destination.getAddress()] > this.endTime[destination.getAddress()] -SimClock.getTime())
				return 0;
			else
				return 1;//ֻ�д�ʱ���ҵ���ͨ��Ŀ�Ľڵ��·����ͬʱ·���ϵ���·����ʱ��������㴫����ʱ
		}
		return 2;
		
	}


	/**
	 * ����Ϣmsgͷ�����и�д��������Ԥ��ڵ�ĵȴ���־������λ
	 * @param fromHost
	 * @param host
	 * @param msg
	 * @param startTime
	 */
	public void addWaitLabelInMessage(DTNHost fromHost, DTNHost host, Message msg, double startTime){
		HashMap<DTNHost, Tuple<DTNHost, Double>> waitList = new HashMap<DTNHost, Tuple<DTNHost, Double>>();
		Tuple<DTNHost, Double> waitLabel = new Tuple<DTNHost, Double>(host, startTime);
		
		if (msg.getProperty(MSG_WAITLABEL) == null){					
			waitList.put(fromHost, waitLabel);//fromHostΪ��Ҫ�ȴ��Ľڵ㣬hostΪ��һ����Ԥ��ڵ�
			msg.addProperty(MSG_WAITLABEL, waitList);
		}else{
			waitList.putAll((HashMap<DTNHost, Tuple<DTNHost, Double>>)msg.getProperty(MSG_WAITLABEL));
			waitList.put(fromHost, waitLabel);
			msg.updateProperty(MSG_WAITLABEL, waitList);
		}
	}
	
	/**
	 * ͨ����Ϣͷ���ڵ�·����Ϣ(�ڵ��ַ)�ҵ���Ӧ�Ľڵ㣬DTNHost��
	 * @param path
	 * @return
	 */
	public List<DTNHost> getHostListFromPath(List<Integer> path){
		List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
		for (int i = 0; i < path.size(); i++){
			hostsOfPath.add(this.getHostFromAddress(path.get(i)));//���ݽڵ��ַ�ҵ�DTNHost 
		}
		return hostsOfPath;
	}
	/**
	 * ͨ���ڵ��ַ�ҵ���Ӧ�Ľڵ㣬DTNHost��
	 * @param address
	 * @return
	 */
	public DTNHost getHostFromAddress(int address){
		for (DTNHost host : this.hosts){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}
	/**
	 * ����·�ɱ�ʱ��Ԥ��ָ��·���ϵ���·����ʱ��
	 * @param formerLiveTime
	 * @param host
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
	/**
	 * ����ͨ��Ԥ��ڵ㵽�����Ĵ���ʱ��(������ʱ����ϵȴ�ʱ��)
	 * @param msgSize
	 * @param startTime
	 * @param host
	 * @param nei
	 * @return
	 */
	public double calculatePredictionDelay(int msgSize, double startTime, DTNHost host, DTNHost nei){
		if (startTime >= SimClock.getTime()){
			double waitTime;
			waitTime = startTime - SimClock.getTime() + msgSize/((nei.getInterface(1).getTransmitSpeed() > 
									host.getInterface(1).getTransmitSpeed()) ? host.getInterface(1).getTransmitSpeed() : 
										nei.getInterface(1).getTransmitSpeed()) + this.transmitRange*1000/SPEEDOFLIGHT;//ȡ���߽�С�Ĵ�������;
			return waitTime;
		}
		else{
			assert false :"Ԥ����ʧЧ ";
			return -1;
		}
	}
	/**
	 * ����ָ����·(�����ڵ�֮��)����Ĵ���ʱ��
	 * @param msgSize
	 * @param nei
	 * @param host
	 * @return
	 */
	public double calculateDelay(int msgSize, DTNHost nei , DTNHost host){
		double transmitDelay = msgSize/((nei.getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : nei.getInterface(1).getTransmitSpeed()) + 
				this.transmitDelay[host.getAddress()] + getDistance(nei, host)*1000/SPEEDOFLIGHT;//ȡ���߽�С�Ĵ�������
		return transmitDelay;
	}
	/**
	 * ���㵱ǰ�ڵ���һ���ھӵĴ�����ʱ
	 * @param msgSize
	 * @param host
	 * @return
	 */
	public double calculateNeighborsDelay(int msgSize, DTNHost host){
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
	/**
	 * ���ݽڵ��ַ�ҵ�����˽ڵ�����������
	 * @param address
	 * @return
	 */
	public Connection findConnection(int address){
		List<Connection> connections = this.getHost().getConnections();
		for (Connection c : connections){
			if (c.getOtherNode(this.getHost()).getAddress() == address){
				return c;
			}
		}
		return null;//û���������������ҵ�ͨ��ָ���ڵ��·��
	}
	/**
	 * ����һ����Ϣ���ض�����һ��
	 * @param t
	 * @return
	 */
	public Message tryMessageToConnection(Tuple<Message, Connection> t){
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
				assert this.busyLabel.get(con) == null : "error! ";
				this.busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
				System.out.println(this.getHost()+"  "+t.getKey()+"  "+
						t.getValue().getOtherNode(this.getHost())+" "+con+"  "+this.busyLabel.get(t.getKey().getId()));			
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
			assert false : "error!";//���ȷʵ����Ҫ�ȴ�δ����һ���ڵ�͵ȣ��ȴ���һ��,���޸�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
	

}
