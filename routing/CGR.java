/** 
 * Project Name:SatelliteNetworkSimulationPlatform 
 * File Name:DijsktraSearchBasedonTemporalGraph.java 
 * Package Name:routing 
 * Date:2017年4月6日上午11:09:57 
 * Copyright (c) 2017, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
*/  
  
/* 
 * Copyright 2016 University of Science and Technology of China , Infonet
 * Written by LiJian.
 */
package routing;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import routing.SPNR.GridNeighbors.GridCell;
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
/** 
 * ClassName:DijsktraSearchBasedonTemporalGraph <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2017年4月6日 上午11:09:57 <br/> 
 * @author   USTC, LiJian
 * @version   
 * @since    JDK 1.7 
 * @see       
 */



public class CGR extends ActiveRouter{
	/**自己定义的变量和映射等
	 * 
	 */
	public static final String MSG_WAITLABEL = "waitLabel";
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	public static final String MSG_ROUTERPATH = "routerPath";  //定义字段名称，假设为MSG_MY_PROPERTY
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";

	private static final double SPEEDOFLIGHT = 299792458;//光速，近似3*10^8m/s
	private static final double MESSAGESIZE = 1024000;//1MB
	private static final double  HELLOINTERVAL = 30;//hello包发送间隔
	
	int[] predictionLabel = new int[2000];
	double[] transmitDelay = new double[2000];//1000代表总的节点数
	//double[] liveTime = new double[2000];//链路的生存时间，初始化时自动赋值为0
	double[] endTime = new double[2000];//链路的生存时间，初始化时自动赋值为0
	
	private boolean msgPathLabel;//此标识指示是否在信息头部中标识路由路径
	private double	transmitRange;//设置的可通行距离阈值
	private List<DTNHost> hosts;//全局节点列表
	
	HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();//节点的路由表
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//指示下一跳节点处于忙的状态，需要等待
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//新增全局其它节点邻居链路生存时间信息
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	
	Random random = new Random();//用于在相同時延開銷下進行隨機選擇
	private boolean routerTableUpdateLabel;
	double RoutingTimeNow;
	double simEndTime;
	private HashMap<DTNHost, List<Tuple<DTNHost, DTNHost>>> contactGraph;
	
	/**
	 * 传入事先定好的接触图
	 * @param contactGraph
	 */
	public void setContactGraph(HashMap<DTNHost, List<Tuple<DTNHost, DTNHost>>> contactGraph){
		this.contactGraph = contactGraph;
	}
	/**
	 * 读取接触图
	 * @return
	 */
	public HashMap<DTNHost, List<Tuple<DTNHost, DTNHost>>> getContactGraph(){
		return this.contactGraph;
	}
	
