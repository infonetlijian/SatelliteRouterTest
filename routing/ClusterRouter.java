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
	/**自己定义的变量和映射等
	 * 
	 */
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	public static final String MSG_ROUTERPATH = "routerPath";  //定义字段名称，假设为MSG_MY_PROPERTY
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	private double	transmitRange;//设置的可通行距离阈值
	private static final double SPEEDOFLIGHT = 299792458;//近似3*10^8m/s
	private static final double MESSAGESIZE = 1024000;//1MB
	int[] predictionLabel = new int[2000];
	double[] transmitDelay = new double[2000];//1000代表总的节点数
	double[] liveTime = new double[2000];//链路的生存时间，初始化时自动赋值为0
	HashMap<DTNHost, List<Integer>> routerTable = new HashMap<DTNHost, List<Integer>>();
	private List<DTNHost> hosts;//全局节点列表
	private HashMap<DTNHost, Double> helloInterval =new HashMap<DTNHost, Double>();
	private static final double  HELLOINTERVAL = 30;//hello包发送间隔
	private boolean msgPathLabel;//此标识指示是否在信息头部中标识路由路径
	
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();
	protected HashMap<DTNHost,HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost,HashMap<DTNHost, double[]>>();//新增全局其它节点邻居链路生存时间信息
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
		
		/*测试代码，保证neighbors和connections的一致性*/
		List<DTNHost> conNeighbors = new ArrayList<DTNHost>();
		for (Connection con : this.getConnections()){
			conNeighbors.add(con.getOtherNode(this.getHost()));
		}
		/*for (DTNHost host : this.getHost().getNeighbors().getNeighbors()){
			assert conNeighbors.contains(host) : "connections is not the same as neighbors";
		}
		*/
		this.getHost().getNeighbors().changeNeighbors(conNeighbors);
		this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.getConnections());//更新邻居节点数据库
		/*测试代码，保证neighbors和connections的一致性*/
		
		this.hosts = this.getHost().getNeighbors().getHosts();
		List<Connection> connections = this.getConnections();  //取得所有邻居节点
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		
		Settings s = new Settings(GROUPNAME_S);
		this.msgPathLabel = s.getBoolean(MSG_PATHLABEL);//从配置文件中读取传输速率
		
		
		
		//System.out.println(this.getHost()+" router protocol "+connections+"  "+messages);
		
		if (isTransferring()) {//判断链路是否被占用
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//有邻居时需要进行hello包发送协议
			//helloProtocol();//执行hello包的维护工作
		}
		if (!canStartTransfer())//是否有林杰节点且有信息需要传送
			return;
		
		for (Message msg : messages){//尝试发送队列里的消息	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true)
				return;
		}
		/*	if (m.getFrom().getAddress() == this.getHost().getAddress()){//检查此信息是否是本节点产生的信息?
				if (findPathToSend(m, connections) == false)//发送失败则，循环找到一个能发出去的信息为止
					continue;
				else
					return;
			}else{//此信息是转发信息，直接读取去写入的路径
				if (relayMessage(m, connections) == false)//执行中继策略
					continue;//发送失败则，循环找到一个能发出去的信息为止
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
	 * 更新路由表，寻找路径并尝试转发消息
	 * @param m
	 * @param connections
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections, boolean msgPathLabel){
		if (msgPathLabel == true){//如果允许在消息中写入路径消息
			if (msg.getFrom() == this.getHost()){
				Tuple<Message, Connection> t = findPathFromRouterTabel(msg, connections, msgPathLabel, false);
				return sendMsg(t);
			}
			else{//如果是中继节点，就检查消息所带的路径信息
				Tuple<Message, Connection> t = findPathFromMessage(msg);
				assert t != null: "读取路径信息失败！";
				return sendMsg(t);
			}
		}else{//不会再信息中写入路径信息，每一跳都需要重新计算路径
			Tuple<Message, Connection> t = findPathFromRouterTabel(msg, connections, msgPathLabel, false);//按待发送消息顺序找路径，并尝试发送
			return sendMsg(t);
		}
	}
	
	public Tuple<Message, Connection> findPathFromMessage(Message msg){
		assert msg.getProperty(MSG_ROUTERPATH) != null : "message don't have routerPath";//先查看信息有没有路径信息，如果有就按照已有路径信息发送，没有则查找路由表进行发送
		List<Integer> routerPath = (List<Integer>)msg.getProperty(MSG_ROUTERPATH);
		int thisAddress = this.getHost().getAddress();
		assert msg.getTo().getAddress() != thisAddress : "本节点已是目的节点，接收处理过程错误";
		int nextHopAddress = -1;
		
		System.out.print(this.getHost()+"  "+msg+" "+routerPath);
		
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i) == thisAddress){
				nextHopAddress = routerPath.get(i+1);//找到下一跳节点地址
				break;//跳出循环
			}
		}
		if (nextHopAddress > -1){
			if (findConnection(nextHopAddress) == null){//能找到路径信息，但是却没能找到连接
				System.out.print(this.getHost()+"  "+msg+" 指定路径失效");
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, this.getConnections(), true, true);
						//如果是因为已有路径失效而更新路由，则需要吧更新位置位，否则调用message.addProperty会报错
				return t;
			}
		}
		return null;	
	}


	public Tuple<Message, Connection> findPathFromRouterTabel(Message message, List<Connection> connections, boolean msgPathLabel, boolean updateLabel){
		if (updateRouterTable(message) == false){//在传输之前，先更新路由表
			return null;
		}
		if (this.routerTable.containsKey(message.getTo())){
			List<Integer> routerPath = this.routerTable.get(message.getTo());

			
			if (msgPathLabel == true){//如果写入路径信息标志位真，就写入路径消息
				if (updateLabel == true)
					message.updateProperty(MSG_ROUTERPATH, routerPath);
				else
					message.updateProperty(MSG_ROUTERPATH, routerPath);//向信息中添加路由信息
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
	 * 进行全局轨道预测和当前链路计算
	 */
	public void updateGlobalInfo(){
		List<DTNHost> hosts = this.getHost().getNeighbors().getHosts();
	
		for (DTNHost host : hosts){//计算出各个节点的邻居和预测
			if (host != this.getHost()){
				host.getNeighbors().updateNeighbors(host, host.getConnections());
				this.neighborsList.put(host, host.getNeighbors().getNeighborsLiveTime());
			}
		}
		this.predictList = this.getHost().getNeighbors().getPotentialNeighborsStartTime();
	}
	/**
	 * Hello协议
	 */
	public void helloProtocol(){
		for (Connection con : this.getHost().getInterface(1).getConnections()){
			if (con.getOtherNode(this.getHost()) != this.getHost()){
				double nextTime = helloInterval.get(con.getOtherNode(this.getHost()));
				if (SimClock.getTime() >= nextTime){
					Message m = new Message(this.getHost(), con.getOtherNode(this.getHost()), "hello", 1000);//创建一个hello消息
					Tuple<Message, Connection> t = new Tuple<Message, Connection>(m, con);
					if (tryMessageToConnection(t) != null)
						helloInterval.put(con.getOtherNode(this.getHost()), nextTime + HELLOINTERVAL);
					
				}
			}
		}
	}
	/**
	 * 更新路由表，包括1、更新已有链路的路径；2、进行全局预测
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){

		if (!this.getHost().getNeighbors().getNeighbors().isEmpty()){//如果本节点不处于孤立状态，则进行邻居节点的路由更新
			
			updateGlobalInfo();//更新全局预测信息			
			this.routerTable.clear();
			
			updateNeighborsRouter(msg);//更新当前已建立连接的路由
			
			/*System.out.println(this.getHost() + "  " + this.routerTable);
			for (int i = 0; i < 10; i++){
				System.out.print(this.transmitDelay[i]+"  ");
			}
			System.out.println("");
			
			for (int i = 0; i < 10; i++){
				System.out.print(this.liveTime[i]+"  ");
			}
			System.out.println("");*/
			
			updatePredictionRouter(msg);//需要进行预测
			if (this.routerTable.containsKey(msg.getTo())){//预测也找不到到达目的节点的路径，则路由失败
				//m.changeRouterPath(this.routerTable.get(m.getTo()));//把计算出来的路径直接写入信息当中
				return true;//找到了路径
			}else
				return false;
		}
		else
			return false;
		
	}
	/**
	 * 向路由表中添加新的表项
	 * @param host
	 * @param path
	 * @param Delay
	 * @param predictionLable，代表此节点是否是预测到达的邻居节点
	 */
	public void addRouterTable(DTNHost host, List<Integer> path, double Delay, double liveTime, boolean predictionLable){
		this.routerTable.put(host, path);
		this.transmitDelay[host.getAddress()] = Delay;
		this.liveTime[host.getAddress()] = liveTime;
		if (predictionLable)
			predictionLabel[host.getAddress()] = 1;//对预测位置位
		else
			predictionLabel[host.getAddress()] = 0;//对预测位置位
	}
	/**
	 * 对没有直接相连链路的节点进行预测
	 */
	public void updatePredictionRouter(Message msg){
		int msgSize = msg.getSize();
		Settings s = new Settings(INTERFACENAME_S);
		this.transmitRange = s.getDouble(TRANSMIT_RANGE_S);//从配置文件中读取传输速率
		
		Collection<DTNHost> itsPotentialNeighbors = this.predictList.keySet();
		for (DTNHost host : itsPotentialNeighbors){
			double[] startTime = this.predictList.get(host);
			double waitTime = calculatePredictionDelay(msgSize, startTime[0], host);//计算等待此节点到来需要多长时间
			if (waitTime > 0){
				List<Integer> path = new ArrayList<Integer>();
				path.add(host.getAddress());
				if (this.transmitDelay[host.getAddress()] != 0){//代表此节点已找到了一条路径，可以通过多跳到达host节点
					if (waitTime < this.transmitDelay[host.getAddress()]){//即通过多跳到达节点host所需时间比等待host成为邻居所需时间还要长
						if (startTime[1] != startTime[0]){//不相等就说明此节点有可能成为未来的邻居，待检查！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
							addRouterTable(host, path, waitTime, startTime[1], true);//添加新项至路由表,同时置预测位为1
						}

					}
				}else{//说明到达此节点只有等未来二者接近才有可能通信									
					if (startTime[1] != startTime[0])//不相等就说明此节点有可能成为未来的邻居，待检查！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
						addRouterTable(host, path, waitTime, startTime[1], true);//添加新项至路由表,同时置预测位为1
				}
			}else{
				assert false : "计算的预测时间已作废！";
			}
		}
	}
	public int transmitFeasible(DTNHost destination){//传输可行性,判断是不是已有到目的节点的路径，同时还要保证此路径的存在时间大于传输所需时间
		if (this.routerTable.containsKey(destination)){
			if (this.transmitDelay[destination.getAddress()] > this.liveTime[destination.getAddress()])
				return 0;
			else
				return 1;//只有此时既找到了通往目的节点的路径，同时路径上的链路存在时间可以满足传输延时
		}
		return 2;
		
	}
	public void updateNeighborsRouter(Message msg){
		int msgSize = msg.getSize();
		HashMap<DTNHost,HashMap<DTNHost, double[]>> totalNeighborsList = this.neighborsList;//所有其它有连接的节点

		List<DTNHost> neighbors = this.getHost().getNeighbors().getNeighbors();	
		
		for (DTNHost host : neighbors){//先找一跳的节点
			List<Integer> path = new ArrayList<Integer>();
			path.add(host.getAddress());
			double liveTime = this.getHost().getNeighbors().getNeighborsLiveTime().get(host)[1] - SimClock.getTime();
			//double liveTime = calculateExistTime(100000, this.getHost(), path);
			addRouterTable(host, path, calculateNeighborsDelay(msgSize, host), liveTime, false);//添加新项至路由表,false代表不是预测到达的邻居节点
		}
		
		if (transmitFeasible(msg.getTo()) == 1)
			return;//判断是否能够从已有链路中传输出去
		if (transmitFeasible(msg.getTo()) == 0)//说明邻居链路马上就要走，此时应该重新找一条新的到达此节点的路径
			this.routerTable.remove(msg.getTo());//把直接到达此节点的一跳路径删掉，让后续算法继续找新的路径
		
		Collection<DTNHost> restHosts = totalNeighborsList.keySet();//除了自己以外其它所有的节点
		List<Integer> minPath = new ArrayList<Integer>();
		DTNHost minFromHost = null, minHost = null;
		double transmitTime, minLiveTime = -2, minTransmitTime = 100000000;
		int size = restHosts.size();
		
		//System.out.println(restHosts);
		
		int iteratorTimes = 0;
		boolean hasNeighborsLabel = true;
		boolean updateLabel = true;
		
		while(true){//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
			if (iteratorTimes >= size || hasNeighborsLabel == false || updateLabel == false || restHosts.isEmpty())
				break;
			hasNeighborsLabel = false;//用以指示某一节点是末端节点(即只有一条链路)
			updateLabel = false;
			restHosts = this.routerTable.keySet();
			for (DTNHost host : restHosts){
				if (!host.getConnections().isEmpty()){//如果某个节点被孤立了，即没有邻居节点则无需进行搜索				
					if (this.routerTable.containsKey(host)){//找已经是邻居节点的节点，相当于从节点邻居的邻居不断扩张出去
						Collection<DTNHost> itsNeighbors = totalNeighborsList.get(host).keySet();
						if (!itsNeighbors.isEmpty()){
							for (DTNHost nei : itsNeighbors){//host节点的所有邻居节点
								if (!this.routerTable.containsKey(nei) && nei.getAddress() != this.getHost().getAddress()){//避免其往已有路径找
									hasNeighborsLabel = true;
									transmitTime = calculateDelay(msgSize, nei , host);//计算两节点之间的传输延时，顺序不能反，host为已在路由表内的最短路径节点，nei是host的邻居
									
									List<Integer> path = new ArrayList<Integer>();
									path.addAll(this.routerTable.get(host));
									path.add(nei.getAddress());//注意顺序
									
									double existTime = totalNeighborsList.get(host).get(nei)[1] - SimClock.getTime();
									existTime = (liveTime[host.getAddress()] > existTime) ? existTime : liveTime[host.getAddress()];
									
									//double existTime = calculateExistTime(liveTime[host.getAddress()], host, path);
									//System.out.println(getDistance(nei, host)+" "+nei.getLocation()+"  "+host.getLocation());//测试用
									if (transmitTime < minTransmitTime && existTime >= transmitTime){//待检查，关于链路存在时间和传输时间之间的判断！！！！！！！！！！！！！
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
				//System.out.print(minFromHost+"  "+minHost+"  ");//测试用
				//System.out.println(minPath);//测试用
			if (minHost == null){
				assert false : "minHost is empty!!! in "+this.getHost();
				break;
			}else{
				addRouterTable(minHost, minPath, minTransmitTime, minLiveTime, false);//添加新项至路由表,false代表不是预测到达的邻居节点
				if (transmitFeasible(msg.getTo()) == 1)//判断是否能够从已有链路中传输出去
					return;//如果中途已经找到了路径，就直接返回不用继续寻找
				//if (transmitFeasible(msg.getTo()) == 0)//判断是否能够从已有链路中传输出去
				//	return;//如果中途已经找到了路径，就直接返回不用继续寻找
				minTransmitTime = 100000000;
				minFromHost = null;
				minHost = null;
			}	
			iteratorTimes++;
		}
		/*//如果还有节点没找到路径，一定是处于孤立状态,进行预测
		if (!restHosts.isEmpty()){
			System.out.println("can't find path  "+ restHosts);
		}*/	
	}
	public List<DTNHost> getHostListFromPath(List<Integer> path){
		List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
		for (int i = 0; i < path.size(); i++){
			hostsOfPath.add(this.getHostFromAddress(path.get(i)));//根据节点地址找到DTNHost 
		}
		return hostsOfPath;
	}
	/**
	 * 在算路由表时，预测指定路径上的链路存在时间
	 * @param host 指最初的一跳
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
										this.getHost().getInterface(1).getTransmitSpeed()) + this.transmitRange*1000/SPEEDOFLIGHT;//取二者较小的传输速率;
			return waitTime;
		}
		else{
			assert false :"预测结果失效 ";
			return -1;
		}
	}
	public double calculateDelay(int msgSize, DTNHost nei , DTNHost host){
		double transmitDelay = msgSize/((nei.getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : nei.getInterface(1).getTransmitSpeed()) + 
				this.transmitDelay[host.getAddress()] + getDistance(nei, host)*1000/SPEEDOFLIGHT;//取二者较小的传输速率
		return transmitDelay;
	}
	public double calculateNeighborsDelay(int msgSize, DTNHost host){//计算一跳邻居的传输延时
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
	public Connection findConnection(int address){//根据节点地址找到，与此节点相连的连接
		List<Connection> connections = this.getHost().getConnections();
		for (Connection c : connections){
			if (c.getOtherNode(this.getHost()).getAddress() == address){
				return c;
			}
		}
		return null;//没有在已有连接中找到通过指定节点的路径
	}

	
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
	/**
	 * 用于判断下一跳节点是否处于发送或接受状态
	 * @param t
	 * @return
	 */
	public boolean hostIsBusyOrNot(Tuple<Message, Connection> t){
		List<Connection> connections = t.getValue().getOtherNode(this.getHost()).getConnections();
		for (Connection con : connections){
			if (con.isTransferring()){
				busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
				System.out.println(this.getHost()+"  "+t.getValue().getOtherNode(this.getHost())+" "+con+"  "+busyLabel.get(t.getKey().getId()));			
				return true;//说明目的节点正忙
			}
		}
		return false;
	}
	/**
	 * 从给定消息和指定链路，尝试发送消息
	 * @param t
	 * @return
	 */
	public boolean sendMsg(Tuple<Message, Connection> t){
		if (t == null){	
			//assert message.getProperty(MSG_WAITLABEL) == null : "wait!";//如果确实是需要等待未来的一个节点就等，先传下一个,待修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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

	
	/*public boolean createNewMessage(Message msg){
		List<Integer> routerPath = new ArrayList<Integer>();	

	    makeRoomForNewMessage(msg.getSize());
	    
	    msg.setTtl(this.msgTtl);
	    msg.addProperty(MSG_ROUTERPATH, routerPath); //就添加这一行
	    addToMessages(msg, true);
	    return true;
	}*/
}
