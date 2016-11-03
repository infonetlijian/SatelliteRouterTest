package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import movement.MovementModel;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimError;

public class GridRouter extends ActiveRouter{
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
	private HashMap<Integer, Double> waitLabel = new HashMap<Integer, Double>();//����Ԥ���ھӵĵȴ�ʱ�����Integer��ʾ�ڵ��ַ��Double��ʾ�ȴ������ʱ��
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//ָʾ��һ���ڵ㴦��æ��״̬����Ҫ�ȴ�
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//����ȫ�������ڵ��ھ���·����ʱ����Ϣ
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	
	private boolean routerTableUpdateLabel;
	private List<DTNHost> totalhosts;
	private GridNeighbors GN;
	Random random = new Random();
	/**
	 * ��ʼ��
	 * @param s
	 */
	public GridRouter(Settings s){
		super(s);
	}
	/**
	 * ��ʼ��
	 * @param r
	 */
	protected GridRouter(GridRouter r) {
		super(r);
		this.GN = new GridNeighbors(this.getHost());
	}
	/**
	 * ���ƴ�router��
	 */
	@Override
	public MessageRouter replicate() {
		return new GridRouter(this);
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
		
		routerTableUpdateLabel = false;
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
	 * ����·�ɱ���Ѱ��·��������ת����Ϣ
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
			"message don't have routerPath";//�Ȳ鿴��Ϣ��û��·����Ϣ������оͰ�������·����Ϣ���ͣ�û�������·�ɱ����з���
		List<Tuple<Integer, Boolean>> routerPath = (List<Tuple<Integer, Boolean>>)msg.getProperty(MSG_ROUTERPATH);
		
		int thisAddress = this.getHost().getAddress();
		assert msg.getTo().getAddress() != thisAddress : "���ڵ�����Ŀ�Ľڵ㣬���մ������̴���";
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
	 * ͨ������·�ɱ����ҵ���ǰ��ϢӦ��ת������һ���ڵ㣬���Ҹ���Ԥ�����þ����˼���õ���·����Ϣ�Ƿ���Ҫд����Ϣmsgͷ������
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
				System.out.println(message+"  "+message.getProperty(MSG_WAITLABEL));
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
	 * ����·�ɱ�������1������������·��·����2������ȫ��Ԥ��
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){
		
		
		
		gridSearch(msg);
		
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
			System.out.println("Ѱ·�ɹ�������");
			return true;//�ҵ���·��
		}else{
			//System.out.println("Ѱ·ʧ�ܣ�����");
			return false;
		}
		
