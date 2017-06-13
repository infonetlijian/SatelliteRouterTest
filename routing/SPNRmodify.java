/** 
 * Project Name:SatelliteRouterTest 
 * File Name:SPNR.java 
 * Package Name:routing 
 * Date:2017��3��31������4:21:56 
 * Copyright (c) 2017, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
*/  
  
package routing;  
/** 
 * ClassName:SPNR <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2017��3��31�� ����4:21:56 <br/> 
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
import java.util.List;
import java.util.Random;

import routing.SPNRmodify.GridNeighbors.GridCell;
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

public class SPNRmodify extends ActiveRouter{
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
	
	/**���ݻ�����������·������������洢�������ĵ���Ŀ�Ľڵ�����·������ѡ����·ʱֱ��ʹ��**/
	private HashMap<DTNHost, List<Tuple<List<Integer>, Boolean>>> multiPathFromNetgridTable = new HashMap<DTNHost, List<Tuple<List<Integer>, Boolean>>>();
	
	HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();//�ڵ��·�ɱ�
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//ָʾ��һ���ڵ㴦��æ��״̬����Ҫ�ȴ�
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//����ȫ�������ڵ��ھ���·����ʱ����Ϣ
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	
	private boolean routerTableUpdateLabel;
	private GridNeighbors GN;
	Random random = new Random();
	double RoutingTimeNow;
	/**
	 * ��ʼ��
	 * @param s
	 */
	public SPNRmodify(Settings s){
		super(s);
	}
	/**
	 * ��ʼ��
	 * @param r
	 */
	protected SPNRmodify(SPNRmodify r) {
		super(r);
		this.GN = new GridNeighbors(this.getHost());//���������ԭ���ǣ���ִ����һ����ʼ����ʱ��host��router��û����ɰ󶨲���
	}
	/**
	 * ���ƴ�router��
	 */
	@Override
	public MessageRouter replicate() {
		//this.GN = new GridNeighbors(this.getHost());
		return new SPNRmodify(this);
	}
	/**
	 * ִ��·�ɵĳ�ʼ������
	 */
	public void initialzation(){
		GN.setHost(this.getHost());//Ϊ��ʵ��GN��Router�Լ�Host֮��İ󶨣����޸ģ�������������������������������������������������������������������������������������������������
		this.GN.initializeGridLocation();
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
		this.RoutingTimeNow = SimClock.getTime();
		this.multiPathFromNetgridTable.clear();
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
				if (t == null){
					msg.removeProperty(MSG_ROUTERPATH);
					//throw new SimError("��ȡ·����Ϣʧ�ܣ�");	
				}						
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
		//PathSearch(msg);
		gridSearch(msg);
		
		//updatePredictionRouter(msg);//��Ҫ����Ԥ��
		if (this.routerTable.containsKey(msg.getTo())){//Ԥ��Ҳ�Ҳ�������Ŀ�Ľڵ��·������·��ʧ��
			//m.changeRouterPath(this.routerTable.get(m.getTo()));//�Ѽ��������·��ֱ��д����Ϣ����
			System.out.println("Ѱ·�ɹ�������    "+" Path length:  "+routerTable.get(msg.getTo()).size()+" routertable size: "+routerTable.size()+" Netgrid Path:  "+routerTable.get(msg.getTo()));
			return true;//�ҵ���·��
		}else{
			//System.out.println("Ѱ·ʧ�ܣ�����");
			return false;
		}
		
		//if (!this.getHost().getNeighbors().getNeighbors().isEmpty())//������ڵ㲻���ڹ���״̬��������ھӽڵ��·�ɸ���
		//	;	
	}
	
	/**
	 * ð������
	 * @param distanceList
	 * @return
	 */
	public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList){
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
	 * ����·���㷨������̰��ѡ�����ʽ��б������ҳ�����Ŀ�Ľڵ�����·��
	 * @param msg
	 */
	public void gridSearch(Message msg){
		double t0 = System.currentTimeMillis();
		System.out.println("start: "+t0);//����ͳ��·���㷨������ʱ��
		
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true�����˴θ���·�ɱ��Ѿ����¹��ˣ����Բ�Ҫ�ظ�����
			return;
		this.routerTable.clear();
		this.arrivalTime.clear();

		
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
		case 2:
			//GN.updateGrid_with_OrbitCalculation();//���������
			break;
		case 1://ͨ����ǰ���������洢�����ڵ�Ĺ����Ϣ���Ӷ����й����в��ٵ��ù�����㺯����Ԥ�����ͨ��������Ԥ��
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!���µ�ʱ��δ��޸�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			GN.updateNetGridInfo_without_OrbitCalculation(this.RoutingTimeNow);
			//GN.updateNetGridInfo_without_OrbitCalculation_without_gridTable();
			//GN.updateGrid_without_OrbitCalculation(this.RoutingTimeNow);//���������
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			break;
		}
		/**ȫ���Ĵ������ʼٶ�Ϊһ����**/
		double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
		/**��ʾ·�ɿ�ʼ��ʱ��**/
		//double RoutingTimeNow = SimClock.getTime();
		
		/**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/
		List<DTNHost> searchedSet = new ArrayList<DTNHost>();
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ���
		searchedSet.add(this.getHost());//��ʼʱֻ��Դ�ڵ�
		
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
		
		/**�����·��̽�⵽��һ���ھ����񣬲�����·�ɱ�**/
		
		int iteratorTimes = 0;
		int size = this.hosts.size();
		boolean updateLabel = true;
		boolean predictLable = false;

		arrivalTime.put(this.getHost(), this.RoutingTimeNow);//��ʼ������ʱ��
		//netgridArrivalTime.put(GN.getGridCellFromCoordNow(this.getHost()), this.RoutingTimeNow);//��ʼ������ʱ��
		
		/**���ȼ����У���������**/
		List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
		//List<Tuple<GridCell, Double>> PriorityQueue = new ArrayList<Tuple<GridCell, Double>>();
		//List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
		//List<Double> correspondingTimeinQueue = new ArrayList<Double>();
		/**���ȼ����У���������**/
		
		double TNMCostTime = 0;//�����㷨����ʱ����
		//int countTimes = 0;//�����ã���ɾ
		
		while(true){//Dijsktra�㷨˼�룬ÿ������ȫ�֣���ʱ����С�ļ���·�ɱ���֤·�ɱ�����Զ��ʱ����С��·��
			if (iteratorTimes >= size )//|| updateLabel == false)
				break; 
			updateLabel = false;
			
			for (DTNHost c : sourceSet){
				
				double t00 = System.currentTimeMillis();//���ӶȲ��Դ���
							
				//List<DTNHost> neiList = GN.getNeighborsNetgrids(c, netgridArrivalTime.get(c));//��ȡԴ������host�ڵ���ھӽڵ�(������ǰ��δ���ھ�)
				List<DTNHost> neighborHostsList = GN.getNeighborsHostsNow(GN.getGridCellFromCoordNow(c));//��ȡԴ������host�ڵ���ھӽڵ�(��ǰ���ھ�����)
				//System.out.println("RoutingHost and time :  "+this.getHost()+this.RoutingTimeNow+"  thisHostGrid:  "+thisHostGrid  +"  SourceNetgird:  "+c+"  contains:  "+GN.getHostsFromNetgridNow(c, this.RoutingTimeNow)+"  NeighborNetgrid:  "+neighborNetgridsList.keySet()+" contains: "+neighborNetgridsList.values()+"  sourceSet:  "+sourceSet);
				
//				List<DTNHost> neighborHostsFromTGM = this.getHost().getNeighbors().getNeighbors(c, SimClock.getTime());
//				if (neighborHostsFromTGM.containsAll(neighborHostsList)){
//					if (neighborHostsFromTGM.size() == neighborHostsList.size()){
//						System.out.println(c+ "'s neighbors equal "+(++countTimes));
//					}
//					else{
//						System.out.println(c+ "  their neighbors number are "+neighborHostsFromTGM.size()+" and "+neighborHostsList.size());
//						System.out.println(neighborHostsFromTGM+" and "+neighborHostsList);
//					}
//				}
//				else{
//					System.out.println("error TGM: "+neighborHostsFromTGM );
//				}
					
					
				
				double t01 = System.currentTimeMillis();//���ӶȲ��Դ���
				TNMCostTime += (t01-t00);				//���ӶȲ��Դ���
				
				
//				if (neighborNetgridsList.containsKey(thisHostGrid)){
//					neighborNetgridsList.remove(thisHostGrid);
//				}
				//System.out.println("searchedSet  "+searchedSet+"   sourceSet   "+sourceSet);
				/**�ж��Ƿ��Ѿ�����������Դ���񼯺��е�����**/
				if (searchedSet.contains(c))
					continue;				
				searchedSet.add(c);
				
				for (DTNHost eachNeighborHost : neighborHostsList){//startTime.keySet()���������е��ھӽڵ㣬����δ�����ھӽڵ�
					if (sourceSet.contains(eachNeighborHost))//ȷ������ͷ
						continue;
					//System.out.println("Host and time :  "+this.getHost()+this.RoutingTimeNow+"  thisHostGrid:  "+thisHostGrid  +"  SourceNetgird:  "+c+"  contains:  "+GN.getHostsFromNetgridNow(c, this.RoutingTimeNow)+"  NeighborNetgrid:  "+eachNeighborNetgrid+ " contains: "+neighborNetgridsList.get(eachNeighborNetgrid)+"  sourceSet:  "+sourceSet);
										
					double time = arrivalTime.get(c) + msg.getSize()/transmitSpeed;
					
					/**���·����Ϣ**/
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(c))
						path.addAll(this.routerTable.get(c));
					Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborHost.getAddress(), predictLable);
					path.add(thisHop);//ע��˳��
					/**���·����Ϣ**/
					
					/**ά����С����ʱ��Ķ���**/
					if (arrivalTime.containsKey(eachNeighborHost)){
						/**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
						if (time <= arrivalTime.get(eachNeighborHost)){
							if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborHost) < 0.1){//���ʱ����ȣ��������ѡ��
								
								/**ע�⣬�ڶԶ��н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
								int index = -1;
								for (Tuple<DTNHost, Double> t : PriorityQueue){
									if (t.getKey() == eachNeighborHost){
										index = PriorityQueue.indexOf(t);
									}
								}
								/**ע�⣬�������PriorityQueue���н��е�����ʱ�򣬲��ܹ���forѭ������Դ˶��н����޸Ĳ���������ᱨ��**/
								if (index > -1){
									PriorityQueue.remove(index);
									PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborHost, time));
									arrivalTime.put(eachNeighborHost, time);
									routerTable.put(eachNeighborHost, path);
								}
							}
						}
						/**���������Ƿ�����ͨ���������·��������У����ĸ�ʱ�����**/
					}
					else{						
						PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborHost, time));
						arrivalTime.put(eachNeighborHost, time);
						routerTable.put(eachNeighborHost, path);
					}
					/**�Զ��н�������**/
					sort(PriorityQueue);					
					updateLabel = true;
				}
			}
			iteratorTimes++;
			for (int i = 0; i < PriorityQueue.size(); i++){
				if (!sourceSet.contains(PriorityQueue.get(i).getKey())){
					sourceSet.add(PriorityQueue.get(i).getKey());//���µ�����������
					break;
				}
			}
				
