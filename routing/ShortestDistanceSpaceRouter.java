/** 
 * Project Name:SatelliteRouterTest 
 * File Name:ShortestDistanceSpaceRouter.java 
 * Package Name:routing 
 * Date:2016��12��10������11:12:55 
 * Copyright (c) 2016, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
*/  
  
package routing;  
/** 
 * ClassName:ShortestDistanceSpaceRouter <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2016��12��10�� ����11:12:55 <br/> 
 * @author   USTC, LiJian
 * @version   
 * @since    JDK 1.7 
 * @see       
 */
/* 
 * Copyright 2016 University of Science and Technology of China , Infonet
 * Written by LiJian.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import routing.ShortestDistanceSpaceRouter.GridNeighbors.GridCell;
import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimError;

public class ShortestDistanceSpaceRouter extends ActiveRouter{
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
	private double msgTtl;
	
	HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();//�ڵ��·�ɱ�
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//ָʾ��һ���ڵ㴦��æ��״̬����Ҫ�ȴ�
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//����ȫ�������ڵ��ھ���·����ʱ����Ϣ
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	/*����ʵ�ֶ��ؿ����������*/
	private HashMap<String, Message> multiCopyList = new HashMap<String, Message>(); 
	
	private boolean routerTableUpdateLabel;
	private GridNeighbors GN;
	Random random = new Random();
	/**
	 * ��ʼ��
	 * @param s
	 */
	public ShortestDistanceSpaceRouter(Settings s){
		super(s);
	}
	/**
	 * ��ʼ��
	 * @param r
	 */
	protected ShortestDistanceSpaceRouter(ShortestDistanceSpaceRouter r) {
		super(r);
		Settings s = new Settings("Group");
		msgTtl = s.getDouble("msgTtl");
		this.GN = new GridNeighbors(this.getHost());//���������ԭ���ǣ���ִ����һ����ʼ����ʱ��host��router��û����ɰ󶨲���
	}
	/**
	 * ���ƴ�router��
	 */
	@Override
	public MessageRouter replicate() {
		//this.GN = new GridNeighbors(this.getHost());
		return new ShortestDistanceSpaceRouter(this);
	}
	/**
	 * ִ��·�ɵĳ�ʼ������
	 */
	public Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>> 
			initialzation(boolean firstCalculationLable, Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>> gridInfo){
		GN.setHost(this.getHost());//Ϊ��ʵ��GN��Router�Լ�Host֮��İ󶨣����޸ģ�������������������������������������������������������������������������������������������������)
		return this.GN.initializeGridLocation(firstCalculationLable, gridInfo);
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
		
		//this.sortByQueueMode(multiCopyList);
		/*���ڵ���ʵ�ֶ��ؿ����������*/
		for (Message msg : multiCopyList.values()){
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (conNeighbors.contains(msg.getTo())){
				Tuple<Message, Connection> t = new Tuple<Message, Connection>(msg, this.findConnection(msg.getTo().getAddress()));//����������Ϣ˳����·���������Է���
				if (sendMsg(t))
					return;
			}			
		}
		/*���ڵ���ʵ�ֶ��ؿ����������*/
		this.sortByQueueMode(messages);
		for (Message msg : messages){//���Է��Ͷ��������Ϣ	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true){
				return;
			}

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
	 * ����·�ɱ�����1������������·��·����2������ȫ��Ԥ��
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){
		
		gridSearch(msg);
		
		/*���ؿ���������Ƶ�ʵ��*/
		if (msg.getProperty("MultiCopy") != null)
			if ((boolean)msg.getProperty("MultiCopy") == true)	
				multiCopyList.put(msg.getId(), msg.replicate());//���Ƶõ���msg��id��һ���ģ�����unquidID��һ��	
		/*���ؿ���������Ƶ�ʵ��*/
		
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
	
	/**
	 * ����·���㷨������̰��ѡ�����ʽ��б������ҳ�����Ŀ�Ľڵ�����·��
	 * @param msg
	 */
	public void gridSearch(Message msg){
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true�����˴θ���·�ɱ��Ѿ����¹��ˣ����Բ�Ҫ�ظ�����
			return;
		this.routerTable.clear();
		
		if (GN.isHostsListEmpty()){
			GN.setHostsList(hosts);
		}
		//GridNeighbors GN = this.getHost().getGridNeighbors();
		Settings s = new Settings(GROUPNAME_S);
		String option = s.getSetting("Pre_or_onlineOrbitCalculation");//�������ļ��ж�ȡ���ã��ǲ��������й����в��ϼ���������ķ�ʽ������ͨ����ǰ���������洢�����ڵ�Ĺ����Ϣ
		
		HashMap<String, Integer> orbitCalculationWay = new HashMap<String, Integer>();
		orbitCalculationWay.put("preOrbitCalculation", 1);
		orbitCalculationWay.put("onlineOrbitCalculation", 2);
		
		switch (orbitCalculationWay.get(option)){
		case 1://ͨ����ǰ���������洢�����ڵ�Ĺ����Ϣ���Ӷ����й����в��ٵ��ù�����㺯����Ԥ�����ͨ��������Ԥ��
			GN.updateGrid_without_OrbitCalculation();//���������
			break;
		case 2:
			GN.updateGrid_with_OrbitCalculation();//���������
			break;
		}
		
		/*�����·��̽�⵽��һ���ھӣ�������·�ɱ�*/
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//��ʼʱֻ�б��ڵ�
		
		List<DTNHost> neiHosts = new ArrayList<DTNHost>();//���ڼ�¼�������ӵ��ھӽڵ�
		for (Connection con : this.getHost().getConnections()){//�����·��̽�⵽��һ���ھӣ�������·�ɱ�
			DTNHost neiHost = con.getOtherNode(this.getHost());
			neiHosts.add(neiHost);
			sourceSet.add(neiHost);//��ʼʱֻ�б��ڵ����·�ھ�		
			double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
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
		DTNHost minHost = this.getHost();
		boolean updateLabel = true;
		boolean predictLable = false;

		
		arrivalTime.put(this.getHost(), SimClock.getTime());//��ʼ������ʱ�䣬���뱾�ڵ��ִ�д��㷨ʱ��ʱ�䣬���������ھӽڵ�ʱ���õ�
		
		/*���������ǲ���һ�����ھӽڵ㣬���ھӽڵ���ֱ�ӷ���*/
		if (this.routerTable.containsKey(msg.getTo()))
			return;
		/*���������ǲ���һ�����ھӽڵ㣬���ھӽڵ���ֱ�ӷ���*/
		List<DTNHost> searchedSet = new ArrayList<DTNHost>();
		searchedSet.add(this.getHost());
		
		List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();//���ڼ�¼�㷨�������ģ�����Ŀ�Ľڵ��·��
		
		//String standard = "ShortestDistance";
		String standard = "MinimumSpeed";
		HashMap<String, Integer> routerStandard = new HashMap<String, Integer>();
		routerStandard.put("ShortestDistance", 1);
		routerStandard.put("MinimumSpeed", 2);
		
		switch (routerStandard.get(standard)){
		case 1:{
			/*�·����Ĳ��Դ���*/
			/*����̿ռ����Ϊ·��׼��ֻ����������̿ռ����ĵ��ھӽڵ���Ϊ��һ��(����Ԥ��ڵ�)��ͬʱ����TTL�����ƣ������������ѡ��֮�Ľڵ�*/
			
			ArrayList<Tuple<DTNHost, Double>> distanceList = new ArrayList<Tuple<DTNHost, Double>>();//���ڽ������洢����õ��ľ�����Ϣ�������ݴ˼�¼����
			HashMap<DTNHost, hostInfo> hostInfoList = new HashMap<DTNHost, hostInfo>();
			
			/*��ȡԴ������ָ��host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)--ʱ���ΪԤ�������host��ʱ��prevTime*/
			List<DTNHost> neiList = GN.getNeighbors(this.getHost(), arrivalTime.get(this.getHost()));		
			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime = GN.getFutureNeighbors(neiList, this.getHost(), arrivalTime.get(this.getHost()));
			/*��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)----ͨ��Grid���е�Ԥ�⣬���Դ���һ�������*/
			
			HashMap<DTNHost, List<Double>> startTime = predictTime.getKey();
			HashMap<DTNHost, List<Double>> leaveTime = predictTime.getValue();
			List<DTNHost> neighborNodes= new ArrayList<DTNHost>(startTime.keySet());//�õ�prevHost��һ�����ھӽڵ�//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
			
			double minDistance = GN.calculateDistance(this.getHost(), msg.getTo(), SimClock.getTime(), SimClock.getTime() + this.msgTtl);
			//double minDistance = 100000;
			double waitTime = 0;
			double minWaitTime = 0;
			
			distanceList.add(new Tuple<DTNHost, Double>(this.getHost(), minDistance));
			hostInfoList.put(this.getHost(), new hostInfo(this.getHost(), minDistance, SimClock.getTime(), 0));
			for (DTNHost host : neighborNodes){
				waitTime = 0;
				/*neiHosts��¼�˵�ǰ�������ӵ��ھӽڵ㣬ͨ��connection�б��ȡ*/
				if (arrivalTime.get(this.getHost()) < startTime.get(host).get(0) && !neiHosts.contains(host))//���ȷʵ��һ����ǰ�ʹ��ڵ�δ���Ľڵ㣬�Ͳ�����starttimeʱ�����ж���
					waitTime = startTime.get(host).get(0) - arrivalTime.get(this.getHost());//���Ϊ�棬��Ϊһ��δ������Ľڵ㣬��Ҫ�еȴ���ʱ
				double time = arrivalTime.get(this.getHost()) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
				System.out.println(this.getHost()+" to "+msg.getTo()+" of msg "+msg.getId()+"  and the next hop  "+host+"  time is: " + time);
				if (SimClock.getTime() + time > SimClock.getTime() + msg.getTtl()*60){
					System.out.println("expire TTL!!! TTL is:  "+(SimClock.getTime() + msg.getTtl()*60));
					continue;
				}
				//����TTLʱ�����һ�����ᱻֱ���޳�
				double distance = GN.calculateDistance(host, msg.getTo(), time, time + this.msgTtl);
				Tuple<DTNHost, Double> t = new Tuple<DTNHost, Double>(host, distance);
				distanceList.add(t);
				
				hostInfo info = new hostInfo(host, distance, time, waitTime);
				hostInfoList.put(host, info);
			}
			/*ð������*/
			System.out.println(distanceList);
			hostInfoList.get(this.getHost()).sort(distanceList);
			System.out.println(distanceList);
			/*ð������*/
			if (distanceList.get(0).getKey() != this.getHost()){//���е�Ŀ�Ľڵ�������ھӽڵ�
				DTNHost nextHop = distanceList.get(0).getKey();
				path.add(hostInfoList.get(nextHop).getWaitTime() > 0 ? 
						new Tuple<Integer, Boolean>(nextHop.getAddress(), true) : 
							new Tuple<Integer, Boolean>(nextHop.getAddress(), false));//ÿһ��������֮�󣬼�¼·��·��
				arrivalTime.put(nextHop, hostInfoList.get(nextHop).getTime());
				this.routerTable.put(msg.getTo(), path);//��ʱֻ����һ���ڵ㣬�����ܷ��ҵ����嵽��Ŀ�Ľڵ�·����������
				System.out.println("1 minHost is: "+nextHop+"   now the path is: "+path);
			}
			else{//˵������ڵ��Ѿ��Ǻ�Ŀ�Ľڵ��������Ľڵ��ˣ�������δ�������������ھӽڵ���Ŀ�Ľڵ������Ҳһ��������TTL���Բ����ߵ����
				//��ʱ�����ڱ��ڵ㱣��һ�ݣ����л���ʱ����Ŀ�Ľڵ㣬ͬʱѡ��ν��Ľڵ��ٷ�һ��
				if (distanceList.size() >= 2){//����֤�����������⻹�п����ھӽڵ�
					for (int i = 1; i < distanceList.size(); i++){
						if (distanceList.get(i).getKey() != this.getHost() && !msg.getHops().contains(distanceList.get(i).getKey())){//��֤���߻�ͷ·
							DTNHost nextHop = distanceList.get(i).getKey();
							path.add(hostInfoList.get(nextHop).getWaitTime() > 0 ? 
									new Tuple<Integer, Boolean>(nextHop.getAddress(), true) : 
										new Tuple<Integer, Boolean>(nextHop.getAddress(), false));//ÿһ��������֮�󣬼�¼·��·��
							this.routerTable.put(msg.getTo(), path);//��ʱֻ����һ���ڵ㣬�����ܷ��ҵ����嵽��Ŀ�Ľڵ�·����������
							System.out.println("2 minHost is: "+nextHop+"   now the path is: "+path);
							return;//�ҵ�һ����һ���ڵ�֮��ͷ���
						}
					}
				}				
				System.out.println("û�и��¸����Ľڵ�");
				/*���޸ģ���Ӷಥ���ִ���*/
			}
				/*�·����Ĳ��Դ���*/
			break;
		}
		case 2:{
			/*����̿ռ����Ϊ·��׼��ֻ����������̿ռ����ĵ��ھӽڵ���Ϊ��һ��(����Ԥ��ڵ�)��ͬʱ����TTL�����ƣ������������ѡ��֮�Ľڵ�*/
		
			ArrayList<Tuple<DTNHost, Double>> speedList = new ArrayList<Tuple<DTNHost, Double>>();
			HashMap<DTNHost, hostInfo> hostInfoList = new HashMap<DTNHost, hostInfo>();
			
			/*��ȡԴ������ָ��host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)--ʱ���ΪԤ�������host��ʱ��prevTime*/
			List<DTNHost> neiList = GN.getNeighbors(this.getHost(), arrivalTime.get(this.getHost()));		
			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime = GN.getFutureNeighbors(neiList, this.getHost(), arrivalTime.get(this.getHost()));
			/*��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)----ͨ��Grid���е�Ԥ�⣬���Դ���һ�������*/

			HashMap<DTNHost, List<Double>> startTime = predictTime.getKey();
			HashMap<DTNHost, List<Double>> leaveTime = predictTime.getValue();
			List<DTNHost> neighborNodes= new ArrayList<DTNHost>(startTime.keySet());//�õ�prevHost��һ�����ھӽڵ�//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
			
			double minDistance = GN.calculateDistance(this.getHost(), msg.getTo(), SimClock.getTime(), SimClock.getTime() + this.msgTtl);
			//double minDistance = 100000;
			double waitTime = 0;
			double minWaitTime = 0;
			
			/**�������ڵ��ֵ��Ϊ��׼**/
			speedList.add(new Tuple<DTNHost, Double>(this.getHost(), 0.0));
			hostInfoList.put(this.getHost(), new hostInfo(this.getHost(), 0, SimClock.getTime(), 0));
			for (DTNHost host : neighborNodes){
				waitTime = 0;
				/*neiHosts��¼�˵�ǰ�������ӵ��ھӽڵ㣬ͨ��connection�б��ȡ*/
				if (arrivalTime.get(this.getHost()) < startTime.get(host).get(0) && !neiHosts.contains(host))//���ȷʵ��һ����ǰ�ʹ��ڵ�δ���Ľڵ㣬�Ͳ�����starttimeʱ�����ж���
					waitTime = startTime.get(host).get(0) - arrivalTime.get(this.getHost());//���Ϊ�棬��Ϊһ��δ������Ľڵ㣬��Ҫ�еȴ���ʱ
				double time = arrivalTime.get(this.getHost()) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
				System.out.println(this.getHost()+" to "+msg.getTo()+" of msg "+msg.getId()+"  and the next hop  "+host+"  time is: " + time);
				if (SimClock.getTime() + time > SimClock.getTime() + msg.getTtl()*60){
					System.out.println("expire TTL!!! TTL is:  "+(SimClock.getTime() + msg.getTtl()*60));
					continue;
				}
				//����TTLʱ�����һ�����ᱻֱ���޳�
				double distance = GN.calculateDistance(host, msg.getTo(), time, time + this.msgTtl);
				double speed = (minDistance - distance)/(time - SimClock.getTime());
				Tuple<DTNHost, Double> t = new Tuple<DTNHost, Double>(host, speed);
				speedList.add(t);
				
				hostInfo info = new hostInfo(host, speed, time, waitTime);
				hostInfoList.put(host, info);
			}
			/*ð������*/
			hostInfoList.get(this.getHost()).sortSpeed(speedList);
			/*ð������*/
			if (speedList.get(0).getKey() != this.getHost() && !msg.getHops().contains(speedList.get(0).getKey())){//���е�Ŀ�Ľڵ�������ھӽڵ�//msg.getHops()��¼msg�Ѿ������Ľڵ�
				DTNHost nextHop = speedList.get(0).getKey();
				path.add(hostInfoList.get(nextHop).getWaitTime() > 0 ? 
						new Tuple<Integer, Boolean>(nextHop.getAddress(), true) : 
							new Tuple<Integer, Boolean>(nextHop.getAddress(), false));//ÿһ��������֮�󣬼�¼·��·��
				arrivalTime.put(nextHop, hostInfoList.get(nextHop).getTime());
				this.routerTable.put(msg.getTo(), path);//��ʱֻ����һ���ڵ㣬�����ܷ��ҵ����嵽��Ŀ�Ľڵ�·����������
				System.out.println("1 minHost is: "+nextHop+"   now the path is: "+path);
			}
			else{//˵������ڵ��Ѿ��Ǻ�Ŀ�Ľڵ��������Ľڵ��ˣ�������δ�������������ھӽڵ���Ŀ�Ľڵ������Ҳһ��������TTL���Բ����ߵ����
				//��ʱ�����ڱ��ڵ㱣��һ�ݣ����л���ʱ����Ŀ�Ľڵ㣬ͬʱѡ��ν��Ľڵ��ٷ�һ��
				if (speedList.size() >= 2){//����֤�����������⻹�п����ھӽڵ�
					for (int i = 1; i < speedList.size(); i++){
						if (speedList.get(i).getKey() != this.getHost() && !msg.getHops().contains(speedList.get(i).getKey())){//��֤���߻�ͷ·
							DTNHost nextHop = speedList.get(i).getKey();
							path.add(hostInfoList.get(nextHop).getWaitTime() > 0 ? 
									new Tuple<Integer, Boolean>(nextHop.getAddress(), true) : 
										new Tuple<Integer, Boolean>(nextHop.getAddress(), false));//ÿһ��������֮�󣬼�¼·��·��
							this.routerTable.put(msg.getTo(), path);//��ʱֻ����һ���ڵ㣬�����ܷ��ҵ����嵽��Ŀ�Ľڵ�·����������
							System.out.println("2 minHost is: "+nextHop+"   now the path is: "+path);
							
							/*ʵ����Ϣ�Ķ��ؿ�������*/
							if (msg.getProperty("MultiCopy") == null){//�����ǵ�һ�ζ��ؿ���
								msg.updateProperty("MultiCopy", true);
								msg.updateProperty("MultiCopyInfo_minDistance", hostInfoList.get(nextHop).getdistance());//��¼��ʱ����̾���
							}
							else{
								assert msg.getProperty("MultiCopyInfo_minDistance") != null : "MultiCopy error!!!" ;
								if ((double)msg.getProperty("MultiCopyInfo_minDistance") > hostInfoList.get(nextHop).getdistance()){
									msg.updateProperty("MultiCopyInfo_minDistance", hostInfoList.get(nextHop).getdistance());
								}
								else
									msg.updateProperty("MultiCopy", false);//�����Ǿ�����̽ڵ㣬��˲����ڴ˽ڵ��ϲ��ܶ��ؿ���
							}
							/*ʵ����Ϣ�Ķ��ؿ�������*/
							return;//�ҵ�һ����һ���ڵ�֮��ͷ���
						}
					}
				}			
				//System.out.println("û�и��¸����Ľڵ�");
			}			
			break;
		}
		case 3:{
			/*�Ծ���ʸ��Ϊ��С�Ż�Ŀ�����̾��������㷨*/
			while(iteratorTimes < size && updateLabel == true){//�趨�㷨ִ��ѭ������ֹ������1.����N�Σ�NΪ�ܽڵ��������2.��һ�θ�����û���ҵ����ʵ���һ���ڵ�
				double minDistance = 100000;
				updateLabel = false;		
				DTNHost prevHost = minHost;					
				
				//List<DTNHost> neiList = GN.getNeighbors(prevHost, SimClock.getTime() + prevTime);//��ȡԴ������ָ��host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)--ʱ���ΪԤ�������host��ʱ��prevTime
				//Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime = GN.getFutureNeighbors(neiList, prevHost, SimClock.getTime() + prevTime);//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)	
				List<DTNHost> neiList = GN.getNeighbors(prevHost, arrivalTime.get(prevHost));//��ȡԴ������ָ��host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)--ʱ���ΪԤ�������host��ʱ��prevTime			
				Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime = GN.getFutureNeighbors(neiList, prevHost, arrivalTime.get(prevHost));//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)

				HashMap<DTNHost, List<Double>> startTime = predictTime.getKey();
				HashMap<DTNHost, List<Double>> leaveTime = predictTime.getValue();
				
				List<DTNHost> neighborNodes= new ArrayList<DTNHost>(startTime.keySet());//�õ�prevHost��һ�����ھӽڵ�//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
				neighborNodes.addAll(neiList);
//				/*�ж��Ƿ�����Ч����һ���ڵ�*/
//				if (searchedSet.containsAll(neighborNodes)){
//					for (Tuple<Integer, Boolean> thisHop : path){
//						DTNHost thisNode = this.getHostFromAddress(thisHop.getKey());//��int��ַ�ҵ���Ӧ��DTNHost�ڵ�
//						HashMap<DTNHost, List<Double>> neighborMap = GN.getFutureNeighbors(neiList, prevHost, SimClock.getTime() + prevTime).getKey();
//						if (neighborMap.containsKey(msg.getTo()))
//							/*���޷�ֱ���ʹ�Ŀ�Ľڵ������£���Դ�ڵ��Ƿ��л�����TTL֮ǰ����Ϣ�͵�Ŀ�Ľڵ�*/
//							if (neighborMap.get(msg.getTo()).get(0) < msg.getTtl()*60){
//								path.clear();
//								path.add(new Tuple<Integer, Boolean>(msg.getTo().getAddress(), true));
//								routerTable.put(msg.getTo(), path);
//							}		
//							/*���޷�ֱ���ʹ�Ŀ�Ľڵ������£���Դ�ڵ��Ƿ��л�����TTL֮ǰ����Ϣ�͵�Ŀ�Ľڵ�*/
//					}
//					break;//���޷��ѵ�ֱ�ӵ���Ŀ�Ľڵ��·��������£�����·���㷨
//				}
//				/*�ж��Ƿ�����Ч����һ���ڵ�*/
				
				if (!neiList.containsAll(predictTime.getKey().keySet())){
					System.out.println("  this node is: "+this.getHost()+" and all neighbors of  "+prevHost+" is "+predictTime.getKey().keySet());
				}
				
				double waitTime = 0;
				for (DTNHost host : neighborNodes){
					waitTime = 0;
					if (searchedSet.contains(host))
						continue;
					System.out.println("neighbors :"+ neiList);
					if (arrivalTime.get(prevHost) < startTime.get(host).get(0) && !neiList.contains(host))//���ȷʵ��һ����ǰ�ʹ��ڵ�δ���Ľڵ㣬�Ͳ�����starttimeʱ�����ж���
						waitTime = startTime.get(host).get(0) - arrivalTime.get(prevHost);//���Ϊ�棬��Ϊһ��δ������Ľڵ㣬��Ҫ�еȴ���ʱ
					double time = arrivalTime.get(prevHost) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
					System.out.println("this time is : "+ host + " and simtime is: "+ SimClock.getTime() +" transmission time is: "+time +"  "+ arrivalTime.get(prevHost) +"  "+ waitTime);
					if (SimClock.getTime() + time > SimClock.getTime() + msg.getTtl()*60){
						System.out.println("expire TTL!!! TTL is:  "+(SimClock.getTime() + msg.getTtl()*60));
						continue;
					}
						
					double distance = GN.calculateDistance(host, msg.getTo(), time, time+this.msgTtl);
					
					if (distance <= minDistance){
						if (random.nextBoolean() == true && distance - minDistance < 0.01){//�������һ������Ľڵ㣬�����ѡһ��
							minDistance = distance;
							minHost = host;
							minTime = time;
							updateLabel = true;
						}
					}
					searchedSet.add(host);//�����Ѿ��������Ľڵ�	
				}		
				if (updateLabel == true){
					path.add(waitTime > 0 ? 
							new Tuple<Integer, Boolean>(minHost.getAddress(), true) : 
								new Tuple<Integer, Boolean>(minHost.getAddress(), false));//ÿһ��������֮�󣬼�¼·��·��
					arrivalTime.put(minHost, SimClock.getTime() + minTime);
					this.routerTable.put(minHost, path);//�������·��׼���ҵ���ͨ��minHost��һ��·��
					System.out.println("minHost is: "+minHost+"   now the path is: "+path+" searchedSet is: "+searchedSet);
				}
				/*�ж��Ƿ��ҵ�����Ŀ�Ľڵ��·��*//*
				if (GN.getNeighbors(minHost, SimClock.getTime() + minTime).contains(msg.getTo())){
					path.add(new Tuple<Integer, Boolean>(msg.getTo().getAddress(), false));
					this.routerTable.put(msg.getTo(), path);
					break;//�ҵ�һ���ܵ���Ŀ�Ľڵ��·�����˳�
				}
				if (minHost == msg.getTo()){
					this.routerTable.put(msg.getTo(), path);
					break;//�ҵ�һ���ܵ���Ŀ�Ľڵ��·�����˳�
				}
				/*�ж��Ƿ��ҵ�����Ŀ�Ľڵ��·��*/
				
				if (routerTable.containsKey(msg.getTo()))//�����;�ҵ���Ҫ��·������ֱ���˳�����
				break;
				iteratorTimes++;
			}
			/*�Ծ���ʸ��Ϊ��С�Ż�Ŀ�����̾��������㷨*/
			break;
		}
		}

		

		
		//System.out.println(this.getHost()+" path: "+path+ " to "+msg.getTo()+" time : "+SimClock.getTime());
//		
//		while(true){//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
//			if (iteratorTimes >= size || updateLabel == false)
//				break; 
//			updateLabel = false;
//			minTime = 100000;
//			HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>> 
//						predictionList = new HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, 
//						HashMap<DTNHost, List<Double>>>>();//�洢���ƣ����֮ǰ�Ѿ�����Ͳ������ظ�������
//			
//			for (DTNHost host : sourceSet){
//				Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime;//�ھӽڵ㵽��ʱ����뿪ʱ��Ķ�Ԫ�����
//				HashMap<DTNHost, List<Double>> startTime;
//				HashMap<DTNHost, List<Double>> leaveTime;
//				
//				if (predictionList.containsKey(host)){//�洢���ƣ����֮ǰ�Ѿ�����Ͳ������ظ�������
//					predictTime = predictionList.get(host);
//				}
//				else{
//					List<DTNHost> neiList = GN.getNeighbors(host, arrivalTime.get(host));//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)
//					assert neiList == null:"û�ܳɹ���ȡ��ָ��ʱ����ھ�";
//					predictTime = GN.getFutureNeighbors(neiList, host, arrivalTime.get(host));
//				}
//				startTime = predictTime.getKey();
//				leaveTime = predictTime.getValue();
//				if (startTime.keySet().isEmpty())
//					continue;
//				for (DTNHost neiHost : startTime.keySet()){//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
//					if (sourceSet.contains(neiHost))
//						continue;
//					if (arrivalTime.get(host) >= SimClock.getTime() + msgTtl*60)//����ʱ����Ѿ�����TTLԤ��ʱ��Ļ���ֱ���ų�
//						continue;
//					double waitTime = startTime.get(neiHost).get(0) - arrivalTime.get(host);
//					if (waitTime <= 0){
//						predictLable = false;
//						//System.out.println("waitTime = "+ waitTime);
//						waitTime = 0;
//					}
//					if (waitTime > 0)
//						predictLable = true;
//					double time = arrivalTime.get(host) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
//					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
//					if (this.routerTable.containsKey(host))
//						path.addAll(this.routerTable.get(host));
//					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), predictLable);
//					path.add(hop);//ע��˳��
//					/*���޸ģ�Ӧ����leavetime�ļ��!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
//					if (leaveTime.isEmpty()){
//						if (time > SimClock.getTime() + msgTtl*60)
//							continue;
//					}
//					else{
//						if (time > leaveTime.get(neiHost).get(0))
//							continue;
//					}
//					/*���޸ģ�Ӧ����leavetime�ļ��!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
//					if (time <= minTime){
//						if (random.nextBoolean() == true && time - minTime < 1){
//							minTime = time;
//							minHost = neiHost;
//							minPath = path;
//							updateLabel = true;	
//						}
//					}					
//				}
//			}
//			if (updateLabel == true){
//				arrivalTime.put(minHost, minTime);
//				routerTable.put(minHost, minPath);
//			}
//			iteratorTimes++;
//			sourceSet.add(minHost);//���µ���̽ڵ����
//			if (routerTable.containsKey(msg.getTo()))//�����;�ҵ���Ҫ��·������ֱ���˳�����
//				break;
//		}
//		routerTableUpdateLabel = true;
//		
//		System.out.println(this.getHost()+" table: "+routerTable+" time : "+SimClock.getTime());
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
		
		private int gridLayer;//�����������������񻮷ֵľ�ϸ�̶�
		
		private List<GridCell[][][]> GridList = new ArrayList<GridCell[][][]>();
		private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();//��¼TTLʱ���ڣ�ÿ��ʱ��㣬�����ڵ������Ķ�Ӧ��ϵ
		private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
		
		/*���ڳ�ʼ��ʱ����������ڵ���һ�������ڵ���������*/
		private HashMap <DTNHost, List<GridCell>> gridLocation = new HashMap<DTNHost, List<GridCell>>();//��Žڵ�������������
		private HashMap <DTNHost, List<Double>> gridTime = new HashMap<DTNHost, List<Double>>();//��Žڵ㾭����Щ����ʱ��ʱ��
		private HashMap <DTNHost, Double> periodMap = new HashMap <DTNHost, Double>();//��¼�����ڵ���������
		
		public GridNeighbors(DTNHost host){
			this.host = host;
			//System.out.println(this.host);
			Settings se = new Settings("Interface");
			transmitRange = se.getDouble("transmitRange");//�������ļ��ж�ȡ��������
			Settings set = new Settings("Group");
			msgTtl = set.getDouble("msgTtl");
			
			Settings s = new Settings(MovementModel.MOVEMENT_MODEL_NS);
			int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,2);//������2ά�޸�Ϊ3ά
			worldSizeX = worldSize[0];
			worldSizeY = worldSize[1];
			worldSizeZ = worldSize[1];//������ά����������飡��������������������������������������������������������������������������������������������
			
			Settings layer = new Settings("Group");
			this.gridLayer = layer.getInt("layer");
			
			switch(this.gridLayer){
			case 1 : 
				cellSize = (int) (transmitRange*0.288);//Layer=2
				break;
			case 2 : 
				cellSize = (int) (transmitRange*0.14433);//Layer=3
			//cellSize = (int) (transmitRange*0.0721687);//Layer=4
				break;
			default :
				cellSize = (int) (transmitRange*0.288);//Layer=2
				break;
			}
			//cellSize = (int) (transmitRange*0.5773502);
			
			CreateGrid(cellSize);
			/*��ʼ����ǰ��������ǹ����Ϣ*/
			
		}
		public void setHost(DTNHost h){
			this.host = h;
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
		/**
		 * ��v���й��c����ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
		 */
		public Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>>
				initializeGridLocation(boolean firstCalculationLable, Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>> gridInfo){
			/*�����ʼ�����̣���Ϊÿ���ڵ������һ�������ظ��ģ�����ֻ����һ�Σ�����Ķ����Ƶ�һ�μ���Ľ������*/
			if (firstCalculationLable != true){
				gridLocation = gridInfo.getKey();
				gridTime = gridInfo.getValue();
				//System.out.println(" copy "+gridLocation.get(this.getHost()) );
				return gridInfo;
			}
			/*�����ʼ�����̣���Ϊÿ���ڵ������һ�������ظ��ģ�����ֻ����һ�Σ�����Ķ����Ƶ�һ�μ���Ľ������*/
			
			Coord location = new Coord(0, 0);
			
			this.host.getHostsList();
			for (DTNHost h : this.host.getHostsList()){//��ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
				double period = getPeriodofOrbit(h);
				this.periodMap.put(h, period);
				System.out.println(this.host+" now calculate "+h+" orbit period: "+period);
				
				List<GridCell> gridList = new ArrayList<GridCell>();
				List<Double> intoTime = new ArrayList<Double>();
				List<Double> outTime = new ArrayList<Double>();
				GridCell startCell = new GridCell();//��¼��ʼ����
				for (double time = 0; time < period; time += updateInterval){
								
					/**���Դ���**/
					//Coord c = h.getCoordinate(time);
					//GridCell gc = cellFromCoord(c);//���������ҵ������ľW��
										
					location.setLocation3D(((SatelliteMovement)h.getMovementModel()).getSatelliteCoordinate(time));
					GridCell gc = cellFromCoord(location);
					/**���Դ���**/
					
					if (!gridList.contains(gc)){
						if (gridList.isEmpty())
							startCell = gc;//��¼��ʼ����
						gridList.add(gc);//��һ�μ�⵽�ڵ���������ע�⣬�߽��飡������ʼ�ͽ�����ʱ�򣡣���!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!��
						intoTime.add(time);//��¼��Ӧ�Ľ���ʱ��
					}	
					/**ע�⣬�ڵ�һ����ӹ�gc��gridlist֮���кܳ�һ��ʱ�䶼�����gridList.contains(gc)==ture���������������else����²������������жϻ���**/
//					else{		
//						//if (gc == startCell)
//							//System.out.println(gc+"  "+time+"  "+period);
//						//else
//							//System.out.println(gc+" error "+time+"  "+period);
//							//intoTime = time;
//					}				
				}
				gridLocation.put(h, gridList);//������һ���ڵ�ͼ�¼����
				gridTime.put(h, intoTime);//��Ӧ��ʱ��
			}					
			//System.out.println(gridLocation);
			return new 
					Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>>(gridLocation, gridTime);			
		}
		/**
		 * �@ȡָ���l�ǹ��c���\���L�ڕr�g
		 * @param h
		 * @return
		 */
		public double getPeriodofOrbit(DTNHost h){
			return h.getPeriod();
		}
		
		/**
		 * ����ӿ�ʼ������ʱ���������ڵ�֮��ľ��룬�������������ɢ��ά����������
		 * @param source
		 * @param destination
		 * @param startTime
		 * @param endTime
		 * @return
		 */
		public double calculateDistance(DTNHost source, DTNHost destination, double startTime, double endTime){		
			List<GridCell> p1 = gridLocation.get(source);//ָ���ڵ�һ�������ھ����������б�
			List<GridCell> p2 = gridLocation.get(destination);
			
			List<Double> t1 = gridTime.get(source);//ָ���ڵ��ھ�����Щ����ʱ��Ӧ��ʱ��
			List<Double> t2 = gridTime.get(destination);
			
//			int startLocation_1 = findMostCloseValueFromList(t1, startTime);
//			int endLocation_1 = findMostCloseValueFromList(t1, endTime);
//			int startLocation_2 = findMostCloseValueFromList(t2, startTime);
//			int endLocation_2 = findMostCloseValueFromList(t2, endTime);
//			
//			List<GridCell> pathDuringTTL_1 = new ArrayList<GridCell>();		
//			for (int i = startLocation_1; i <= endLocation_1; i++){
//				pathDuringTTL_1.add(p1.get(i));
//			}
//			List<GridCell> pathDuringTTL_2 = new ArrayList<GridCell>();
//			for (int i = startLocation_2; i <= endLocation_2; i++){
//				pathDuringTTL_2.add(p1.get(i));
//			}
			double interval = 1.0;
			int location_1 = 0, location_2 = 0;
			double distance = 0;
			for (double t = startTime; t <= endTime; t += interval){
				location_1 = findMostCloseValueFromList(t1, t);
				location_2 = findMostCloseValueFromList(t2, t);
				/*��������������ά ����֮��*/
				for (int i = 0; i < 3; i++){
					distance = distance + Math.abs(p1.get(location_1).getNumber()[i] - p2.get(location_2).getNumber()[i]);
				}			
			}
			//�����⣬���޸ģ�����������������������������
			//System.out.println("  "+source+"  "+destination+" distance is: "+distance);
			return distance;
		}
		
		public int findMostCloseValueFromList(List<Double> t1, double time){
			double minDifferenceValue = 10000000;
			int iterator = 0;
			for (int i = 0; i < t1.size(); i++){
				double differenceValue = Math.abs(t1.get(i) - time);
				if (differenceValue < minDifferenceValue){
					minDifferenceValue = differenceValue;
					iterator = i;
				}
			}
			return iterator;
		}
		
		public double transformTimeFormat(double t){
			double time = t;
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;
			
			if (time > SimClock.getTime()+msgTtl*60){//��������ʱ���Ƿ񳬹�Ԥ��ʱ��
				//assert false :"����Ԥ��ʱ��";
				time = SimClock.getTime()+msgTtl*60;
			}
			return time;
		}
		
		/**
		 * ��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ����δ��������ھ�)
		 * @param host
		 * @param time
		 * @return
		 */
		public List<DTNHost> getNeighbors(DTNHost host, double time){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;
			
			if (time > SimClock.getTime()+msgTtl*60){//��������ʱ���Ƿ񳬹�Ԥ��ʱ��
				//assert false :"����Ԥ��ʱ��";
				time = SimClock.getTime()+msgTtl*60;
			}
			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//��ȡָ��ʱ�̣�ȫ�ֽڵ�������ӳ���
			GridCell cell = ginterfaces.get(host.getInterface(1));
			int[] number = cell.getNumber();

			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
			List<DTNHost> hostList = new ArrayList<DTNHost>();//(�ھ������ڵĽڵ㼯��)

			assert cellmap.containsKey(time):" ʱ����� ";
			for (GridCell c : cellList){
				if (cellmap.get(time).containsKey(c)){//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
					hostList.addAll(cellmap.get(time).get(c));
				}
			}	
			if (hostList.contains(host))//������ڵ�ȥ��
				hostList.remove(host);

			return hostList;
		}
		
		public List<DTNHost> getNeighborsNow(DTNHost host, double time){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;
			
			if (time > SimClock.getTime()+msgTtl*60){//��������ʱ���Ƿ񳬹�Ԥ��ʱ��
				//assert false :"����Ԥ��ʱ��";
				time = SimClock.getTime()+msgTtl*60;
			}
				
			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//��ȡָ��ʱ�̣�ȫ�ֽڵ�������ӳ���
			GridCell cell = ginterfaces.get(host.getInterface(1));
			int[] number = cell.getNumber();
			
			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
			List<DTNHost> hostList = new ArrayList<DTNHost>();//(�ھ������ڵĽڵ㼯��)
			assert cellmap.containsKey(time):" ʱ����� ";
			for (GridCell c : cellList){
				if (cellmap.get(time).containsKey(c))//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
					hostList.addAll(cellmap.get(time).get(c));
			}	
			if (hostList.contains(host))//������ڵ�ȥ��
				hostList.remove(host);
			//System.out.println(host+" �ھ��б�   "+hostList);
			return hostList;
		}
		
		/**
		 * ��Ҫ��getNeighbors(DTNHost host, double time)���������صĵ�ǰʱ���ھӲ������ʹ�ã�
		 * �ҳ�δ��ʱ����ָ���ڵ��δ�������ھӣ����������л������ھӵ��б�(��ǰ��δ����)
		 * @param neiList
		 * @param host
		 * @param time
		 * @return
		 */
		public Tuple<HashMap<DTNHost, List<Double>>, //neiList Ϊ�Ѿ�������ĵ�ǰ�ھӽڵ��б�
			HashMap<DTNHost, List<Double>>> getFutureNeighbors(List<DTNHost> neiList, DTNHost host, double time){
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;	
			
			HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
			HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
			for (DTNHost neiHost : neiList){
				List<Double> t= new ArrayList<Double>();
				t.add(SimClock.getTime());
				startTime.put(neiHost, t);//����Ѵ����ھӽڵ�Ŀ�ʼʱ��
			}
			
			List<DTNHost> futureList = new ArrayList<DTNHost>();//(�ھ������ڵ�δ���ڵ㼯��)
			List<NetworkInterface> futureNeiList = new ArrayList<NetworkInterface>();//(Ԥ��δ���ھӵĽڵ㼯��)
					
			Collection<DTNHost> temporalNeighborsBefore = startTime.keySet();//ǰһʱ�̵��ھӣ�ͨ������Ա���һʱ�̵��ھӣ���֪����Щ���¼���ģ���Щ�����뿪��			
			Collection<DTNHost> temporalNeighborsNow = new ArrayList<DTNHost>();//���ڼ�¼��ǰʱ�̵��ھ�
			for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
				
				HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//ȡ��timeʱ�̵������
				GridCell cell = ginterfaces.get(host.getInterface(1));//�ҵ���ʱָ���ڵ�����������λ��
				
				int[] number = cell.getNumber();
				List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//��ȡ�����ھӵ����񣨵�ǰʱ�̣�
				
				for (GridCell c : cellList){	//�����ڲ�ͬʱ��ά���ϣ�ָ���ڵ���Χ������ھ�
					if (!cellmap.get(time).containsKey(c))
						continue;
					temporalNeighborsNow.addAll(cellmap.get(time).get(c));
					for (DTNHost ni : cellmap.get(time).get(c)){//��鵱ǰԤ��ʱ��㣬���е��ھӽڵ�
						if (ni == this.host)//�ų�����ڵ�
							continue;
						if (!neiList.contains(ni))//��������ھ���û�У���һ����δ����������ھ�					
							futureList.add(ni); //��Ϊδ�����ᵽ����ھ�(��Ȼ���ڵ�ǰ���е��ھӣ�Ҳ���ܻ���;�뿪��Ȼ���ٻ���)
										
						/**�����δ��������ھӣ�ֱ��get�᷵�ؿ�ָ�룬����Ҫ�ȼ�startTime��leaveTime�ж�**/
						if (startTime.containsKey(ni)){
							if (leaveTime.isEmpty())
								break;
							if (startTime.get(ni).size() == leaveTime.get(ni).size()){//����������һ�����ھӽڵ��뿪�����					
								List<Double> mutipleTime= leaveTime.get(ni);
								mutipleTime.add(time);
								startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
							}
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
				
				for (DTNHost h : temporalNeighborsBefore){//����Ա���һʱ�̺���һʱ�̵��ھӽڵ㣬�Ӷ��ҳ��뿪���ھӽڵ�
					if (!temporalNeighborsNow.contains(h)){
						List<Double> mutipleTime= leaveTime.get(h);
						mutipleTime.add(time);
						leaveTime.put(h, mutipleTime);//�����µ��뿪ʱ�����
					}						
				}
				temporalNeighborsBefore.clear();
				temporalNeighborsBefore = temporalNeighborsNow;
				temporalNeighborsNow.clear();	
			}
			
			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //��Ԫ��ϲ���ʼ�ͽ���ʱ��
					new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
			
			
			return predictTime;
		}
		
		public List<GridCell> getNeighborCells(double time, int row, int col, int z){
			HashMap<GridCell, List<DTNHost>> cellToHost = cellmap.get(time);//��ȡtimeʱ�̵�ȫ�������
			List<GridCell> GC = new ArrayList<GridCell>();
			/***********************************************************************/
			switch(this.gridLayer){
			case 1 : 
			/*��������ָ�*/
				for (int i = -1; i < 2; i += 1){
					for (int j = -1; j < 2; j += 1){
						for (int k = -1; k < 2; k += 1){
							GC.add(cells[row+i][col+j][z+k]);
						}
					}
				}
				break;
			case 2 : {
			/*��������ָ�*/
				for (int i = -3; i <= 3; i += 1){
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
			}
			break;
			default :/*��������ָ�*/
				for (int i = -1; i < 2; i += 1){
					for (int j = -1; j < 2; j += 1){
						for (int k = -1; k < 2; k += 1){
							GC.add(cells[row+i][col+j][z+k]);
							
						}
					}
				}
				break;
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
		
		
		/**
		 * ��ǰ�����˸��������һ�������ڵ�����������������ɹ����Ӧ��������������ݴ˱�Ϳ��Լ����໥֮��δ���Ĺ�ϵ���������ټ�����
		 */
		public void updateGrid_without_OrbitCalculation(){
			if (gridLocation.isEmpty())//��ʼ��ִֻ��һ��
				System.out.println("girdInfo errors!  "+gridLocation);
				//initializeGridLocation();
			if (periodMap.isEmpty()){
				for (DTNHost h : hosts){//��ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
					double period = getPeriodofOrbit(h);
					this.periodMap.put(h, period);
				}	
			}
			ginterfaces.clear();//ÿ�����
			//Coord location = new Coord(0,0); 	// where is the host
			double simClock = SimClock.getTime();
			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
				for (DTNHost host : hosts){
					List<GridCell> gridCellList = this.gridLocation.get(host);//��ȡ�ڵ�host��һ����������ڵ����������������б�
					List<Double> timeList = this.gridTime.get(host);//������Щ��������Ӧ��ʱ�䣨��һ�������ڣ�
					double period = this.periodMap.get(host);//��Ӧ�Ĺ������ 
					double t0 = time;
					GridCell cell = new GridCell();
					boolean label = false;
					if (time >= period)
						t0 = t0 % period;//�������ھ�ȡ�����
					assert timeList.size() == gridCellList.size() : " Size of Grid Table doesn't match!!! ";
					for (int iterator = 0; iterator < timeList.size(); iterator++){
						if (timeList.get(iterator) >= t0){
							cell = gridCellList.get(iterator);
							label = true;
							break;
						}
						//iterator++;//�ҵ���timeListʱ���Ӧ����������λ��,iterator ������������list�е�ָ��						
					}				
					//System.out.println(host+" number "+cell.getNumber()[0]+cell.getNumber()[1]+cell.getNumber()[2]);
					//System.out.println(host+" error!!! "+label);
					assert label : "grid calculation error";
					
					this.ginterfaces.put(host.getInterface(1), cell);
					
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
		
		/**
		 * GridRouter�ĸ��¹��̺���
		 */
		public void updateGrid_with_OrbitCalculation(){			
			ginterfaces.clear();//ÿ�����
			Coord location = new Coord(0,0); 	// where is the host
			double simClock = SimClock.getTime();

			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
				for (DTNHost host : hosts){
					
					/**���Դ���**/
					location.setLocation3D(((SatelliteMovement)host.getMovementModel()).getSatelliteCoordinate(time));
					//System.out.println(host+"  "+location);
					/**���Դ���**/
					
					//location.my_Test(time, 0, host.getParameters());
					
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
			assert row > 0 && row <= rows && col > 0 && col <= cols && z > 0 && z <= zs
					: "Location " + c + " is out of world's bounds";		
			return this.cells[row][col][z];
		}
		
		public void setHostsList(List<DTNHost> hosts){
			this.hosts = hosts;
		}
		
		public class GridCell {
			// how large array is initially chosen
			private static final int EXPECTED_INTERFACE_COUNT = 18;
			//private ArrayList<NetworkInterface> interfaces;//GridCell��������ά������ӿ��б�����¼�ڴ������ڵĽڵ㣬����ȫ��������˵����Ҫ��֤ͬһ������ӿڲ���ͬʱ����������GridCell��
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
	
	/**
	 * ����gridSearch��·�������㷨�У���Ҫ���ڴ洢����õ����м�ڵ�ľ�����Ϣ�������ں�������
	 */
	public class hostInfo{
		
		DTNHost host;
		double distance;
		double time;
		double waitTime;
		public hostInfo(DTNHost host, double distance, double time, double waitTime){
			this.host = host;
			this.distance = distance;
			this.time = time;
			this.waitTime = waitTime;
		}
		/**
		 * ð������
		 * @param distanceList
		 * @return
		 */
		public ArrayList<Tuple<DTNHost, Double>> sort(ArrayList<Tuple<DTNHost, Double>> distanceList){
			for (int j = 0; j < distanceList.size(); j++){
				for (int i = 0; i < distanceList.size() - j - 1; i++){
					if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()){//��С���󣬴��ֵ���ڶ����Ҳ�
						Tuple<DTNHost, Double> var1 = distanceList.get(i);
						Tuple<DTNHost, Double> var2 = distanceList.get(i + 1);
						distanceList.remove(i);
						distanceList.remove(i);//ע�⣬һ��ִ��remove֮������List�Ĵ�С�ͱ��ˣ�����ԭ��i+1��λ�����ڱ����i
						//ע��˳��
						distanceList.add(i, var2);
						distanceList.add(i + 1, var1);
					}
				}
			}
			return distanceList;
		}
		/**
		 * ð������
		 * @param distanceList
		 * @return
		 */
		public ArrayList<Tuple<DTNHost, Double>> sortSpeed(ArrayList<Tuple<DTNHost, Double>> speedList){
			for (int j = 0; j < speedList.size(); j++){
				for (int i = 0; i < speedList.size() - j - 1; i++){
					if (speedList.get(i).getValue() < speedList.get(i + 1).getValue()){//��С���󣬴��ֵ���ڶ����Ҳ�
						Tuple<DTNHost, Double> var1 = speedList.get(i);
						Tuple<DTNHost, Double> var2 = speedList.get(i + 1);
						speedList.remove(i);
						speedList.remove(i);//ע�⣬һ��ִ��remove֮������List�Ĵ�С�ͱ��ˣ�����ԭ��i+1��λ�����ڱ����i
						//ע��˳��
						speedList.add(i, var2);
						speedList.add(i + 1, var1);
					}
				}
			}
			return speedList;
		}
		public double getTime(){
			return this.time;
		}
		public double getWaitTime(){
			return this.waitTime;
		}
		public double getdistance(){
			return this.distance;
		}
		public DTNHost getHost(){
			return this.host;
		}
	}
}


  