		//if (!this.getHost().getNeighbors().getNeighbors().isEmpty())//������ڵ㲻���ڹ���״̬��������ھӽڵ��·�ɸ���
		//	;	
	}
	
	public void gridSearch(Message msg){
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true������˴θ���·�ɱ��Ѿ����¹��ˣ����Բ�Ҫ�ظ�����
			return;
		this.routerTable.clear();
		
		if (GN.isHostsListEmpty()){
			GN.setHostsList(hosts);
		}
		//GridNeighbors GN = this.getHost().getGridNeighbors();
		GN.updateGrid();//���������
		
		/*������·��̽�⵽��һ���ھӣ�������·�ɱ�*/
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//��ʼʱֻ�б��ڵ�
		
		for (Connection con : this.getHost().getConnections()){//������·��̽�⵽��һ���ھӣ�������·�ɱ�
			DTNHost neiHost = con.getOtherNode(this.getHost());
			sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�		
			Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
			List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
			Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
			path.add(hop);//ע��˳��
			arrivalTime.put(neiHost, time);
			routerTable.put(neiHost, path);
		}
		/*������·��̽�⵽��һ���ھӣ�������·�ɱ�*/
		
		int iteratorTimes = 0;
		int size = this.hosts.size();
		double minTime = 100000;
		DTNHost minHost =null;
		boolean updateLabel = true;
		boolean predictLable = false;
		List<Tuple<Integer, Boolean>> minPath = new ArrayList<Tuple<Integer, Boolean>>();

		
		arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ��
		
		while(true){//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ�����֤·�ɱ�����Զ��ʱ����С��·��
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
					List<DTNHost> neiList = GN.getNeighbors(host, arrivalTime.get(host));//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)
					assert neiList==null:"û�ܳɹ���ȡ��ָ��ʱ����ھ�";
					predictTime = GN.getFutureNeighbors(neiList, host, arrivalTime.get(host));
				}
				startTime = predictTime.getKey();
				leaveTime = predictTime.getValue();
				if (startTime.keySet().isEmpty())
					continue;
				for (DTNHost neiHost : startTime.keySet()){//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
					if (sourceSet.contains(neiHost))
						continue;
					if (arrivalTime.get(host) >= SimClock.getTime() + msgTtl*60)//����ʱ����Ѿ�����TTLԤ��ʱ��Ļ���ֱ���ų�
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
					/*���޸ģ�Ӧ����leavetime�ļ��*/
					if (time > SimClock.getTime() + msgTtl*60)
						continue;
					/*���޸ģ�Ӧ����leavetime�ļ��*/
					if (time <= minTime){
						if (random.nextBoolean() == true && time - minTime < 1){
							minTime = time;
							minHost = neiHost;
							minPath = path;
							updateLabel = true;	
						}
					}					
				}
			}
			if (updateLabel == true){
				arrivalTime.put(minHost, minTime);
				routerTable.put(minHost, minPath);
			}
			iteratorTimes++;
			sourceSet.add(minHost);//���µ���̽ڵ����
			if (routerTable.containsKey(msg.getTo()))//�����;�ҵ���Ҫ��·������ֱ���˳�����
				break;
		}
		routerTableUpdateLabel = true;
		
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
			if (tryMessageToConnection(t) != null)//�б���һ��Ԫ�ش�0ָ�뿪ʼ������	
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
	public void setTotalHosts(List<DTNHost> hosts){
		this.totalhosts=hosts;
	}
	
	public class GridNeighbors {
		
		private List<DTNHost> hosts = new ArrayList<DTNHost>();//ȫ�����ǽڵ��б�
		private DTNHost host;
		private double transmitRange;
		private double msgTtl;
		
		private double updateInterval = 1;
		
		private GridCell[][][] cells;//GridCell����࣬����һ��ʵ������һ����������������world������һ����ά����洢�������ÿ���������ִ洢�˵�ǰ�����е�host��networkinterface
		private HashMap<NetworkInterface, GridCell> ginterfaces;
		private int cellSize;
		private int rows;
		private int cols;
		private int zs;//������ά����
		private  int worldSizeX;
		private  int worldSizeY;
		private  int worldSizeZ;//����
		
		private List<GridCell[][][]> GridList = new ArrayList<GridCell[][][]>();
		private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();
		private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
		
		public GridNeighbors(DTNHost host){
			this.host = host;
			Settings se = new Settings("Interface");
			transmitRange = se.getDouble("transmitRange");//�������ļ��ж�ȡ��������
			Settings set = new Settings("Group");
			msgTtl = set.getDouble("msgTtl");
			
			Settings s = new Settings(MovementModel.MOVEMENT_MODEL_NS);
			int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,2);//������2ά�޸�Ϊ3ά
			worldSizeX = worldSize[0];
			worldSizeY = worldSize[1];
			worldSizeZ = worldSize[1];//������ά����������飡��������������������������������������������������������������������������������������������
			
			//cellSize = (int) (transmitRange*0.5773502);
			cellSize = (int) (transmitRange*0.288);//Layer=2
			//cellSize = (int) (transmitRange*0.14433);//Layer=3
			//cellSize = (int) (transmitRange*0.0721687);//Layer=4
			CreateGrid(cellSize);
			
		}

		public DTNHost getHost(){
			return this.host;
		}
		
		public void CreateGrid(int cellSize){
			this.rows = worldSizeY/cellSize + 1;
			this.cols = worldSizeX/cellSize + 1;
			this.zs = worldSizeZ/cellSize + 1;//����
			System.out.println(cellSize+"  "+this.rows+"  "+this.cols+"  "+this.zs);
			// leave empty cells on both sides to make neighbor search easier 
			this.cells = new GridCell[rows+2][cols+2][zs+2];
			this.cellSize = cellSize;

			for (int i=0; i<rows+2; i++) {
				for (int j=0; j<cols+2; j++) {
					for (int n=0;n<zs+2; n++){//������ά����
						this.cells[i][j][n] = new GridCell();
						cells[i][j][n].setNumber(i, j, n);
					}
				}
			}
			ginterfaces = new HashMap<NetworkInterface,GridCell>();
		}
		
		public List<DTNHost> getNeighbors(DTNHost host, double time){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;
			
			if (time > SimClock.getTime()+msgTtl*60){//��������ʱ���Ƿ񳬹�Ԥ��ʱ��
				//assert false :"����Ԥ��ʱ��";
				time = SimClock.getTime()+msgTtl*60;
			}
				
			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			GridCell cell = ginterfaces.get(host.getInterface(1));
			int[] number = cell.getNumber();
			
			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
			List<DTNHost> hostList = new ArrayList<DTNHost>();//(�ھ������ڵĽڵ㼯��)
			assert cellmap.containsKey(time):" ʱ����� ";
			for (GridCell c : cellList){
				if (cellmap.get(time).containsKey(c))//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
					hostList.addAll(cellmap.get(time).get(c));
			}	
			if (hostList.contains(host))//�������ڵ�ȥ��
				hostList.remove(host);
			//System.out.println(host+" �ھ��б�   "+hostList);
			return hostList;
		}

		public Tuple<HashMap<DTNHost, List<Double>>, //neiList Ϊ�Ѿ�������ĵ�ǰ�ھӽڵ��б�
			HashMap<DTNHost, List<Double>>> getFutureNeighbors(List<DTNHost> neiList, DTNHost host, double time){
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;	
			
			HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
			HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
			for (DTNHost neiHost : neiList){
				List<Double> t= new ArrayList<Double>();
				t.add(SimClock.getTime());
				startTime.put(neiHost, t);//�����Ѵ����ھӽڵ�Ŀ�ʼʱ��
			}
			
			List<DTNHost> futureList = new ArrayList<DTNHost>();//(�ھ������ڵĽڵ㼯��)
			List<NetworkInterface> futureNeiList = new ArrayList<NetworkInterface>();//(Ԥ��δ���ھӵĽڵ㼯��)
			
			for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
				
				HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//ȡ��timeʱ�̵������
				GridCell cell = ginterfaces.get(host.getInterface(1));//�ҵ���ʱָ���ڵ�����������λ��
				
				int[] number = cell.getNumber();
				List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//��ȡ�����ھӵ����񣨵�ǰʱ�̣�
				
				for (GridCell c : cellList){	//�����ڲ�ͬʱ��ά���ϣ�ָ���ڵ���Χ������ھ�
					if (!cellmap.get(time).containsKey(c))
						continue;
					for (DTNHost ni : cellmap.get(time).get(c)){//��鵱ǰԤ��ʱ��㣬���е��ھӽڵ�
						if (ni == this.host)//�ų������ڵ�
							continue;
						if (!neiList.contains(ni))//��������ھ���û�У���һ����δ����������ھ�					
							futureList.add(ni); //��Ϊδ�����ᵽ����ھ�(��Ȼ���ڵ�ǰ���е��ھӣ�Ҳ���ܻ���;�뿪��Ȼ���ٻ���)
						
						/**�����δ��������ھӣ�ֱ��get�᷵�ؿ�ָ�룬����Ҫ�ȼ�startTime��leaveTime�ж�**/
						if (startTime.containsKey(ni)){
							/*if (leaveTime.containsKey(ni)){//�����������һ����Ԥ��ʱ����ڴ��ھӻ��뿪����һ������Ǵ��ھӲ����ڴ�ʱ����ڻ��뿪�������
								if (startTime.get(ni).size() == leaveTime.get(ni).size()){//����������һ�����ھӽڵ��뿪�����					
									List<Double> mutipleTime= leaveTime.get(ni);
									mutipleTime.add(time);
									startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
								}
								else{
									List<Double> mutipleTime= leaveTime.get(ni);
									mutipleTime.add(time);
									leaveTime.put(ni, mutipleTime);//�����µ��뿪ʱ�����
								}	
							}
							else{
								List<Double> mutipleTime= new ArrayList<Double>();
								mutipleTime.add(time);
								leaveTime.put(ni, mutipleTime);//�����µ��뿪ʱ�����
							}*/
						}
						else{
							//System.out.println(this.host+" ����Ԥ��ڵ�: "+ni+" ʱ��  "+time);
							List<Double> mutipleTime= new ArrayList<Double>();
							mutipleTime.add(time);
							startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
						}
						/**�����δ��������ھӣ�ֱ��get�᷵�ؿ�ָ�룬����Ҫ�ȼ�startTime��leaveTime�ж�**/
					}	
				}
			}
			
			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //��Ԫ��ϲ���ʼ�ͽ���ʱ��
					new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
			
			
			return predictTime;
		}
		
		public List<GridCell> getNeighborCells(double time, int row, int col, int z){
			HashMap<GridCell, List<DTNHost>> cellToHost = cellmap.get(time);//��ȡtimeʱ�̵�ȫ�������
			List<GridCell> GC = new ArrayList<GridCell>();
			/***********************************************************************/

			/*��������ָ�*/
			for (int i = -1; i < 2; i += 1){
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						GC.add(cells[row+i][col+j][z+k]);
						
					}
				}
			}
			
			/*��������ָ�*/
			/*for (int i = -3; i <= 3; i += 1){
				for (int j = -3; j <= 3; j += 1){
					for (int k = -3; k <= 3; k += 1){
						if (boundaryCheck(row+i,col+j,z+k))
							GC.add(cells[row+i][col+j][z+k]);

					}
				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row+4,col+j,z+k)){
						GC.add(cells[row+4][col+j][z+k]);

					}
				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row-4,col+j,z+k))
						GC.add(cells[row-4][col+j][z+k]);

				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row+j,col+4,z+k))
						GC.add(cells[row+j][col+4][z+k]);

				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row+j,col-4,z+k))
						GC.add(cells[row+j][col-4][z+k]);

				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row+j,col+k,z+4))
						GC.add(cells[row+j][col+k][z+4]);

				}
			}
			for (int j = -1; j < 2; j += 1){
				for (int k = -1; k < 2; k += 1){
					if (boundaryCheck(row+j,col+k,z-4))
						GC.add(cells[row+j][col+k][z-4]);

				}
			}
			/*�Ĳ������ָ�*/
			/*	for (int i = -7; i <= 7; i += 1){
			for (int j = -7; j <= 7; j += 1){
				for (int k = -7; k <= 7; k += 1){
					if (boundaryCheck(row+i,col+j,z+k))
						GC.add(cells[row+i][col+j][z+k]);

				}
			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+8,col+j,z+k)){
					GC.add(cells[row+8][col+j][z+k]);

				}
			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row-8,col+j,z+k))
					GC.add(cells[row-8][col+j][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+8,z+k))
					GC.add(cells[row+j][col+8][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col-8,z+k))
					GC.add(cells[row+j][col-8][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+k,z+8))
					GC.add(cells[row+j][col+k][z+8]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+k,z-8))
					GC.add(cells[row+j][col+k][z-8]);

			}
		}
		//
		
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+9,col+j,z+k)){
					GC.add(cells[row+9][col+j][z+k]);

				}
			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row-9,col+j,z+k))
					GC.add(cells[row-9][col+j][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+9,z+k))
					GC.add(cells[row+j][col+9][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col-9,z+k))
					GC.add(cells[row+j][col-9][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+k,z+9))
					GC.add(cells[row+j][col+k][z+9]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+k,z-9))
					GC.add(cells[row+j][col+k][z-9]);

			}
		}
			//GC.add(cells[row][col][z]);//�޸��ھ������������������������������������������������������������������������������������������������
			/***********************************************************************/
			return GC;
		}
		
		public boolean boundaryCheck(int i, int j, int k){
			if (i<0 || j<0 || k<0)
				return false;
			if (i > rows+1 || j > cols+1 || k > zs+1){
				return false;
			}
			return true;
		}
		
		public boolean isHostsListEmpty(){
			return this.hosts.isEmpty();
		}
		
		public void updateGrid(){	

			ginterfaces.clear();//ÿ�����
			Coord location = new Coord(0,0); 	// where is the host
			double simClock = SimClock.getTime();

			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
				for (DTNHost host : hosts){
					location.my_Test(time, 0, host.getParameters());
					
					GridCell cell = updateLocation(time, location, host);//������ָ��ʱ��ڵ������Ĺ�����ϵ
					List<DTNHost> hostList = new ArrayList<DTNHost>();
					if (cellToHost.containsKey(cell)){
						hostList = cellToHost.get(cell);	
					}
					hostList.add(host);
					cellToHost.put(cell, hostList);
				}		
				cellmap.put(time, cellToHost);
				gridmap.put(time, ginterfaces);//Ԥ��δ��timeʱ����ڵ������֮��Ķ�Ӧ��ϵ
				//ginterfaces.clear();//ÿ�����
				ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����
				//CreateGrid(cellSize);//����cells��new��ginterfaces��new
			}
			
		}
		public GridCell updateLocation(double time, Coord location, DTNHost host){
			GridCell cell = cellFromCoord(location);
			//cell.addInterface(host.getInterface(1));
			ginterfaces.put(host.getInterface(1), cell);
			return cell;
		}
		
		

		private GridCell cellFromCoord(Coord c) {
			// +1 due empty cells on both sides of the matrix
			int row = (int)(c.getY()/cellSize) + 1; 
			int col = (int)(c.getX()/cellSize) + 1;
			int z = (int)(c.getZ()/cellSize) + 1;
			assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " + 
			c + " is out of world's bounds";
		
			return this.cells[row][col][z];
		}
		
		public void setHostsList(List<DTNHost> hosts){
			this.hosts = hosts;
		}
		
		public class GridCell {
			// how large array is initially chosen
			private static final int EXPECTED_INTERFACE_COUNT = 18;
			//private ArrayList<NetworkInterface> interfaces;//GridCell��������ά������ӿ��б�������¼�ڴ������ڵĽڵ㣬����ȫ��������˵����Ҫ��֤ͬһ������ӿڲ���ͬʱ����������GridCell��
			private int[] number;
			
			private GridCell() {
			//	this.interfaces = new ArrayList<NetworkInterface>(
			//			EXPECTED_INTERFACE_COUNT);
				number = new int[3];
			}
			
			public void setNumber(int row, int col, int z){
				number[0] = row;
				number[1] = col;
				number[2] = z;
			}
			public int[] getNumber(){
				return number;
			}
			
			public String toString() {
				return getClass().getSimpleName() + " with " + 
					" cell number: "+ number[0]+" "+number[1]+" "+number[2];
			}
		}
	}
}