//			if (netgridRouterTable.containsKey(msg.getTo()))//�����;�ҵ���Ҫ��·������ֱ���˳�����
//				break;
		}
		routerTableUpdateLabel = true;
		
		double t1 = System.currentTimeMillis();//����ͳ��·���㷨������ʱ��
		System.out.println("cost: "+ (t1-t0)+" TNMCostTime: "+TNMCostTime);
		throw new SimError("Pause");	
		//System.out.println(this.getHost()+" table: "+netgridRouterTable+" time : "+SimClock.getTime());
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
	 * ������һ���Ŀ�ѡ�ڵ��ַ���ϣ�ѡ��һ������ʵ���һ���ڵ㲢�ҵ���Ӧ��connection���з���
	 * @param address
	 * @return
	 */
	public Connection findConnectionFromHosts(Message msg, List<Integer> hostsInThisHop){
		if (hostsInThisHop.size() == 1){
			return findConnection(hostsInThisHop.get(0));
		}
		/**�ж����ѡ��һ���ڵ��ʱ��**/
		else{
			/**ȷ��һ���Ĵ��䲻����**/
			DTNHost destination = msg.getTo();
			for (int i = 0; i < hostsInThisHop.size(); i++){
				Connection connect = findConnection(hostsInThisHop.get(i));
				
				/**·���ҵ���·�����ܳ��ִ��󣬵��µ�ǰ·��������**/
				if (connect == null) 
					return null;
				/**·���ҵ���·�����ܳ��ִ��󣬵��µ�ǰ·��������**/
				
				if (connect.getOtherInterface(this.getHost().getInterface(1)).getHost() == destination)
					return connect;
			}
			/**ȷ��һ���Ĵ��䲻����**/
			/****************************************************************!!!!!���޸�!!!!!!**************************************************************************/
			int randomInt = this.random.nextInt(hostsInThisHop.size());
			Connection con = findConnection(hostsInThisHop.get(randomInt) - 1);//ע��Ҫ��һ����Ϊ��ArrayList�������±�
			if (con != null){
				return con;
			}
			/**һ����һ��ʧ�ܾͽ��б���Ѱ��**/
			else{
				for (int i = 0; i < hostsInThisHop.size(); i++){
					con = findConnection(i);
					/**�������п����ԣ��ҳ�һ���ɴ���ھӽڵ㣬���򷵻�null**/
					if (con != null)
						return con;
				}
			}
			
			return null;
			/****************************************************************!!!!!���޸�!!!!!!**************************************************************************/
		}
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
				
		Connection con = t.getValue();
		/**���������·��������������һ������·�Ѿ���ռ�ã�����Ҫ�ȴ�**/
		if (con.isTransferring()){
			this.busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
			System.out.println(this.getHost()+"  "+t.getKey()+"  "+
					t.getValue().getOtherNode(this.getHost())+" "+con+"  "+this.busyLabel.get(t.getKey().getId()));			
			return true;//˵��Ŀ�Ľڵ���æ
		}
		return false;
		/**���ڼ�����е���·ռ������������ڵ��Ƿ��ڶ��ⷢ�͵��������update�������Ѿ������ˣ��ڴ������ظ����**/
	}
	/**
	 * �Ӹ�����Ϣ��ָ����·�����Է�����Ϣ
	 * @param t
	 * @return
	 */
	public boolean sendMsg(Tuple<Message, Connection> t){
		if (t == null){	
			//throw new SimError("error! ");//���ȷʵ����Ҫ�ȴ�δ����һ���ڵ�͵ȣ��ȴ���һ��,���޸�!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
		
		private int cellSize;
		private int rows;
		private int cols;
		private int zs;//������ά����
		private  int worldSizeX;
		private  int worldSizeY;
		private  int worldSizeZ;//����
		
		private int gridLayer;
		
		/**ÿ��routing���и���ʱ�����ڴ洢ָ��ʱ�������״̬������ͽڵ��ӳ���ϵ**/
//		private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();
//		private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
		
		/**��ǰ˲ʱʱ�̵�����״̬����������ͽڵ��ӳ���ϵ**/
		HashMap<NetworkInterface, GridCell> interfaceToGridCell = new HashMap<NetworkInterface, GridCell>();
		HashMap<GridCell, List<DTNHost>> gridCellToHosts = new HashMap<GridCell, List<DTNHost>>();
		
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
				break;
			case 3:
				cellSize = (int) (transmitRange*0.0721687);//Layer=4
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
		/**
		 * ��ʼ�������̶�������
		 * @param cellSize
		 */
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
		}
		/**
		 * ��v���й��c����ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
		 */
		public void initializeGridLocation(){	
			this.host.getHostsList();
			for (DTNHost h : this.host.getHostsList()){//��ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
				double period = getPeriodofOrbit(h);
				this.periodMap.put(h, period);
				System.out.println(this.host+" now calculate "+h+"  "+period);
				
				List<GridCell> gridList = new ArrayList<GridCell>();
				List<Double> intoTime = new ArrayList<Double>();
				List<Double> outTime = new ArrayList<Double>();
				GridCell startCell = cellFromCoord(h.getCoordinate(0));//��¼��ʼ����
				for (double time = 0; time < period; time += updateInterval){
					Coord c = h.getCoordinate(time);
					GridCell gc = cellFromCoord(c);//���������ҵ������ľW��
					if (!gridList.contains(gc)){
						if (gridList.isEmpty()){
							startCell = gc;//��¼��ʼ����
							gridList.add(null);//����ʼ�����һ�ηſ�ָ�룬ռ��λ
							intoTime.add(time);
						}						
						gridList.add(gc);//��һ�μ�⵽�ڵ���������ע�⣬�߽��飡������ʼ�ͽ�����ʱ�򣡣���!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!��
						intoTime.add(time);//��¼��Ӧ�Ľ���ʱ��
						if (gc == startCell){
							gridList.set(0, startCell);
							intoTime.set(0, time);
						}
					}	
					else{
						//������ʼ�������������ʱ�䣬��һ�����������
						if (gc == startCell){
							gridList.set(0, startCell);
							intoTime.set(0, time);
						}						
					}
				}
				//System.out.println(h+" startCell "+h.getCoordinate(1)+" time: "+h.getCoordinate(0)+ "  "+h.getCoordinate(period)+ "  "+h.getCoordinate(6024)+ "  "+h.getCoordinate(6023));
				//System.out.println(h+" startCell "+startCell+" time: "+intoTime.get(0)+ "  "+intoTime.get(1)+"  "+intoTime.get(intoTime.size()-1)+"  "+gridLocation);
				gridLocation.put(h, gridList);//������һ���ڵ�ͼ�¼����
				gridTime.put(h, intoTime);
			}
			System.out.println(gridLocation);
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
		 * �ҵ�host�ڵ��ڵ�ǰʱ���Ӧ���ڵ�����
		 * @param host
		 * @param time
		 * @return
		 */
		public GridCell getGridCellFromCoordNow(DTNHost host){
			/**ע�����ʽ��õ��������꣬��ʵʱ��ά�������õ�����������֮������������**/
			return this.interfaceToGridCell.get(host.getInterface(1));
			//return cellFromCoord(host.getCoordinate(time));
		}
		
		/**
		 * �ҵ�host�ڵ���ʱ��time��Ӧ���ڵ�����
		 * @param host
		 * @param time
		 * @return
		 */
		public GridCell getGridCellFromCoordAtTime(DTNHost host, double time){
			/**ע�����ʽ��õ��������꣬��ʵʱ��ά�������õ�����������֮������������**/
			return this.interfaceToGridCell.get(host.getInterface(1));
			//return cellFromCoord(host.getCoordinate(time));
		}
		/**
		 * ��ȡָ��ʱ��㣬ָ��������ھ�����
		 * @param source
		 * @param time
		 * @return
		 */
		public HashMap<GridCell, Tuple<GridCell, List<DTNHost>>> getNeighborsNetgridsNow(GridCell source){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)	

			//HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			//GridCell cell = this.interfaceToGridCell.get(host.getInterface(1));
			int[] number = source.getNumber();//�õ����������ά����
			
			List<GridCell> cellList = getNeighborCells(number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
			/**�ҳ����е��ھ������Լ�������Ľڵ�**/
			HashMap<GridCell, Tuple<GridCell, List<DTNHost>>> neighborNetgridInfo = new HashMap<GridCell, Tuple<GridCell, List<DTNHost>>>();
			//List<Tuple<GridCell, List<DTNHost>>> gridInfoList = new ArrayList<Tuple<GridCell, List<DTNHost>>>();
			/**�ҳ����е��ھ������Լ�������Ľڵ�**/
			//assert cellmap.containsKey(time):" ʱ����� ";
			/**ȥ��������**/
			if (cellList.contains(source))
				cellList.remove(source);
			
			for (GridCell c : cellList){
				if (this.gridCellToHosts.containsKey(c)){//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
					List<DTNHost> hostList = new ArrayList<DTNHost>(this.gridCellToHosts.get(c));//�ҳ���һ���ھ������ڶ�Ӧ�����нڵ�
					Tuple<GridCell, List<DTNHost>> oneNeighborGrid = new Tuple<GridCell, List<DTNHost>>(c, hostList);
					neighborNetgridInfo.put(c, oneNeighborGrid);
				}
			}	
			
			//System.out.println(host+" �ھ��б�   "+hostList);
			return neighborNetgridInfo;
		}
		/**
		 * ��ȡ��ǰ����ʱ���£�ָ��������ھ������������е������ھӽڵ�
		 * @param source
		 * @param time
		 * @return
		 */
		public List<DTNHost> getNeighborsHostsNow(GridCell source){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)	
			//HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			//GridCell cell = this.interfaceToGridCell.get(host.getInterface(1));
			int[] number = source.getNumber();//�õ����������ά����
			
			List<GridCell> cellList = getNeighborCells(number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
			/**�ҳ����е��ھ������Լ�������Ľڵ�**/

			/**ȥ��������**/
			if (cellList.contains(source))
				cellList.remove(source);
			
			List<DTNHost> neighborHosts = new ArrayList<DTNHost>();
			
			for (GridCell c : cellList){
				if (this.gridCellToHosts.containsKey(c)){//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
					neighborHosts.addAll(this.gridCellToHosts.get(c));//�ҳ���һ���ھ������ڶ�Ӧ�����нڵ�
				}
			}	
			
			//System.out.println(host+" �ھ��б�   "+hostList);
			return neighborHosts;
		}
//		public List<DTNHost> getNeighbors(DTNHost host, double time){//��ȡָ��ʱ����ھӽڵ�(ͬʱ����Ԥ�⵽TTLʱ���ڵ��ھ�)
//			int num = (int)((time-SimClock.getTime())/updateInterval);
//			time = SimClock.getTime()+num*updateInterval;
//			
//			if (time > SimClock.getTime()+msgTtl*60){//��������ʱ���Ƿ񳬹�Ԥ��ʱ��
//				//assert false :"����Ԥ��ʱ��";
//				time = SimClock.getTime()+msgTtl*60;
//			}
//			
//			//double t0 = System.currentTimeMillis();
//			//System.out.println(t0);
//			
//			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
//			GridCell cell = ginterfaces.get(host.getInterface(1));
//			int[] number = cell.getNumber();
//			
//			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//�����ھӵ����񣨵�ǰʱ�̣�
//			List<DTNHost> hostList = new ArrayList<DTNHost>();//(�ھ������ڵĽڵ㼯��)
//			assert cellmap.containsKey(time):" ʱ����� ";
//			for (GridCell c : cellList){
//				if (cellmap.get(time).containsKey(c))//�������������˵�����ھ�����Ϊ�գ����治���κνڵ�
//					hostList.addAll(cellmap.get(time).get(c));
//			}	
//			if (hostList.contains(host))//������ڵ�ȥ��
//				hostList.remove(host);
//			
//			//double t1 = System.currentTimeMillis();
//			//System.out.println("search cost"+(t1-t0));
//			//System.out.println(host+" �ھ��б�   "+hostList);
//			return hostList;
//		}

//		public Tuple<HashMap<DTNHost, List<Double>>, //neiList Ϊ�Ѿ�������ĵ�ǰ�ھӽڵ��б�
//			HashMap<DTNHost, List<Double>>> getFutureNeighbors(List<DTNHost> neiList, DTNHost host, double time){
//			int num = (int)((time-SimClock.getTime())/updateInterval);
//			time = SimClock.getTime()+num*updateInterval;	
//			
//			HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
//			HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
//			for (DTNHost neiHost : neiList){
//				List<Double> t= new ArrayList<Double>();
//				t.add(SimClock.getTime());
//				startTime.put(neiHost, t);//����Ѵ����ھӽڵ�Ŀ�ʼʱ��
//			}
//			
//			List<DTNHost> futureList = new ArrayList<DTNHost>();//(�ھ������ڵ�δ���ڵ㼯��)
//			List<NetworkInterface> futureNeiList = new ArrayList<NetworkInterface>();//(Ԥ��δ���ھӵĽڵ㼯��)
//			
//			
//			Collection<DTNHost> temporalNeighborsBefore = startTime.keySet();//ǰһʱ�̵��ھӣ�ͨ������Ա���һʱ�̵��ھӣ���֪����Щ���¼���ģ���Щ�����뿪��			
//			Collection<DTNHost> temporalNeighborsNow = new ArrayList<DTNHost>();//���ڼ�¼��ǰʱ�̵��ھ�
//			for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
//				
//				HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//ȡ��timeʱ�̵������
//				GridCell cell = ginterfaces.get(host.getInterface(1));//�ҵ���ʱָ���ڵ�����������λ��
//				
//				int[] number = cell.getNumber();
//				List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//��ȡ�����ھӵ����񣨵�ǰʱ�̣�
//				
//				for (GridCell c : cellList){	//�����ڲ�ͬʱ��ά���ϣ�ָ���ڵ���Χ������ھ�
//					if (!cellmap.get(time).containsKey(c))
//						continue;
//					temporalNeighborsNow.addAll(cellmap.get(time).get(c));
//					for (DTNHost ni : cellmap.get(time).get(c)){//��鵱ǰԤ��ʱ��㣬���е��ھӽڵ�
//						if (ni == this.host)//�ų�����ڵ�
//							continue;
//						if (!neiList.contains(ni))//��������ھ���û�У���һ����δ����������ھ�					
//							futureList.add(ni); //��Ϊδ�����ᵽ����ھ�(��Ȼ���ڵ�ǰ���е��ھӣ�Ҳ���ܻ���;�뿪��Ȼ���ٻ���)
//										
//						/**�����δ��������ھӣ�ֱ��get�᷵�ؿ�ָ�룬����Ҫ�ȼ�startTime��leaveTime�ж�**/
//						if (startTime.containsKey(ni)){
//							if (leaveTime.isEmpty())
//								break;
//							if (startTime.get(ni).size() == leaveTime.get(ni).size()){//����������һ�����ھӽڵ��뿪�����					
//								List<Double> mutipleTime= leaveTime.get(ni);
//								mutipleTime.add(time);
//								startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
//							}
//							/*if (leaveTime.containsKey(ni)){//�����������һ����Ԥ��ʱ����ڴ��ھӻ��뿪����һ������Ǵ��ھӲ����ڴ�ʱ����ڻ��뿪�������
//								if (startTime.get(ni).size() == leaveTime.get(ni).size()){//����������һ�����ھӽڵ��뿪�����					
//									List<Double> mutipleTime= leaveTime.get(ni);
//									mutipleTime.add(time);
//									startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
//								}
//								else{
//									List<Double> mutipleTime= leaveTime.get(ni);
//									mutipleTime.add(time);
//									leaveTime.put(ni, mutipleTime);//�����µ��뿪ʱ�����
//								}	
//							}
//							else{
//								List<Double> mutipleTime= new ArrayList<Double>();
//								mutipleTime.add(time);
//								leaveTime.put(ni, mutipleTime);//�����µ��뿪ʱ�����
//							}*/
//						}
//						else{
//							//System.out.println(this.host+" ����Ԥ��ڵ�: "+ni+" ʱ��  "+time);
//							List<Double> mutipleTime= new ArrayList<Double>();
//							mutipleTime.add(time);
//							startTime.put(ni, mutipleTime);//�����µĿ�ʼʱ�����
//						}
//						/**�����δ��������ھӣ�ֱ��get�᷵�ؿ�ָ�룬����Ҫ�ȼ�startTime��leaveTime�ж�**/
//					}	
//				}
//				
//				for (DTNHost h : temporalNeighborsBefore){//����Ա���һʱ�̺���һʱ�̵��ھӽڵ㣬�Ӷ��ҳ��뿪���ھӽڵ�
//					if (!temporalNeighborsNow.contains(h)){
//						List<Double> mutipleTime= leaveTime.get(h);
//						mutipleTime.add(time);
//						leaveTime.put(h, mutipleTime);//�����µ��뿪ʱ�����
//					}						
//				}
//				temporalNeighborsBefore.clear();
//				temporalNeighborsBefore = temporalNeighborsNow;
//				temporalNeighborsNow.clear();	
//			}
//			
//			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //��Ԫ��ϲ���ʼ�ͽ���ʱ��
//					new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
//			
//			
//			return predictTime;
//		}
		
		public List<GridCell> getNeighborCells(int row, int col, int z){
			//HashMap<GridCell, List<DTNHost>> cellToHost = this.gridCellToHosts;//��ȡtimeʱ�̵�ȫ�������
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
			
			case 3:{
				/*�Ĳ������ָ�*/
				for (int i = -7; i <= 7; i += 1){
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
		 * �����µ������ȡ��ʽ�£�·���㷨������
		 * @param simClock
		 */
		public void updateNetGridInfo_without_OrbitCalculation_without_gridTable(){
			//if (gridLocation.isEmpty())//��ʼ��ִֻ��һ��
			//	initializeGridLocation();
			
			HashMap<NetworkInterface, GridCell> ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����;			
			HashMap<GridCell, List<DTNHost>> cellToHost = new HashMap<GridCell, List<DTNHost>>();
			
			for (DTNHost host : hosts){
				GridCell cell = null;
			
				cell = cellFromCoord(host.getLocation());
				//cell = this.getGridCellFromCoordNow(host);
				if (cell == null)
					throw new SimError(" cell error!");
				
				ginterfaces.put(host.getInterface(1), cell);
				
				List<DTNHost> hostList = new ArrayList<DTNHost>();
				if (cellToHost.containsKey(cell)){
					hostList.addAll(cellToHost.get(cell));	
				}
				hostList.add(host);
				cellToHost.put(cell, hostList);
			}		
			gridCellToHosts.clear();
			interfaceToGridCell.clear();
			
			gridCellToHosts.putAll(cellToHost);
			interfaceToGridCell.putAll(ginterfaces);				
		}
		
		
		public void updateNetGridInfo_without_OrbitCalculation(double simClock){
			if (gridLocation.isEmpty())//��ʼ��ִֻ��һ��
				initializeGridLocation();
			
			HashMap<NetworkInterface, GridCell> ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����;
			//ginterfaces.clear();//ÿ�����
			//Coord location = new Coord(0,0); 	// where is the host
			//double simClock = SimClock.getTime();
			//System.out.println("update time:  "+ simClock);
				
			//int[] coordOfNetgrid;
			
			HashMap<GridCell, List<DTNHost>> cellToHost = new HashMap<GridCell, List<DTNHost>>();
			for (DTNHost host : hosts){
				/**��¼�����ڵ�������������**/
				List<GridCell> gridCellList = this.gridLocation.get(host);
				/**��¼�����ڵ�����������ʱ��Ӧ�Ľ���ʱ��**/
				List<Double> timeList = this.gridTime.get(host);

				if (gridCellList.size() != timeList.size()){
					throw new SimError("���Ԥ��õ������������⣡");	
				}
				/**���ǹ������**/
				double period = this.periodMap.get(host);
				double t0 = simClock;
				GridCell cell = null;
				boolean label = false;
					
				if (simClock > period)
					t0 = t0 % period;//�������ھ�ȡ�����
				
				if (timeList.get(0) > timeList.get(timeList.size() - 1)){
					for (int iterator = 1; iterator < timeList.size(); iterator++){
						if (timeList.get(iterator) > t0){
							/**ע�⣬����iterator - 1��û�д�ģ���Ϊ����iterator����˵������һ����������ʱ�䣬���if�������㣬��ô��ʱ�̽ڵ�Ӧ�ô���ǰһ������λ�õ���**/
							int[] coordOfNetgrid = gridCellList.get(iterator - 1).getNumber();
							cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
							//cell = gridCellList.get(iterator - 1);
							label = true;
							break;
						}
					}
					/**�ж��ǲ��Ǵ��ڹ�����ڵ�ĩβʱ�̣��߽�λ��**/
					if (t0 >= timeList.get(0) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(0).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						label = true;
					}
					
					if (t0 >= timeList.get(timeList.size() - 1) & t0 < timeList.get(0) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						label = true;
					}	
				}
				else{
					for (int iterator = 1; iterator < timeList.size(); iterator++){
						if (timeList.get(iterator) > t0){
							/**ע�⣬����iterator - 1��û�д�ģ���Ϊ����iterator����˵������һ����������ʱ�䣬���if�������㣬��ô��ʱ�̽ڵ�Ӧ�ô���ǰһ������λ�õ���**/
							int[] coordOfNetgrid = gridCellList.get(iterator - 1).getNumber();
							cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
							//cell = gridCellList.get(iterator - 1);
							label = true;
							break;
						}
					}
					/**�ж��ǲ��Ǵ��ڹ�����ڵ�ĩβʱ�̣��߽�λ��**/
					if (t0 >= timeList.get(timeList.size() - 1) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						//cell = gridCellList.get(0);
						label = true;
					}	
				}
			
//				for (double t : timeList){
//					if (t >= t0){
//						cell = gridCellList.get(iterator);
//						label = true;
//						break;
//					}
//					iterator++;//�ҵ���timeListʱ���Ӧ����������λ��,iterator ������������list�е�ָ��						
//				}				
				//System.out.println(host+"  "+cell+" time "+SimClock.getTime());

				if (label != true){
					/**���ǰ��û���ҵ�����˵����ʱ�ڵ�����һ����������ڵģ��������һ������͵�һ������Ľ��紦,Ӧ��ȡ���һ������**/
//					int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
//					cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
					System.out.println(simClock+"  "+host);
					throw new SimError("grid calculation error");
				}
				
//				/**��֤��**/
//				int[] coordOfNetgrid = cell.getNumber();
//				int[] TRUEcoordOfNetgrid = this.getGridCellFromCoordAtTime(host, simClock).getNumber();
//				if (!(TRUEcoordOfNetgrid[0] == coordOfNetgrid[0] & TRUEcoordOfNetgrid[0] == coordOfNetgrid[0] & TRUEcoordOfNetgrid[0] == coordOfNetgrid[0])){
//					System.out.println(simClock+"  "+host+" coordofnetgrid "+TRUEcoordOfNetgrid[0]+" "+ TRUEcoordOfNetgrid[1]+" "+TRUEcoordOfNetgrid[2]+"  "+coordOfNetgrid[0]+" "+coordOfNetgrid[1]+" "+coordOfNetgrid[2]);
//					//cell = this.getGridCellFromCoord(host, simClock);
//					//throw new SimError("grid calculation error");	
//				}					
//				/**��֤��**/

				ginterfaces.put(host.getInterface(1), cell);
				
				List<DTNHost> hostList = new ArrayList<DTNHost>();
				if (cellToHost.containsKey(cell)){
					hostList = cellToHost.get(cell);	
				}
				hostList.add(host);
				cellToHost.put(cell, hostList);
			}		
			gridCellToHosts.clear();
			interfaceToGridCell.clear();
			
			gridCellToHosts.putAll(cellToHost);
			interfaceToGridCell.putAll(ginterfaces);
			//ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����
			
			
//			cellmap.put(simClock, cellToHost);
//			gridmap.put(simClock, ginterfaces);//Ԥ��δ��timeʱ����ڵ������֮��Ķ�Ӧ��ϵ
			//ginterfaces.clear();//ÿ�����
			
			//CreateGrid(cellSize);//����cells��new��ginterfaces��new
				
		}
		/**
		 * ��ǰ�����˸��������һ�������ڵ�����������������ɹ����Ӧ��������������ݴ˱�Ϳ��Լ����໥֮��δ���Ĺ�ϵ���������ټ�����
		 */
//		public void updateGrid_without_OrbitCalculation(double simClock){
//			if (gridLocation.isEmpty())//��ʼ��ִֻ��һ��
//				initializeGridLocation();
//			
//			ginterfaces.clear();//ÿ�����
//			//Coord location = new Coord(0,0); 	// where is the host
//			//double simClock = SimClock.getTime();
//			System.out.println("update time:  "+ simClock);
//			for (double time = simClock; time <= simClock; time += updateInterval){
//				//double ts = System.currentTimeMillis();
//				//System.out.println(this.host+"   "+ SimClock.getTime()+" start time "+ts);
//				
//				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
//				for (DTNHost host : hosts){
//					/**��¼�����ڵ�������������**/
//					List<GridCell> gridCellList = this.gridLocation.get(host);
//					/**��¼�����ڵ�����������ʱ��Ӧ�Ľ���ʱ��**/
//					List<Double> timeList = this.gridTime.get(host);
//					assert gridCellList.size() == timeList.size() : "���Ԥ��õ������������⣡";
//					
//					double period = this.periodMap.get(host);
//					double t0 = time;
//					GridCell cell = new GridCell();
//					boolean label = false;
//					int iterator = 0;
//					if (time >= period)
//						t0 = t0 % period;//�������ھ�ȡ�����
//					for (double t : timeList){
//						if (t >= t0){
//							cell = gridCellList.get(iterator);
//							label = true;
//							break;
//						}
//						iterator++;//�ҵ���timeListʱ���Ӧ����������λ��,iterator ������������list�е�ָ��						
//					}				
//					//System.out.println(host+" number "+cell.getNumber()[0]+cell.getNumber()[1]+cell.getNumber()[2]);
//					//System.out.println(host+" error!!! "+label);
//					assert label : "grid calculation error";
//					
//					this.ginterfaces.put(host.getInterface(1), cell);
//					
//					List<DTNHost> hostList = new ArrayList<DTNHost>();
//					if (cellToHost.containsKey(cell)){
//						hostList = cellToHost.get(cell);	
//					}
//					hostList.add(host);
//					cellToHost.put(cell, hostList);
//				}		
//				cellmap.put(time, cellToHost);
//				gridmap.put(time, ginterfaces);//Ԥ��δ��timeʱ����ڵ������֮��Ķ�Ӧ��ϵ
//				//ginterfaces.clear();//ÿ�����
//				ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����
//				//CreateGrid(cellSize);//����cells��new��ginterfaces��new
//				
//				//double te = System.currentTimeMillis();
//				//System.out.println(this.host+"   "+ SimClock.getTime()+" execution time "+(te-ts));
//			}
//		}
		
		/**
		 * GridRouter�ĸ��¹��̺���
		 */
//		public void updateGrid_with_OrbitCalculation(){			
//			ginterfaces.clear();//ÿ�����
//			Coord location = new Coord(0,0); 	// where is the host
//			double simClock = SimClock.getTime();
//
//			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
//				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
//				for (DTNHost host : hosts){
//					
//					/**���Դ���**/
//					location.setLocation3D(((SatelliteMovement)host.getMovementModel()).getSatelliteCoordinate(time));
//					//System.out.println(host+"  "+location);
//					/**���Դ���**/
//					
//					//location.my_Test(time, 0, host.getParameters());
//					
//					GridCell cell = updateLocation(time, location, host);//������ָ��ʱ��ڵ������Ĺ�����ϵ
//					List<DTNHost> hostList = new ArrayList<DTNHost>();
//					if (cellToHost.containsKey(cell)){
//						hostList = cellToHost.get(cell);	
//					}
//					hostList.add(host);
//					cellToHost.put(cell, hostList);
//				}		
//				cellmap.put(time, cellToHost);
//				gridmap.put(time, ginterfaces);//Ԥ��δ��timeʱ����ڵ������֮��Ķ�Ӧ��ϵ
//				//ginterfaces.clear();//ÿ�����
//				ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����
//				//CreateGrid(cellSize);//����cells��new��ginterfaces��new
//			}
//			
//		}
		
		
//		public GridCell updateLocation(double time, Coord location, DTNHost host){
//			GridCell cell = cellFromCoord(location);
//			//cell.addInterface(host.getInterface(1));
//			ginterfaces.put(host.getInterface(1), cell);
//			return cell;
//		}
	


		/**
		 * ��������c�ҵ�c����������
		 * @param c
		 * @return
		 */
		private GridCell cellFromCoord(Coord c) {
			// +1 due empty cells on both sides of the matrix
			int row = (int)(c.getY()/cellSize) + 1; 
			int col = (int)(c.getX()/cellSize) + 1;
			int z = (int)(c.getZ()/cellSize) + 1;
			if (!(row > 0 && row <= rows && col > 0 && col <= cols))
				throw new SimError("Location " + c + " is out of world's bounds");
			//assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " + 
			//c + " is out of world's bounds";
		
			return this.cells[row][col][z];
		}
		
		public void setHostsList(List<DTNHost> hosts){
			this.hosts = hosts;
		}
		
		/**
		 * �½��ڲ��࣬����ʵ�����񻮷֣��洢�����������ɢ����
		 */
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
					"cell number: "+ number[0]+" "+number[1]+" "+number[2];
			}
		}
	}
}
