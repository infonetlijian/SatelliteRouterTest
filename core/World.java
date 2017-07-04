/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;
import interfaces.ConnectivityOptimizer;
import interfaces.ContactGraphInterface;
import interfaces.EnableInterruptionsInterface;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import routing.CGR;
import routing.CGRbasedonEASR;
import util.Tuple;

/**
 * World contains all the nodes and is responsible for updating their
 * location and connections.
 */
public class World {
	/** name space of optimization settings ({@value})*/
	public static final String OPTIMIZATION_SETTINGS_NS = "Optimization";

	/**
	 * Should the order of node updates be different (random) within every 
	 * update step -setting id ({@value}). Boolean (true/false) variable. 
	 * Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
	 */
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
	/** should the update order of nodes be randomized -setting's default value
	 * ({@value}) */
	public static final boolean DEF_RANDOMIZE_UPDATES = true;
	
	/**
	 * Should the connectivity simulation be stopped after one round 
	 * -setting id ({@value}). Boolean (true/false) variable. 
	 */
	public static final String SIMULATE_CON_ONCE_S = "simulateConnectionsOnce";

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;//Ŀǰ������ͨ����ʼ���趨���¼��������ͺͲ�������ǰ���ɺ����г�ʼ���¼�����Ҫ��message�Ĳ����¼���
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	/** list of nodes; nodes are indexed by their network address */
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	/** nodes in the order they should be updated (if the order should be 
	 * randomized; null value means that the order should not be randomized) */
	private ArrayList<DTNHost> updateOrder;
	/** is cancellation of simulation requested from UI */
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	/** Queue of scheduled update requests */
	private ScheduledUpdatesQueue scheduledUpdates;
	private boolean simulateConOnce;
	private boolean isConSimulated;
	
	/*�޸Ĳ�������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	boolean initInterruptionLabel = false;
	List<Tuple<DTNHost, Double>> InterruptionList;
	private Neighbors neighbor;//����
	private HashMap<DTNHost, DTNHost> connectedHosts = new HashMap<DTNHost, DTNHost>();
	/** router mode in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** router mode in the sim -setting id ({@value})*/
	public static final String ROUTERMODENAME_S = "routerMode";
	public static final String DIJSKTRA_S = "dijsktra";
	public static final String SIMPLECONNECTIVITY_S = "simpleConnectivity";//����
	/*�޸Ĳ�������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	/**
	 * Constructor.
	 */
	public World(List<DTNHost> hosts, int sizeX, int sizeY, 
			double updateInterval, List<UpdateListener> updateListeners,
			boolean simulateConnections, List<EventQueue> eventQueues) {
		this.hosts = hosts;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.updateInterval = updateInterval;
		this.updateListeners = updateListeners;
		this.simulateConnections = simulateConnections;
		this.eventQueues = eventQueues;
		
		this.simClock = SimClock.getInstance();
		this.scheduledUpdates = new ScheduledUpdatesQueue();
		this.isCancelled = false;
		this.isConSimulated = false;

		//this.neighbor=new Neighbors(hosts);//����������ʼ�����ɵ�DTNHost�б���Neighbors����
		
		setNextEventQueue();
		initSettings();
		
		/**����CGR��ʼ������**/
		HashMap<String, Integer> router = new HashMap<String, Integer>();
		router.put("CGR", 1);
		router.put("CGRbasedonEASR", 2);
		Settings s = new Settings("Group");
		if (router.get(s.getSetting("router")) != null){
			System.out.println(s.getSetting("router"));
			constractContactGraph();
			if (router.get(s.getSetting("router")) == 1){
				/**����contactgraph**/
				for(DTNHost h : this.hosts){
					((CGR)h.getRouter()).setContactGraph(contactGraph);
				}
				System.out.println("node size: "+contactGraph.size());	
				System.out.println("time size:  "+contactGraph.get(this.hosts.get(0)).size());
			}
			else{
				/**����contactgraph**/
				for(DTNHost h : this.hosts){
					((CGRbasedonEASR)h.getRouter()).setContactGraph(contactGraph);
				}
				System.out.println("node size: "+contactGraph.size());	
				System.out.println("time size:  "+contactGraph.get(this.hosts.get(0)).size());
			}
		}			
	}

