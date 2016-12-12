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
			updateAllHostsInterface();//����,ͨ������ѭ��������ȫ������Ľڵ������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			break;
		}
		
		if (this.updateOrder == null) { // randomizing is off
			for (int i=0, n = hosts.size();i < n; i++) {
				if (this.isCancelled) {
					break;
				}			
				//this.neighbor.CalculateNeighbor(this.hosts.get(i), SimClock.getTime());//��������������ÿ�����ǵ��ھӽڵ��б�
							
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
				//this.neighbor.CalculateNeighbor(this.updateOrder.get(i), SimClock.getTime());//��������������ÿ�����ǵ��ھӽڵ��б�
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
