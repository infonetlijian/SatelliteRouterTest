/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;
import interfaces.ConnectivityOptimizer;
import interfaces.SimpleSatelliteInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
	private List<EventQueue> eventQueues;//目前假设是通过初始化设定的事件发生类型和参数，提前生成好所有初始化事件（主要是message的产生事件）
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
	
	/*修改参数部分!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	private Neighbors neighbor;//新增
	private HashMap<DTNHost, DTNHost> connectedHosts = new HashMap<DTNHost, DTNHost>();
	/** router mode in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** router mode in the sim -setting id ({@value})*/
	public static final String ROUTERMODENAME_S = "routerMode";
	public static final String DIJSKTRA_S = "dijsktra";
	public static final String SIMPLECONNECTIVITY_S = "simpleConnectivity";//新增
	/*修改参数部分!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
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

		//this.neighbor=new Neighbors(hosts);//新增，将初始化生成的DTNHost列表传入Neighbors当中
		
		setNextEventQueue();
		initSettings();
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
		//EventQueue是一个接口,通过赋予实现了这个接口的类来进行实例化
		EventQueue nextQueue = scheduledUpdates;//定义ScheduledUpdatesQueue scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {//每次历遍eventQueues，找到其中最早需要处理的事件
			if (eq.nextEventsTime() < earliest){
				nextQueue = eq;	
				earliest = eq.nextEventsTime();//找到接下来事件当中处理事件最早的事件，放到下一个进行处理nextEventQueue
			}
		}
		
		this.nextEventQueue = nextQueue;//定义EventQueue nextEventQueue;
		this.nextQueueEventTime = earliest;
	}

	/** 
	 * Update (move, connect, disconnect etc.) all hosts in the world.
	 * Runs all external events that are due between the time when
	 * this method is called and after one update interval.
	 */
	public void update () {
		double runUntil = SimClock.getTime() + this.updateInterval;
		
		setNextEventQueue();//产生下一个消息事件（但消息实际上还没有产生），同时把最早需要处理的事件队列拿出来

		//this.neighbor=new Neighbors(this.hosts);//新增函数
		
		//此循环处理在一个更新周期内的事件
		/* process all events that are due until next interval update */
		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();//nextEvent产生下一个消息事件，但注意，只有此时间被处理之后才真正产生了一个消息
			ee.processEvent(this);
			updateHosts(); // update all hosts after every event
			setNextEventQueue();//每次历遍eventQueues，找到其中最早需要处理的事件，只要在更新间隔时间内，就进行处理
		}

		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);

		updateHosts();

		/* inform all update listeners */
		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}
	}
	/*test!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	public void test(){//网格法优化用
		LinkedMap<Double, ConnectivityOptimizer> map = new LinkedMap<Double, ConnectivityOptimizer>();
		double endTime = 60000;
		List<ConnectivityOptimizer> optList = new ArrayList<ConnectivityOptimizer>();
		for (double time = 0; time < endTime; time += this.updateInterval){
			for (int i = 0; i < this.hosts.size(); i++){
				hosts.get(i).updateLocation(time);//循环遍历更新所有的节点位置
			}
			hosts.get(0).getInterface(1).predictionUpdate();
		}
		for (double time = 0; time < endTime; time += this.updateInterval){
			for (int i = 0; i < this.hosts.size(); i++){
				hosts.get(i).updateLocation(time);//循环遍历更新所有的节点位置
				for (NetworkInterface inter : hosts.get(i).getInterfaces()){
					ConnectivityOptimizer conOpt = inter.predictionUpdate();//更新各个节点的网格位置
					optList.add(conOpt);
					
					System.out.println(conOpt.getAllInterfaces());
				}
			}			
		}

	}
	/*新增函数!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	public double calculateDistance(Coord c1,Coord c2){
		double distance = c1.distance(c2);
		return distance;
	}
	private HashMap<Tuple<DTNHost, DTNHost>, Tuple<Double, Double>> linkExistList =
				new HashMap<Tuple<DTNHost, DTNHost>, Tuple<Double, Double>>();
	
	public void updateAllHostsInterface(){
		double timeNow = SimClock.getTime();
		
		removeAllConnections();//还需要移除所有连接，否则
		this.connectedHosts.clear();//每次先清空记录已建立连接的节点
		
		for (int i=0, n = hosts.size();i < n; i++) {
			if (this.isCancelled) {
				break;
			}	
			hosts.get(i).getInterface(1).update();//把DTNHost里的INTERFACE更新拿出来，而且只更新位置，建立连接统一建立
		}
		
		for (int i=0, n = hosts.size();i < n; i++) {//经过此循环后便建立了全局所需的节点间连接
			Tuple<DTNHost, DTNHost> t = getClosedHosts();
			if (t != null){
				DTNHost from = t.getKey();
				DTNHost to = t.getValue();
				from.getInterface(1).connect(to.getInterface(1));//连接
				this.connectedHosts.put(from, to);//记录已经建立连接的节点
				/*Tuple<DTNHost, DTNHost> link = new Tuple<DTNHost, DTNHost>(from, to);
				if (linkExistList.containsKey(link)){
					if (linkExistList.get(link).getValue() < timeNow){//需要检查
						Tuple<Double, Double> existTime = new Tuple<Double, Double>(linkExistList.get(link).getKey(), timeNow);//更新链路存在时间
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
		Tuple<DTNHost, DTNHost> closedHosts = new Tuple<DTNHost, DTNHost>(hosts.get(0), hosts.get(1));//初始化
		boolean changeLabel = false;
		for (int i=0, n = hosts.size();i < n - 1; i++) {
			DTNHost from = hosts.get(i);
			
			if (from.getConnections().size() > 0){//每个卫星节点只有一个激光头，因此同时最多只能有一个连接
				continue;
			}
			for (int j = 0; j < n; j++) {//注意检查循环边界！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
				if (i == j)
					continue;//相等则直接跳过
				DTNHost to = hosts.get(j);
				double distance = calculateDistance(from.getLocation(), to.getLocation());
				System.out.println(from+"  "+to+"  "+ minDistance+"  "+distance);
				if (minDistance > distance){
					if (this.connectedHosts.containsKey(from)){//每个卫星节点只有一个激光头，因此同时最多只能有一个连接
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
	/*新增函数!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

	/**
	 * Updates all hosts (calls update for every one of them). If update
	 * order randomizing is on (updateOrder array is defined), the calls
	 * are made in random order.
	 */
	private void updateHosts() {

		//test();//网格法优化用
		//assert false: "test";
		
		Settings s = new Settings(USERSETTINGNAME_S);
		int mode = s.getInt(ROUTERMODENAME_S);//从配置文件中读取路由模式
		switch(mode){
		case 1:	//全局最优路由算法,dijsktra
			break;
		case 2 ://simpleConnectivity;
			updateAllHostsInterface();//新增,通过三层循环，建立全局所需的节点间连接!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			break;
		}
		
		if (this.updateOrder == null) { // randomizing is off
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}			
				//this.neighbor.CalculateNeighbor(this.hosts.get(i), SimClock.getTime());//新增函数，更新每个卫星的邻居节点列表
							
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
				//this.neighbor.CalculateNeighbor(this.updateOrder.get(i), SimClock.getTime());//新增函数，更新每个卫星的邻居节点列表
				this.updateOrder.get(i).update(simulateConnections);
			}			
		}
		
		if (simulateConOnce && simulateConnections) {
			simulateConnections = false;
		}
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