	/**
	 * 初始化
	 * @param s
	 */
	public CGR (Settings s){
		super(s);
	}
	/**
	 * 初始化
	 * @param r
	 */
	protected CGR(CGR  r) {
		super(r);
	}
	/**
	 * 复制此router类
	 */
	@Override
	public MessageRouter replicate() {
		return new CGR(this);
	}
	/**
	 * 在Networkinterface类中执行链路中断函数disconnect()后，对应节点的router调用此函数
	 */
	@Override
	public void changedConnection(Connection con){
		super.changedConnection(con);

//		if (!con.isUp()){
//			if(con.isTransferring()){
//				if (con.getOtherNode(this.getHost()).getRouter().isIncomingMessage(con.getMessage().getId()))
//					con.getOtherNode(this.getHost()).getRouter().removeFromIncomingBuffer(con.getMessage().getId(), this.getHost());
//				super.addToMessages(con.getMessage(), false);//对于因为链路中断而丢失的消息，重新放回发送方的队列中，并且删除对方节点的incoming信息
//			}
//		}
	}
	/**
	 * 路由更新，每次调用路由更新时的主入口
	 */
	@Override
	public void update() {
		super.update();
		
		Settings settings = new Settings("Scenario");
		this.simEndTime = settings.getDouble("endTime");

		/*测试代码，保证neighbors和connections的一致性*/
		List<DTNHost> conNeighbors = new ArrayList<DTNHost>();
		for (Connection con : this.getConnections()){
			conNeighbors.add(con.getOtherNode(this.getHost()));
		}
		/*for (DTNHost host : this.getHost().getNeighbors().getNeighbors()){
			assert conNeighbors.contains(host) : "connections is not the same as neighbors";
		}
		*/
		//this.getHost().getNeighbors().changeNeighbors(conNeighbors);
		//this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.getConnections());//更新邻居节点数据库
		/*测试代码，保证neighbors和connections的一致性*/
		
		this.hosts = this.getHost().getNeighbors().getHosts();
		List<Connection> connections = this.getConnections();  //取得所有邻居节点
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		
		Settings s = new Settings(GROUPNAME_S);
		this.msgPathLabel = s.getBoolean(MSG_PATHLABEL);//从配置文件中读取传输速率
		
		if (isTransferring()) {//判断链路是否被占用
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//有邻居时需要进行hello包发送协议
			//helloProtocol();//执行hello包的维护工作
		}
		if (!canStartTransfer())//是否有林杰节点且有信息需要传送
			return;
		
		//如果全局链路状态有所改变，就需要重新计算所有路由
		/*boolean linkStateChange = false;
		if (linkStateChange == true){
			this.busyLabel.clear();
			this.routerTable.clear();
		}*/
		
		this.RoutingTimeNow = SimClock.getTime();
		/**设置标志位，保证在同一时间路由算法只更新一次**/
		routerTableUpdateLabel = false;
		
		if (messages.isEmpty())
			return;
		for (Message msg : messages){//尝试发送队列里的消息	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true)
				return;
		}
	}
	/**
	 * 检查此待传消息msg是否需要等待，等待原因可能是1.目的节点正在被占用；2.路由得到的路径是预测路径，下一跳节点需要等待一段时间才能到达
	 * @param msg
	 * @return 是否需要等待
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
	 * 更新路由表，寻找路径并尝试转发消息
	 * @param msg
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections, boolean msgPathLabel){
		if (msgPathLabel == true){//如果允许在消息中写入路径消息
			if (msg.getProperty(MSG_ROUTERPATH) == null){//通过包头是否已写入路径信息来判断是否需要单独计算路由(同时也包含了预测的可能)
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, connections, msgPathLabel);
				return sendMsg(t);
			}
			else{//如果是中继节点，就检查消息所带的路径信息
				Tuple<Message, Connection> t = 
						findPathFromMessage(msg);
				assert t != null: "读取路径信息失败！";
				return sendMsg(t);
			}
		}else{//不会再信息中写入路径信息，每一跳都需要重新计算路径
			Tuple<Message, Connection> t = 
					findPathFromRouterTabel(msg, connections, msgPathLabel);//按待发送消息顺序找路径，并尝试发送
			return sendMsg(t);
		}
	}
	/**
	 * 通过读取信息msg头部里的路径信息，来获取路由路径，如果失效，则需要当前节点重新计算路由
	 * @param msg
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromMessage(Message msg){
		assert msg.getProperty(MSG_ROUTERPATH) != null : 
			"message don't have routerPath";//先查看信息有没有路径信息，如果有就按照已有路径信息发送，没有则查找路由表进行发送
		List<Tuple<Integer, Boolean>> routerPath = (List<Tuple<Integer, Boolean>>)msg.getProperty(MSG_ROUTERPATH);
		
		int thisAddress = this.getHost().getAddress();
		assert msg.getTo().getAddress() != thisAddress : "本节点已是目的节点，接收处理过程错误";
		int nextHopAddress = -1;
		
		//System.out.println(this.getHost()+"  "+msg+" "+routerPath);
		boolean waitLable = false;
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i).getKey() == thisAddress){
				nextHopAddress = routerPath.get(i+1).getKey();//找到下一跳节点地址
				waitLable = routerPath.get(i+1).getValue();//找到下一跳是否需要等待的标志位
				break;//跳出循环
			}
		}
				
		if (nextHopAddress > -1){
			Connection nextCon = findConnection(nextHopAddress);
			if (nextCon == null){//能找到路径信息，但是却没能找到连接
				//说明是未来的contact，信息等待
//				if (!waitLable){//检查是不是有预测邻居链路
//					System.out.println(this.getHost()+"  "+msg+" 指定路径失效");
//					msg.removeProperty(this.MSG_ROUTERPATH);//清除原先路径信息!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//					Tuple<Message, Connection> t = 
//							findPathFromRouterTabel(msg, this.getConnections(), true);//清除原先路径信息之后再重新寻路
//					return t;
//				}
			}else{
				Tuple<Message, Connection> t = new 
						Tuple<Message, Connection>(msg, nextCon);
				return t;
			}
		}
		return null;	
	}

	/**
	 * 通过更新路由表，找到当前信息应当转发的下一跳节点，并且根据预先设置决定此计算得到的路径信息是否需要写入信息msg头部当中
	 * @param message
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromRouterTabel(Message message, List<Connection> connections, boolean msgPathLabel){
		
		if (updateRouterTable(message) == false){//在传输之前，先更新路由表
			return null;//若没有返回说明一定找到了对应路径
		}
		List<Tuple<Integer, Boolean>> routerPath = this.routerTable.get(message.getTo());
		
		if (msgPathLabel == true){//如果写入路径信息标志位真，就写入路径消息
			message.updateProperty(MSG_ROUTERPATH, routerPath);
		}
					
		Connection path = findConnection(routerPath.get(0).getKey());//取第一跳的节点地址
		if (path != null){
			Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, path);//找到与第一跳节点的连接
			return t;
		}
		else{			
			if (routerPath.get(0).getValue()){
				System.out.println("第一跳预测");
				return null;
				//DTNHost nextHop = this.getHostFromAddress(routerPath.get(0).getKey()); 
				//this.busyLabel.put(message.getId(), startTime);//设置一个等待
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
	 * 由节点地址找到对应的节点DTNHost
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
	 * 由下一跳节点地址寻找对应的邻居连接
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
	 * 更新路由表，包括1、更新已有链路的路径；2、进行全局预测
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){
		
		//this.routerTable.clear();
		//PathSearch(msg);
		gridSearch(msg);
		
		//updatePredictionRouter(msg);//需要进行预测
		if (this.routerTable.containsKey(msg.getTo())){//预测也找不到到达目的节点的路径，则路由失败
			//m.changeRouterPath(this.routerTable.get(m.getTo()));//把计算出来的路径直接写入信息当中
			System.out.println("寻路成功！！！    "+" Path length:  "+routerTable.get(msg.getTo()).size()+" routertable size: "+routerTable.size()+" Netgrid Path:  "+routerTable.get(msg.getTo()));
			return true;//找到了路径
		}else{
			//System.out.println("寻路失败！！！");
			return false;
		}
		
		//if (!this.getHost().getNeighbors().getNeighbors().isEmpty())//如果本节点不处于孤立状态，则进行邻居节点的路由更新
		//	;	
	}
	
	/**
	 * 核心路由算法，运用贪心选择性质进行遍历，找出到达目的节点的最短路径
	 * @param msg
	 */
	public void gridSearch(Message msg){
//		double t0 = System.currentTimeMillis();
//		System.out.println("start: "+t0);//用于统计路由算法的运行时间
		
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true则代表此次更新路由表已经更新过了，所以不要重复计算
			return;
		this.routerTable.clear();
		this.arrivalTime.clear();
		
		/**全网的传输速率假定为一样的**/
		double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
		/**表示路由开始的时间**/
		//double RoutingTimeNow = SimClock.getTime();
		
		/**添加链路可探测到的一跳邻居网格，并更新路由表**/
		List<DTNHost> searchedSet = new ArrayList<DTNHost>();
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//初始时只有源节点所
		searchedSet.add(this.getHost());//初始时只有源节点
		
		for (Connection con : this.getHost().getConnections()){//添加链路可探测到的一跳邻居，并更新路由表
			DTNHost neiHost = con.getOtherNode(this.getHost());
			sourceSet.add(neiHost);//初始时只有本节点和链路邻居		
			Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
			List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
			Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
			path.add(hop);//注意顺序
			arrivalTime.put(neiHost, time);
			routerTable.put(neiHost, path);
		}
		//System.out.println(this.getHost()+" :  "+routerTable);
		/**添加链路可探测到的一跳邻居网格，并更新路由表**/
		
		int iteratorTimes = 0;
		int size = this.hosts.size();
		boolean updateLabel = true;
		boolean predictLable = false;
		Neighbors nei = this.getHost().getNeighbors();
		
		Settings s = new Settings("Scenario");
		double updateInterval = s.getDouble("updateInterval");
		
		arrivalTime.put(this.getHost(), this.RoutingTimeNow);//初始化到达时间
		
		/**优先级队列，做排序用**/
		List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
		//List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
		//List<Double> correspondingTimeinQueue = new ArrayList<Double>();
		/**优先级队列，做排序用**/
		
		double TNMCostTime = 0;//测试算法运行时间用
		
		while(true){//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
			if (iteratorTimes >= size)// || updateLabel == false)
				break; 
			updateLabel = false;

			for (DTNHost c : sourceSet){
				
				//List<DTNHost> neiList = GN.getNeighborsNetgrids(c, netgridArrivalTime.get(c));//获取源集合中host节点的邻居节点(包括当前和未来邻居)
				//List<DTNHost> neiList = nei.getNeighbors(c, SimClock.getTime());
				
				/**获取contactGraph里面的未来可用链路(当前时刻直到TTL结束前)**/
				double t00 = System.currentTimeMillis();//复杂度测试代码
				
				List<DTNHost> neiList = new ArrayList<DTNHost>();
				double nextTime = arrivalTime.get(c);
				
				HashMap<DTNHost, Double> connectionSetUpTime = new HashMap<DTNHost, Double>();
				
				for (double endTime = nextTime + msgTtl; nextTime < endTime; nextTime += updateInterval){
					if (endTime >= this.simEndTime)
						break; 
//					/**四舍五入**/
//					BigDecimal b = new BigDecimal(nextTime);  
//					nextTime = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();  
					int index = (int)(nextTime * 10);
					Tuple<DTNHost, DTNHost> connection = this.getContactGraph().get(c).get(index);

					//System.out.println(nextTime+"  "+connection);
					DTNHost thisHost = connection.getValue() == c ? connection.getKey() : connection.getValue();
					neiList.add(thisHost);
					if (connectionSetUpTime.get(thisHost) != null)
						continue;
					else{
						connectionSetUpTime.put(thisHost, nextTime);
					}
				}
				/**获取contactGraph里面的未来可用链路**/
				double t01 = System.currentTimeMillis();//复杂度测试代码
				TNMCostTime += (t01-t00);				//复杂度测试代码
				
				/**判断是否已经是搜索过的源网格集合中的网格**/
				if (searchedSet.contains(c))
					continue;
				
				searchedSet.add(c);
				for (DTNHost eachNeighborNetgrid : neiList){//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
					if (sourceSet.contains(eachNeighborNetgrid))//确保不回头
						continue;
					
					double waitTime = connectionSetUpTime.get(eachNeighborNetgrid) - arrivalTime.get(c);
					if (waitTime <= 0)
						waitTime = 0;
					double time = arrivalTime.get(c) + msg.getSize()/transmitSpeed + waitTime;
					/**添加路径信息**/
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(c))
						path.addAll(this.routerTable.get(c));
					
					if (waitTime > 0)
						predictLable = true;
					else
						predictLable = false;
					
					Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
					path.add(thisHop);//注意顺序
					/**添加路径信息**/
					/**维护最小传输时间的队列**/
					if (arrivalTime.containsKey(eachNeighborNetgrid)){
						/**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
						if (time <= arrivalTime.get(eachNeighborNetgrid)){
							if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1){//如果时间相等，做随机化选择
								
								/**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
								int index = -1;
								for (Tuple<DTNHost, Double> t : PriorityQueue){
									if (t.getKey() == eachNeighborNetgrid){
										index = PriorityQueue.indexOf(t);
									}
								}
								/**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
								if (index > -1){
									PriorityQueue.remove(index);
									PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
									arrivalTime.put(eachNeighborNetgrid, time);
									routerTable.put(eachNeighborNetgrid, path);
								}
							}
						}
						/**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
					}
					else{						
						PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
						arrivalTime.put(eachNeighborNetgrid, time);
						routerTable.put(eachNeighborNetgrid, path);
					}
					/**对队列进行排序**/
					sort(PriorityQueue);	
					updateLabel = true;
				}
			}
			iteratorTimes++;
			for (int i = 0; i < PriorityQueue.size(); i++){
				if (!sourceSet.contains(PriorityQueue.get(i).getKey())){
					sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
					break;
				}
			}
				
//			if (routerTable.containsKey(msg.getTo()))//如果中途找到需要的路徑，就直接退出搜索
//				return;
		}
		routerTableUpdateLabel = true;
		
//		double t1 = System.currentTimeMillis();//用于统计路由算法的运行时间
//		System.out.println("cost: "+ (t1-t0)+" TNMCostTime: "+TNMCostTime);
//		throw new SimError("Pause");	
		
		//System.out.println(this.getHost()+" table: "+routerTable+" time : "+SimClock.getTime());
	}
	/**
	 * 冒泡排序
	 * @param distanceList
	 * @return
	 */
	public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList){
		for (int j = 0; j < distanceList.size(); j++){
			for (int i = 0; i < distanceList.size() - j - 1; i++){
				if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()){//从小到大，大的值放在队列右侧
					Tuple<DTNHost, Double> var1 = distanceList.get(i);
					Tuple<DTNHost, Double> var2 = distanceList.get(i + 1);
					distanceList.remove(i);
					distanceList.remove(i);//注意，一旦执行remove之后，整个List的大小就变了，所以原本i+1的位置现在变成了i
					//注意顺序
					distanceList.add(i, var2);
					distanceList.add(i + 1, var1);
				}
			}
		}
		return distanceList;
	}
	/**
	 * EASR(earliest arrival space routing algorithm)，执行最短路径路由算法
	 * @param msg
	 */
	public void PathSearch(Message msg){
		this.routerTable.clear();
		this.arrivalTime.clear();
		
		
		Neighbors nei = this.getHost().getNeighbors();
		/*nei.updateNeighbors(this.getHost(), connections);//更新邻居列表
		
		/*添加链路可探测到的一跳邻居，并更新路由表*/
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//初始时只有本节点
		
		for (Connection con : this.getHost().getConnections()){//添加链路可探测到的一跳邻居，并更新路由表
			DTNHost neiHost = con.getOtherNode(this.getHost());
			sourceSet.add(neiHost);//初始时只有本节点和链路邻居		
			Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
			List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
			Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
			path.add(hop);//注意顺序
			arrivalTime.put(neiHost, time);
			routerTable.put(neiHost, path);
		}
		System.out.println(this.getHost()+" :  "+routerTable);
		/*添加链路可探测到的一跳邻居，并更新路由表*/
		
		int iteratorTimes = 0;
		int size = this.hosts.size();
		double minTime = 100000;
		DTNHost minHost =null;
		boolean updateLabel = true;
		boolean predictLable = false;
		List<Tuple<Integer, Boolean>> minPath = new ArrayList<Tuple<Integer, Boolean>>();

		
		arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间
		
		while(true){//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
			if (iteratorTimes >= size || updateLabel == false)
				break; 
			updateLabel = false;
			minTime = 100000;
//			HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>> 
//						predictionList = new HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, 
//						HashMap<DTNHost, List<Double>>>>();//存储机制，如果之前已经算过就不用再重复计算了
			
			for (DTNHost host : sourceSet){
				//List<DTNHost> neiList = nei.getNeighbors(host, arrivalTime.get(host));//获取源集合中host节点的邻居节点(包括当前和未来邻居)
				/**只获取当前时刻的邻居**/
				List<DTNHost> neiList = nei.getNeighbors(host, SimClock.getTime());
				for (DTNHost neiHost : neiList){//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
					if (sourceSet.contains(neiHost))
						continue;
					if (arrivalTime.get(host) >= SimClock.getTime() + msgTtl)//出发时间就已经超出TTL预测时间的话就直接排除
						continue;
					
					double time = arrivalTime.get(host) + msg.getSize()/host.getInterface(1).getTransmitSpeed();
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(host))
						path.addAll(this.routerTable.get(host));
					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), predictLable);
					path.add(hop);//注意顺序
					if (time > SimClock.getTime() + msgTtl)
						continue;
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
			sourceSet.add(minHost);//将新的最短节点加入
//			if (routerTable.containsKey(msg.getTo()))//如果中途找到需要的路徑，就直接退出搜索
//				break;
		}
		System.out.println(this.getHost()+" table: "+routerTable+" time : "+SimClock.getTime());
	}
	

	public int transmitFeasible(DTNHost destination){//传输可行性,判断是不是已有到目的节点的路径，同时还要保证此路径的存在时间大于传输所需时间
		if (this.routerTable.containsKey(destination)){
			if (this.transmitDelay[destination.getAddress()] > this.endTime[destination.getAddress()] -SimClock.getTime())
				return 0;
			else
				return 1;//只有此时既找到了通往目的节点的路径，同时路径上的链路存在时间可以满足传输延时
		}
		return 2;
		
	}


	/**
	 * 对信息msg头部进行改写操作，对预测节点的等待标志进行置位
	 * @param fromHost
	 * @param host
	 * @param msg
	 * @param startTime
	 */
	public void addWaitLabelInMessage(DTNHost fromHost, DTNHost host, Message msg, double startTime){
		HashMap<DTNHost, Tuple<DTNHost, Double>> waitList = new HashMap<DTNHost, Tuple<DTNHost, Double>>();
		Tuple<DTNHost, Double> waitLabel = new Tuple<DTNHost, Double>(host, startTime);
		
		if (msg.getProperty(MSG_WAITLABEL) == null){					
			waitList.put(fromHost, waitLabel);//fromHost为需要等待的节点，host为下一跳的预测节点
			msg.addProperty(MSG_WAITLABEL, waitList);
		}else{
			waitList.putAll((HashMap<DTNHost, Tuple<DTNHost, Double>>)msg.getProperty(MSG_WAITLABEL));
			waitList.put(fromHost, waitLabel);
			msg.updateProperty(MSG_WAITLABEL, waitList);
		}
	}
	
	/**
	 * 通过信息头部内的路径信息(节点地址)找到对应的节点，DTNHost类
	 * @param path
	 * @return
	 */
	public List<DTNHost> getHostListFromPath(List<Integer> path){
		List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
		for (int i = 0; i < path.size(); i++){
			hostsOfPath.add(this.getHostFromAddress(path.get(i)));//根据节点地址找到DTNHost 
		}
		return hostsOfPath;
	}
	/**
	 * 通过节点地址找到对应的节点，DTNHost类
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
	 * 在算路由表时，预测指定路径上的链路存在时间
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
		if (path.size() > 1){//至少长度为2
			for (int i = 1; i < path.size() - 1; i++){
				if (i > path.size() -1)//超过长度，自动返回
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
	 * 计算通过预测节点到达，所需的传输时间(即传输时间加上等待时间)
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
										nei.getInterface(1).getTransmitSpeed()) + this.transmitRange*1000/SPEEDOFLIGHT;//取二者较小的传输速率;
			return waitTime;
		}
		else{
			assert false :"预测结果失效 ";
			return -1;
		}
	}
	/**
	 * 计算指定链路(两个节点之间)所需的传输时间
	 * @param msgSize
	 * @param nei
	 * @param host
	 * @return
	 */
	public double calculateDelay(int msgSize, DTNHost nei , DTNHost host){
		double transmitDelay = msgSize/((nei.getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : nei.getInterface(1).getTransmitSpeed()) + 
				this.transmitDelay[host.getAddress()] + getDistance(nei, host)*1000/SPEEDOFLIGHT;//取二者较小的传输速率
		return transmitDelay;
	}
	/**
	 * 计算当前节点与一跳邻居的传输延时
	 * @param msgSize
	 * @param host
	 * @return
	 */
	public double calculateNeighborsDelay(int msgSize, DTNHost host){
		double transmitDelay = msgSize/((this.getHost().getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : this.getHost().getInterface(1).getTransmitSpeed()) + getDistance(this.getHost(), host)*1000/SPEEDOFLIGHT;//取二者较小的传输速率
		return transmitDelay;
	}
	
	/**
	 * 计算两个节点之间的距离
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
	 * 根据节点地址找到，与此节点相连的连接
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
		return null;//没有在已有连接中找到通过指定节点的路径
	}
	/**
	 * 发送一个信息到特定的下一跳
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
	        } else if (retVal > 0) { //系统定义，只有TRY_LATER_BUSY大于0，即为1
	            return null;          // should try later -> don't bother trying others
	        }
		 return null;
	}

	/**
	 * 用于判断下一跳节点是否处于发送或接受状态
	 * @param t
	 * @return
	 */
	public boolean hostIsBusyOrNot(Tuple<Message, Connection> t){
		
		Connection con = t.getValue();
		/**检查所经过路径的情况，如果下一跳的链路已经被占用，则需要等待**/
		if (con.isTransferring()){
			this.busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
			System.out.println(this.getHost()+"  "+t.getKey()+"  "+
					t.getValue().getOtherNode(this.getHost())+" "+con+"  "+this.busyLabel.get(t.getKey().getId()));			
			return true;//说明目的节点正忙
		}
		return false;
		/**至于检查所有的链路占用情况，看本节点是否在对外发送的情况，在update函数中已经检查过了，在此无需重复检查**/
	}
	/**
	 * 从给定消息和指定链路，尝试发送消息
	 * @param t
	 * @return
	 */
	public boolean sendMsg(Tuple<Message, Connection> t){
		if (t == null){	
			assert false : "error!";//如果确实是需要等待未来的一个节点就等，先传下一个,待修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			return false;
		}
		else{
			if (hostIsBusyOrNot(t) == true)//假设目的节点处于忙的状态
				return false;//发送失败，需要等待
			if (tryMessageToConnection(t) != null)//列表第一个元素从0指针开始！！！	
				return true;//只要成功传一次，就跳出循环
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
		//判断该节点能否进行传输消息，存在以下情况一种以上的，直接返回，不更新,即现在信道已被占用：
		//情形1：本节点正在向外传输
		if (this.sendingConnections.size() > 0) {//protected ArrayList<Connection> sendingConnections;
			return true; // sending something
		}
		
		List<Connection> connections = getConnections();
		//情型2：没有邻居节点
		if (connections.size() == 0) {
			return false; // not connected
		}
		//情型3：有邻居节点，但自身与周围节点正在传输
		//模拟了无线广播链路，即邻居节点之间同时只能有一对节点传输数据!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//需要修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			if (!con.isReadyForTransfer()) {//isReadyForTransfer返回false则表示有信道在被占用，因此对于广播信道而言不能传输
				return true;	// a connection isn't ready for new transfer
			}
		}		
		return false;		
	}
	/**
	 * 此重写函数保证在传输完成之后，源节点的信息从messages缓存中删除
	 */
	@Override
	protected void transferDone(Connection con){
		String msgId = con.getMessage().getId();
		removeFromMessages(msgId);
	}
}
  