	/**
	 * Initializes settings fields that can be configured using Settings class
	 */
	private void initSettings() {
		Settings s = new Settings(OPTIMIZATION_SETTINGS_NS);
		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;

		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}
		simulateConOnce = s.getBoolean(SIMULATE_CON_ONCE_S, false);
		
		if(randomizeUpdates) {
			// creates the update order array that can be shuffled
			this.updateOrder = new ArrayList<DTNHost>(this.hosts);
		}
		else { // null pointer means "don't randomize"
			this.updateOrder = null;
		}
	}

	/**
	 * Moves hosts in the world for the time given time initialize host 
	 * positions properly. SimClock must be set to <CODE>-time</CODE> before
	 * calling this method.
	 * @param time The total time (seconds) to move
	 */
	public void warmupMovementModel(double time) {
		if (time <= 0) {
			return;
		}

		while(SimClock.getTime() < -updateInterval) {
			moveHosts(updateInterval);
			simClock.advance(updateInterval);
		}

		double finalStep = -SimClock.getTime();

		moveHosts(finalStep);
		simClock.setTime(0);	
	}

	/**
	 * Goes through all event Queues and sets the 
	 * event queue that has the next event.
	 */
	public void setNextEventQueue() {
		//EventQueue��һ���ӿ�,ͨ������ʵ��������ӿڵ���������ʵ����
		EventQueue nextQueue = scheduledUpdates;//����ScheduledUpdatesQueue scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {//ÿ������eventQueues���ҵ�����������Ҫ������¼�
			if (eq.nextEventsTime() < earliest){
				nextQueue = eq;	
				earliest = eq.nextEventsTime();//�ҵ��������¼����д����¼�������¼����ŵ���һ�����д���nextEventQueue
			}
		}
		
		this.nextEventQueue = nextQueue;//����EventQueue nextEventQueue;
		this.nextQueueEventTime = earliest;
	}

	/** 
	 * Update (move, connect, disconnect etc.) all hosts in the world.
	 * Runs all external events that are due between the time when
	 * this method is called and after one update interval.
	 */
	public void update () {
		double runUntil = SimClock.getTime() + this.updateInterval;
		
		setNextEventQueue();//������һ����Ϣ�¼�������Ϣʵ���ϻ�û�в�������ͬʱ��������Ҫ������¼������ó���

//		/**��������·�жϵĴ���**/
//		if (this.InterruptionList == null && this.initInterruptionLabel == false){
//			this.InterruptionList = initInterruption();//�洢Ԥ����Ƶ��жϷ���
//		}			
//		if (!this.InterruptionList.isEmpty()){
//			if (simClock.getTime() >= this.InterruptionList.get(0).getValue()){
//				DTNHost selectedHost = this.InterruptionList.get(0).getKey();
//				interruptHostsConnection(selectedHost);
//				this.InterruptionList.remove(0);
//				//throw new SimError("Interrupt!!!");
//			}	
//		}
		randomlyInterrupt();
		/**��������·�жϵĴ���**/
		
		//this.neighbor=new Neighbors(this.hosts);//��������
		
		//��ѭ��������һ�����������ڵ��¼�
		/* process all events that are due until next interval update */
		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();//nextEvent������һ����Ϣ�¼�����ע�⣬ֻ�д�ʱ�䱻����֮�������������һ����Ϣ
			ee.processEvent(this);
			updateHosts(); // update all hosts after every event
			setNextEventQueue();//ÿ������eventQueues���ҵ�����������Ҫ������¼���ֻҪ�ڸ��¼��ʱ���ڣ��ͽ��д���
		}

		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);

		updateHosts();

		/* inform all update listeners */
		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}
	}


	public double calculateDistance(Coord c1,Coord c2){
		double distance = c1.distance(c2);
		return distance;
	}
	private HashMap<Tuple<DTNHost, DTNHost>, Tuple<Double, Double>> linkExistList =
				new HashMap<Tuple<DTNHost, DTNHost>, Tuple<Double, Double>>();
	
	public void updateAllHostsInterface(){
		double timeNow = SimClock.getTime();
		
		removeAllConnections();//����Ҫ�Ƴ��������ӣ�����
		this.connectedHosts.clear();//ÿ������ռ�¼�ѽ������ӵĽڵ�
		
		for (int i=0, n = hosts.size();i < n; i++) {
			if (this.isCancelled) {
				break;
			}	
			hosts.get(i).getInterface(1).update();//��DTNHost���INTERFACE�����ó���������ֻ����λ�ã���������ͳһ����
		}
		
		for (int i=0, n = hosts.size();i < n; i++) {//������ѭ����㽨����ȫ������Ľڵ������
			Tuple<DTNHost, DTNHost> t = getClosedHosts();
			if (t != null){
				DTNHost from = t.getKey();
				DTNHost to = t.getValue();
				from.getInterface(1).connect(to.getInterface(1));//����
				this.connectedHosts.put(from, to);//��¼�Ѿ��������ӵĽڵ�
				/*Tuple<DTNHost, DTNHost> link = new Tuple<DTNHost, DTNHost>(from, to);
				if (linkExistList.containsKey(link)){
					if (linkExistList.get(link).getValue() < timeNow){//��Ҫ���
						Tuple<Double, Double> existTime = new Tuple<Double, Double>(linkExistList.get(link).getKey(), timeNow);//������·����ʱ��
						linkExistList.put(link, existTime);
					}
				}*/
			}
			else
				assert false : "error!";
		}
	}
	public Tuple<DTNHost, DTNHost> getClosedHosts(){
		
		double minDistance = 1000000;
		Tuple<DTNHost, DTNHost> closedHosts = new Tuple<DTNHost, DTNHost>(hosts.get(0), hosts.get(1));//��ʼ��
		boolean changeLabel = false;
		for (int i=0, n = hosts.size();i < n - 1; i++) {
			DTNHost from = hosts.get(i);
			
			if (from.getConnections().size() > 0){//ÿ�����ǽڵ�ֻ��һ������ͷ�����ͬʱ���ֻ����һ������
				continue;
			}
			for (int j = 0; j < n; j++) {//ע����ѭ���߽磡��������������������������������������������������������������������������
				if (i == j)
					continue;//�����ֱ������
				DTNHost to = hosts.get(j);
				double distance = calculateDistance(from.getLocation(), to.getLocation());
				System.out.println(from+"  "+to+"  "+ minDistance+"  "+distance);
				if (minDistance > distance){
					if (this.connectedHosts.containsKey(from)){//ÿ�����ǽڵ�ֻ��һ������ͷ�����ͬʱ���ֻ����һ������
						continue;
					}					
					closedHosts = new Tuple<DTNHost, DTNHost>(from, to);
					minDistance = distance;
					changeLabel = true;
				}				
			}
		}
		if (changeLabel == true)
			return closedHosts;
		else 
			return null;
	}	
	/*��������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	/*��������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

	private HashMap<DTNHost, List<Tuple<DTNHost, DTNHost>>> contactGraph = new HashMap<DTNHost, List<Tuple<DTNHost, DTNHost>>>();
	private Random random = new Random();
	
	/**
	 * ������ķ�ʽ��Ԥ�ȹ���CGR��������������е���·�������
	 */
	public void constractContactGraph(){
		Settings s = new Settings("Scenario");
		double endTime = s.getDouble("endTime");
		Settings CGRsettings = new Settings("Group");
		
		int duration = CGRsettings.getInt("router.CGR.LinkDurationTimesOfUpdateInterval");

		for (double time = 0; time <= endTime + 100; time += duration * this.updateInterval){
			for (DTNHost from : this.hosts){
				/**�ж�����ڵ���timeʱ�̵�contact plan������·�Ƿ��Ѿ���֮ǰ�Ѿ��������ˣ��������˾�����**/
//				if (contactGraph.get(from) != null)
//					if (contactGraph.get(from).get(time) != null)
//						continue;
				
				List<DTNHost> neighborHosts = getNeighbors(from, time);
				DTNHost to = neighborHosts.get(random.nextInt(neighborHosts.size()));

				Tuple<DTNHost, DTNHost> connection = new Tuple<DTNHost, DTNHost>(from, to);
				addContactGraph(connection, time, duration);			
			}
		}
		//System.out.println(this.contactGraph);
	}
	public List<DTNHost> getNeighbors(DTNHost host, double time){
		List<DTNHost> neiHost = new ArrayList<DTNHost>();//�ھ��б�
		HashMap<DTNHost, Coord> loc = new HashMap<DTNHost, Coord>();
		loc.clear();
		
		/**ʵʱ����ȫ���ڵ�����깹������ͼ**/
		for (DTNHost h : hosts){//����ָ��ʱ��ȫ�ֽڵ������
			//location.my_Test(time, 0, h.getParameters());
			//Coord xyz = new Coord(location.getX(), location.getY(), location.getZ());
			/**ֱ�ӻ�ȡ��ǰ�Ľڵ�λ�ã��򻯼�����̣���߷��������ٶ�**/
			Coord xyz = h.getCoordinate(time);
			//Coord xyz = h.getLocation();
			/**ֱ�ӻ�ȡ��ǰ�Ľڵ�λ�ã��򻯼������**/
			loc.put(h, xyz);//��¼ָ��ʱ��ȫ�ֽڵ������
		}
		
		Coord myLocation = loc.get(host);
		for (DTNHost h : hosts){//�ٷֱ𼰼���
			if (h == host)
				continue;
			if (JudgeNeighbors(myLocation, loc.get(h)) == true){
				//System.out.println(host+"  locate  "+myLocation+"  "+loc.get(host));
				neiHost.add(h);
			}
		}
		//System.out.println(host+" neighbor: "+neiHost+" time: "+time);
		return neiHost;
	}
	/**
	 * ��Coord��������о������
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean JudgeNeighbors(Coord c1,Coord c2){
		Settings s = new Settings("Interface");
		double transmitRange = s.getDouble("transmitRange");//�������ļ��ж�ȡ����뾶
		
		double distance = c1.distance(c2);
		if (distance <= transmitRange)
			return true;
		else
			return false;
	}	
	public void addContactGraph(Tuple<DTNHost, DTNHost> connection, double time, int duration){
		//this.updateInterval;
		DTNHost from = connection.getKey();
		DTNHost to = connection.getValue();
		
//		/**��double���͵�ֵ�����������룬�������1.00000001�������**/
//		BigDecimal b = new BigDecimal(time);  
//		time = b.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue(); 
		/**��ʼ����**/
		if (this.contactGraph.get(from) == null){
			List<Tuple<DTNHost, DTNHost>> contactPlan = new ArrayList<Tuple<DTNHost, DTNHost>>();
			for (int i = 0 ; i < duration ; i++ ){
				contactPlan.add(connection);
			}		
			this.contactGraph.put(from, contactPlan);		
		}
		else{			
			List<Tuple<DTNHost, DTNHost>> contactPlan1 = this.contactGraph.get(from);
			if (contactPlan1.size() >= (int)(time*10))
				return;
			for (int i = 0 ; i < duration ; i++ ){
				contactPlan1.add(connection);
			}				
			this.contactGraph.put(from, contactPlan1);
		}
		if (this.contactGraph.get(to) == null){
			List<Tuple<DTNHost, DTNHost>> contactPlan = new ArrayList<Tuple<DTNHost, DTNHost>>();
			for (int i = 0 ; i < duration ; i++ ){
				contactPlan.add(connection);
			}	
			this.contactGraph.put(to, contactPlan);
		}
		else{
			List<Tuple<DTNHost, DTNHost>> contactPlan2 = this.contactGraph.get(to);
			if (contactPlan2.size() >= (int)(time*10))
				return;
			for (int i = 0 ; i < duration ; i++ ){
				contactPlan2.add(connection);
			}					
			this.contactGraph.put(to, contactPlan2);
		}	
		System.out.println("connection: "+connection +"  "+ time);
	}
	
	/*��������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

	/**
	 * Updates all hosts (calls update for every one of them). If update
	 * order randomizing is on (updateOrder array is defined), the calls
	 * are made in random order.
	 */
	private void updateHosts() {

		//test();//�����Ż���
		//assert false: "test";
		
		Settings s = new Settings(USERSETTINGNAME_S);
		int mode = s.getInt(ROUTERMODENAME_S);//�������ļ��ж�ȡ·��ģʽ
		switch(mode){
		case 1:	//ȫ������·���㷨,dijsktra
			break;
		case 2 ://simpleConnectivity;
			//updateAllHostsInterface();//����,ͨ������ѭ��������ȫ������Ľڵ������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			break;
		}
		
		if (this.updateOrder == null) { // randomizing is off
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}										
				hosts.get(i).update(simulateConnections);
			}
		}
		else { // update order randomizing is on
			assert this.updateOrder.size() == this.hosts.size() : 
				"Nrof hosts has changed unexpectedly";
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng); 
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}			
				this.updateOrder.get(i).update(simulateConnections);
			}			
		}
		
		if (simulateConOnce && simulateConnections) {
			simulateConnections = false;
		}
	}
	
	public List<Tuple<DTNHost, Double>> initInterruption(){
		/**��������·�жϵĴ���**/

		Settings scenario = new Settings("Scenario");
		double endTime = scenario.getDouble("endTime");
		double updateInterval = scenario.getDouble("updateInterval");
		Settings setting = new Settings("Interface");
		double probabilityOfInterrupt = setting.getDouble("probabilityOfInterrupt");//��ȡ�û����õ���·�жϸ���
		
		List<Tuple<DTNHost, Double>> InterruptionList = new ArrayList<Tuple<DTNHost, Double>>();//�洢Ԥ����Ƶ��жϷ���
		
		for (double simTime = 0; simTime <= endTime; simTime += updateInterval){
			if (probabilisticInterrupt(probabilityOfInterrupt)){
				Random random = new Random();
				
				int selectedNum = random.nextInt(hosts.size());
				selectedNum = Math.abs(selectedNum == 0 ? 0 : selectedNum - 1);
				DTNHost selectedHost = hosts.get(selectedNum);			
				Tuple<DTNHost, Double> Interruption = new Tuple<DTNHost, Double>(selectedHost, simTime);
				InterruptionList.add(Interruption);
			}
		}
		this.initInterruptionLabel = true;
		return InterruptionList;
	}
	/**
	 * ���������·�ж����
	 */
	public void randomlyInterrupt(){
		/**��������·�жϵĴ���**/
		Settings setting = new Settings("Interface");
		double probabilityOfInterrupt = setting.getDouble("probabilityOfInterrupt");//��ȡ�û����õ���·�жϸ���
		
		List<Tuple<Connection, DTNHost>> transferringConnectionList = new ArrayList<Tuple<Connection, DTNHost>>();
		/**�ҵ�����ʱ�̵�ȫ����·Connection**/
		HashMap<Connection, DTNHost> allConnectionList = new HashMap<Connection, DTNHost>();
		for (DTNHost h : this.hosts){
			for (Connection con : h.getConnections()){			
				if (!allConnectionList.containsKey(con)){
					allConnectionList.put(con, h);
				}
			}		
		}
		/**ȫ����·�������ж�**/
		for (Connection con : allConnectionList.keySet()){
			if (probabilisticInterrupt(probabilityOfInterrupt)){
				/**�����ڵĴ�����·��ֱ����ֹ����**/
				if (con.isTransferring())
					con.abortTransfer();
				DTNHost selectedHost = allConnectionList.get(con);
				DTNHost anotherHost = con.getOtherNode(selectedHost);
				selectedHost.connectionDown(con);
				anotherHost.connectionDown(con);
				String interfaceType = setting.getSetting("type");
				if (interfaceType.endsWith("ContactGraphInterface")){
					/**������Ҫ�жϵĽڵ��б�**/
					((ContactGraphInterface)selectedHost.getInterface(1)).setInterruptHost(anotherHost);
					((ContactGraphInterface)anotherHost.getInterface(1)).setInterruptHost(selectedHost);
				}
				if (interfaceType.endsWith("EnableInterruptionsInterface")){
					/**������Ҫ�жϵĽڵ��б�**/
					((EnableInterruptionsInterface)selectedHost.getInterface(1)).setInterruptHost(anotherHost);
					((EnableInterruptionsInterface)anotherHost.getInterface(1)).setInterruptHost(selectedHost);
				}
			}
		}
//		if (probabilisticInterrupt(probabilityOfInterrupt)){
//			Random random = new Random();
//			int chosenHost = random.nextInt(hosts.size());
//			chosenHost = Math.abs(chosenHost == 0 ? 0 : chosenHost - 1);
//			if (hosts.get(chosenHost).getConnections().size() <= 0)
//				return;
//			int chosenConnection = random.nextInt(hosts.get(chosenHost).getConnections().size());
//			chosenConnection = Math.abs(chosenConnection == 0 ? 0 : chosenConnection - 1);
//			Connection chosenOne = hosts.get(chosenHost).getConnections().get(chosenConnection);
//			NetworkInterface chosenInterface = hosts.get(chosenHost).getInterface(1); 
//			NetworkInterface anotherInterface = chosenOne.getOtherInterface(chosenInterface);
//			//System.out.println("Interrupt! + " + hosts.get(chosenHost) + "   " + chosenOne);
//			chosenInterface.disconnect(chosenOne, anotherInterface);//�Ͽ�����
//			hosts.get(chosenHost).getConnections().remove(chosenConnection);			
//		}
		/**��������·�жϵĴ���**/
	}
	public boolean interruptHostsConnection(DTNHost selectedHost){
		
		List<Tuple<Connection, DTNHost>> transferringConnectionList = new ArrayList<Tuple<Connection, DTNHost>>();
		for (DTNHost h : this.hosts){
			for (Connection con : h.getConnections()){
				if (con.isTransferring()){
					transferringConnectionList.add(new Tuple(con, h));
				}
			}		
		}
		if (!transferringConnectionList.isEmpty()){
			int selectedNum = random.nextInt(transferringConnectionList.size());
			selectedNum = Math.abs(selectedNum == 0 ? 0 : selectedNum - 1);
			Tuple<Connection, DTNHost> t = transferringConnectionList.get(selectedNum);
			NetworkInterface selectedInterface = t.getValue().getInterface(1);
			Connection selectedConnection = t.getKey();
			NetworkInterface anotherInterface = selectedConnection.getOtherInterface(selectedInterface);
		
			//selectedInterface.disconnect(selectedConnection, anotherInterface);//�Ͽ�����
			selectedConnection.abortTransfer();
			selectedHost.getInterface(1).destroyConnection(anotherInterface);
			//selectedInterface.connect(anotherInterface);//���������µ�connection
			//System.out.println("Interrupt! + " + selectedHost + "   " + selectedConnection.getMessage());		
			return true;
		}

		if (selectedHost.getConnections().size() <= 0)
			return false;
		int selectedNum = random.nextInt(selectedHost.getConnections().size());
		selectedNum = Math.abs(selectedNum == 0 ? 0 : selectedNum - 1);
		Connection selectedConnection = selectedHost.getConnections().get(selectedNum);
		NetworkInterface selectedInterface = selectedHost.getInterface(1); 
		NetworkInterface anotherInterface = selectedConnection.getOtherInterface(selectedInterface);
		//System.out.println("Interrupt! + " + selectedHost + "   ");	
		//System.out.println(selectedHost + "  " + selectedConnection + "  contains? " + selectedHost.getConnections().contains(selectedConnection) + "  " + anotherInterface.getConnections().contains(selectedConnection));	
		//selectedInterface.disconnect(selectedConnection, anotherInterface);//�Ͽ�����
		selectedHost.getInterface(1).destroyConnection(anotherInterface);
		//anotherInterface.getConnections().remove(selectedConnection);
		//selectedHost.getInterface(1).getConnections().remove(selectedConnection);
		//System.out.println(selectedHost + "  " + selectedConnection + "  contains? " + selectedHost.getConnections().contains(selectedConnection) + " "  + anotherInterface.getConnections().contains(selectedConnection));	
		//selectedInterface.connect(anotherInterface);//���������µ�connection
//		System.out.println(selectedHost + "  " + selectedConnection + " isUp? "+ selectedConnection.isUp() + " contains?  " + selectedHost.getConnections().contains(selectedConnection)+ " "  + anotherInterface.getConnections().contains(selectedConnection));
//		DTNHost anotherHost = anotherInterface.getHost();
//		for (int i=0; i < selectedHost.getConnections().size(); i++) {
//			if (selectedHost.getConnections().get(i).getOtherNode(selectedHost) == anotherHost){
//				selectedConnection = selectedHost.getConnections().get(i);
//			}
//		}
//		System.out.println(selectedHost + "  " + selectedConnection + " isUp? "+ selectedConnection.isUp() + " contains?  " + selectedHost.getConnections().contains(selectedConnection)+ " "  + anotherInterface.getConnections().contains(selectedConnection));	
		//anotherInterface.getConnections().remove(selectedConnection);
		//selectedHost.getConnections().remove(selectedConnection);	
		return true;
	}
	/**
	 * ��������жϺ���������ĸ���ֵ����0��1֮��
	 * @param probabilityOfInterrupt
	 * @return
	 */
	public boolean probabilisticInterrupt(double probabilityOfInterrupt){		
		double roll = random.nextDouble();//�������0��1֮�����
		
		if (roll < probabilityOfInterrupt)
			return true;			
		else
			return false;
	}
	
	/**
	 * Moves all hosts in the world for a given amount of time
	 * @param timeIncrement The time how long all nodes should move
	 */
	private void moveHosts(double timeIncrement) {
		for (int i=0,n = hosts.size(); i<n; i++) {
			DTNHost host = hosts.get(i);
			host.move(timeIncrement);			
		}		
	}

	/**
	 * Asynchronously cancels the currently running simulation
	 */
	public void cancelSim() {
		this.isCancelled = true;
	}

	/**
	 * Returns the hosts in a list
	 * @return the hosts in a list
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	/**
	 * Returns the x-size (width) of the world 
	 * @return the x-size (width) of the world 
	 */
	public int getSizeX() {
		return this.sizeX;
	}

	/**
	 * Returns the y-size (height) of the world 
	 * @return the y-size (height) of the world 
	 */
	public int getSizeY() {
		return this.sizeY;
	}

	/**
	 * Returns a node from the world by its address
	 * @param address The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address " +
					"range of 0-" + (hosts.size()-1) + " is valid");
		}

		DTNHost node = this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. " + 
			"Node " + node + " in index " + address;

		return node; 
	}

	/**
	 * Schedules an update request to all nodes to happen at the specified 
	 * simulation time.
	 * @param simTime The time of the update
	 */
	public void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}
}
