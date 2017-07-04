/* 
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.EventQueueHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import movement.MapBasedMovement;
import movement.MovementModel;
import movement.SatelliteMovement;
import movement.map.SimMap;
import routing.MessageRouter;
import routing.ShortestDistanceSpaceRouter.GridNeighbors.GridCell;
import util.Tuple;

/**
 * A simulation scenario used for getting and storing the settings of a
 * simulation run.
 */
public class SimScenario implements Serializable {
	
	/** a way to get a hold of this... */	
	private static SimScenario myinstance=null;

	/** namespace of scenario settings ({@value})*/
	public static final String SCENARIO_NS = "Scenario";
	/** number of host groups -setting id ({@value})*/
	public static final String NROF_GROUPS_S = "nrofHostGroups";
	/** number of interface types -setting id ({@value})*/
	public static final String NROF_INTTYPES_S = "nrofInterfaceTypes";
	/** scenario name -setting id ({@value})*/
	public static final String NAME_S = "name";
	/** end time -setting id ({@value})*/
	public static final String END_TIME_S = "endTime";
	/** update interval -setting id ({@value})*/
	public static final String UP_INT_S = "updateInterval";
	/** simulate connections -setting id ({@value})*/
	public static final String SIM_CON_S = "simulateConnections";

	/** namespace for interface type settings ({@value}) */
	public static final String INTTYPE_NS = "Interface";
	/** interface type -setting id ({@value}) */
	public static final String INTTYPE_S = "type";
	/** interface name -setting id ({@value}) */
	public static final String INTNAME_S = "name";

	/** namespace for application type settings ({@value}) */
	public static final String APPTYPE_NS = "Application";
	/** application type -setting id ({@value}) */
	public static final String APPTYPE_S = "type";
	/** setting name for the number of applications */
	public static final String APPCOUNT_S = "nrofApplications";
	
	/** namespace for host group settings ({@value})*/
	public static final String GROUP_NS = "Group";
	/** group id -setting id ({@value})*/
	public static final String GROUP_ID_S = "groupID";
	/** number of hosts in the group -setting id ({@value})*/
	public static final String NROF_HOSTS_S = "nrofHosts";
	/** movement model class -setting id ({@value})*/
	public static final String MOVEMENT_MODEL_S = "movementModel";
	/** router class -setting id ({@value})*/
	public static final String ROUTER_S = "router";
	/** number of interfaces in the group -setting id ({@value})*/
	public static final String NROF_INTERF_S = "nrofInterfaces";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "interface";
	/** application name in the group -setting id ({@value})*/
	public static final String GAPPNAME_S = "application";

	/** package where to look for movement models */
	private static final String MM_PACKAGE = "movement.";
	/** package where to look for router classes */
	private static final String ROUTING_PACKAGE = "routing.";

	/** package where to look for interface classes */
	private static final String INTTYPE_PACKAGE = "interfaces.";
	
	/** package where to look for application classes */
	private static final String APP_PACKAGE = "applications.";
	
	/** user setting in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** hosts mode in the sim -setting id ({@value})*/
	public static final String HOSTSMODENAME_S = "hostsMode";//������������
	/** user setting in the sim -setting id ({@value})*/
	public static final String CLUSTER_S = "cluster";
	/** user setting in the sim -setting id ({@value})*/
	public static final String NORMAL_S = "normal";
	/** user setting in the sim -setting id ({@value})*/
	public static final String NROFPLANE_S = "nrofPlane";
	
	/** The world instance */
	private World world;
	/** List of hosts in this simulation */
	protected List<DTNHost> hosts;
	/** Name of the simulation */
	private String name;
	/** number of host groups */
	int nrofGroups;
	/** Width of the world */

	
	/** Largest host's radio range */
	private double maxHostRange;
	/** Simulation end time */
	private double endTime;
	/** Update interval of sim time */
	private double updateInterval;
	/** External events queue */
	private EventQueueHandler eqHandler;
	/** Should connections between hosts be simulated */
	private boolean simulateConnections;
	/** Map used for host movement (if any) */
	private SimMap simMap;

	/** Global connection event listeners */
	private List<ConnectionListener> connectionListeners;
	/** Global message event listeners */
	private List<MessageListener> messageListeners;
	/** Global movement event listeners */
	private List<MovementListener> movementListeners;
	/** Global update event listeners */
	private List<UpdateListener> updateListeners;
	/** Global application event listeners */
	private List<ApplicationListener> appListeners;
	
	/*�޸ĺ�������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	private int worldSizeX;
	/** Height of the world */
	private int worldSizeY;
	private int worldSizeZ;//������������ά����
	/*�޸ĺ�������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	static {
		DTNSim.registerForReset(SimScenario.class.getCanonicalName());
		reset();
	}
	
	public static void reset() {
		myinstance = null;
	}

	/**
	 * Creates a scenario based on Settings object.
	 */

	
	/**
	 * Returns the SimScenario instance and creates one if it doesn't exist yet
	 */
	public static SimScenario getInstance() {
		if (myinstance == null) {
			myinstance = new SimScenario();
		}
		return myinstance;
	}



	/**
	 * Returns the name of the simulation run
	 * @return the name of the simulation run
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns true if connections should be simulated
	 * @return true if connections should be simulated (false if not)
	 */
	public boolean simulateConnections() {
		return this.simulateConnections;
	}

	/**
	 * Returns the width of the world
	 * @return the width of the world
	 */
	public int getWorldSizeX() {
		return this.worldSizeX;
	}

	/**
	 * Returns the height of the world
	 * @return the height of the world
	 */
	public int getWorldSizeY() {
		return worldSizeY;
	}

	/**
	 * Returns simulation's end time
	 * @return simulation's end time
	 */
	public double getEndTime() {
		return endTime;
	}

	/**
	 * Returns update interval (simulated seconds) of the simulation
	 * @return update interval (simulated seconds) of the simulation
	 */
	public double getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Returns how long range the hosts' radios have
	 * @return Range in meters
	 */
	public double getMaxHostRange() {
		return maxHostRange;
	}

	/**
	 * Returns the (external) event queue(s) of this scenario or null if there 
	 * aren't any
	 * @return External event queues in a list or null
	 */
	public List<EventQueue> getExternalEvents() {
		return this.eqHandler.getEventQueues();
	}

	/**
	 * Returns the SimMap this scenario uses, or null if scenario doesn't
	 * use any map
	 * @return SimMap or null if no map is used
	 */
	public SimMap getMap() {
		return this.simMap;
	}

	/**
	 * Adds a new connection listener for all nodes
	 * @param cl The listener
	 */
	public void addConnectionListener(ConnectionListener cl){
		this.connectionListeners.add(cl);
	}

	/**
	 * Adds a new message listener for all nodes
	 * @param ml The listener
	 */
	public void addMessageListener(MessageListener ml){
		this.messageListeners.add(ml);
	}

	/**
	 * Adds a new movement listener for all nodes
	 * @param ml The listener
	 */
	public void addMovementListener(MovementListener ml){
		this.movementListeners.add(ml);
	}

	/**
	 * Adds a new update listener for the world
	 * @param ul The listener
	 */
	public void addUpdateListener(UpdateListener ul) {
		this.updateListeners.add(ul);
	}

	/**
	 * Returns the list of registered update listeners
	 * @return the list of registered update listeners
	 */
	public List<UpdateListener> getUpdateListeners() {
		return this.updateListeners;
	}

	/** 
	 * Adds a new application event listener for all nodes.
	 * @param al The listener
	 */
	public void addApplicationListener(ApplicationListener al) {
		this.appListeners.add(al);
	}
	
	/**
	 * Returns the list of registered application event listeners
	 * @return the list of registered application event listeners
	 */
	public List<ApplicationListener> getApplicationListeners() {
		return this.appListeners;
	}
	/**
	 * Returns the list of nodes for this scenario.
	 * @return the list of nodes for this scenario.
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}
	
	/**
	 * Returns the World object of this scenario
	 * @return the World object
	 */
	public World getWorld() {
		return this.world;
	}
	
	/*�޸ĺ�������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
	protected SimScenario() {
		Settings s = new Settings(SCENARIO_NS);
		nrofGroups = s.getInt(NROF_GROUPS_S);

		this.name = s.valueFillString(s.getSetting(NAME_S));
		this.endTime = s.getDouble(END_TIME_S);
		this.updateInterval = s.getDouble(UP_INT_S);
		this.simulateConnections = s.getBoolean(SIM_CON_S);

		s.ensurePositiveValue(nrofGroups, NROF_GROUPS_S);
		s.ensurePositiveValue(endTime, END_TIME_S);
		s.ensurePositiveValue(updateInterval, UP_INT_S);

		this.simMap = null;
		this.maxHostRange = 1;

		this.connectionListeners = new ArrayList<ConnectionListener>();
		this.messageListeners = new ArrayList<MessageListener>();
		this.movementListeners = new ArrayList<MovementListener>();
		this.updateListeners = new ArrayList<UpdateListener>();
		this.appListeners = new ArrayList<ApplicationListener>();
		this.eqHandler = new EventQueueHandler();

		/* TODO: check size from movement models */
		s.setNameSpace(MovementModel.MOVEMENT_MODEL_NS);
		int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE, 2);//��2ά�޸�Ϊ3ά
		this.worldSizeX = worldSize[0];
		this.worldSizeY = worldSize[1];
		this.worldSizeZ = worldSize[1];//����������!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		createHosts();
		
		this.world = new World(hosts, worldSizeX, worldSizeY, updateInterval, 
				updateListeners, simulateConnections, 
				eqHandler.getEventQueues());
	}
	
	/**
	 * ����ָ���������ԵĽڵ㣬���ɵĽڵ������ɵ��ó������
	 * @param nrof_Hosts
	 * @param serialnumberofGroups
	 * @return
	 */
	protected List<DTNHost> createInitialHosts(int nrof_Hosts, int serialnumberofGroups, String typeofSatellites){
		List<DTNHost> hostsinthisCreation = new ArrayList<DTNHost>();
		
		int i = serialnumberofGroups;
	
		List<NetworkInterface> interfaces = 
				new ArrayList<NetworkInterface>();
			Settings s = new Settings(GROUP_NS+i);
			s.setSecondaryNamespace(GROUP_NS);
			String gid = s.getSetting(GROUP_ID_S);
			int nrofHosts = s.getInt(NROF_HOSTS_S);
			int nrofInterfaces = s.getInt(NROF_INTERF_S);
			int appCount;

			// creates prototypes of MessageRouter and MovementModel
			MovementModel mmProto = 
				(MovementModel)s.createIntializedObject(MM_PACKAGE + 
						s.getSetting(MOVEMENT_MODEL_S));
			MessageRouter mRouterProto = 
				(MessageRouter)s.createIntializedObject(ROUTING_PACKAGE + 
						s.getSetting(ROUTER_S));
			
			/* checks that these values are positive (throws Error if not) */
			s.ensurePositiveValue(nrofHosts, NROF_HOSTS_S);
			s.ensurePositiveValue(nrofInterfaces, NROF_INTERF_S);

			// setup interfaces
			for (int j=1;j<=nrofInterfaces;j++) {
				String intName = s.getSetting(INTERFACENAME_S + j);
				Settings intSettings = new Settings(intName); 
				NetworkInterface iface = 
					(NetworkInterface)intSettings.createIntializedObject(
							INTTYPE_PACKAGE +intSettings.getSetting(INTTYPE_S));
				iface.setClisteners(connectionListeners);
				iface.setGroupSettings(s);
				interfaces.add(iface);
			}

			// setup applications
			if (s.contains(APPCOUNT_S)) {
				appCount = s.getInt(APPCOUNT_S);
			} else {
				appCount = 0;
			}
			for (int j=1; j<=appCount; j++) {
				String appname = null;
				Application protoApp = null;
				try {
					// Get name of the application for this group
					appname = s.getSetting(GAPPNAME_S+j);
					// Get settings for the given application
					Settings t = new Settings(appname);
					// Load an instance of the application
					protoApp = (Application)t.createIntializedObject(
							APP_PACKAGE + t.getSetting(APPTYPE_S));
					// Set application listeners
					protoApp.setAppListeners(this.appListeners);
					// Set the proto application in proto router
					//mRouterProto.setApplication(protoApp);
					mRouterProto.addApplication(protoApp);
				} catch (SettingsError se) {
					// Failed to create an application for this group
					System.err.println("Failed to setup an application: " + se);
					System.err.println("Caught at " + se.getStackTrace()[0]);
					System.exit(-1);
				}
			}

			if (mmProto instanceof MapBasedMovement) {
				this.simMap = ((MapBasedMovement)mmProto).getMap();
			}
			
			/*�޸�*/	
			Settings setting = new Settings(USERSETTINGNAME_S);//��ȡ���ã��ж��Ƿ���Ҫ�ִ�
			String string = setting.getSetting(HOSTSMODENAME_S);
			
			Settings sat = new Settings(GROUP_NS);
			int TOTAL_SATELLITES = sat.getInt(NROF_HOSTS_S);//�ܽڵ���
			int TOTAL_PLANE = setting.getInt(NROFPLANE_S);//���ƽ����
			int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
			
			// creates hosts of ith group
			for (int j=0; j<nrof_Hosts; j++) {			
				ModuleCommunicationBus comBus = new ModuleCommunicationBus();

				// prototypes are given to new DTNHost which replicates
				// new instances of movement model and message router
				DTNHost host = new DTNHost(this.messageListeners, 
						this.movementListeners,	gid, interfaces, comBus, 
						mmProto, mRouterProto);

				int nrofPlane = j/NROF_S_EACHPLANE + 1;
				int nrofSatelliteINPlane = j - (nrofPlane - 1) * NROF_S_EACHPLANE;
				
				if (typeofSatellites == "LEO")
					host.setSatelliteParameters(TOTAL_SATELLITES, TOTAL_PLANE, nrofPlane,
						nrofSatelliteINPlane, initialSatelliteParameters(j, TOTAL_SATELLITES, TOTAL_PLANE));
				else{
					if (typeofSatellites == "MEO")				
						host.setSatelliteParameters(TOTAL_SATELLITES, TOTAL_PLANE, nrofPlane,
								nrofSatelliteINPlane, initialMEOParameters(j, TOTAL_SATELLITES, TOTAL_PLANE));
				}
				
				hostsinthisCreation.add(host);
				this.hosts.add(host);			
			}		
		return hostsinthisCreation;
	}
	
	/**
	 * Creates hosts for the scenario
	 */
	protected void createHosts() {
		this.hosts = new ArrayList<DTNHost>();
		/*�޸�*/
		/*��������*/
		Settings setting = new Settings(USERSETTINGNAME_S);//��ȡ���ã��ж��Ƿ���Ҫ�ִ�
		String string = setting.getSetting(HOSTSMODENAME_S);
		int nrofMEO = setting.getInt("nrofMEO");
		
		/**ͨ��map���switch case��֧��string���͵����(JAVA 1.7���²�֧��)**/
		Map<String,Integer> mode=new HashMap<String, Integer>();
		mode.put("normal", 1);//ȫ��·�ɣ����ִأ�
		mode.put("cluster", 2);//�ִ�·��
		/**ͨ��map���switch case��֧��string���͵����(JAVA 1.7���²�֧��)**/
		
		switch(mode.get(string)){
		case 1:{//normal
			for (int i = 1; i <= nrofGroups; i++){
				Settings s = new Settings(GROUP_NS);
				int nrofHosts = s.getInt(NROF_HOSTS_S);
				
				List<DTNHost> hosts = new ArrayList<DTNHost>();
				hosts.addAll(createInitialHosts(nrofHosts, 1, "LEO"));//��ʼ������ָ�������Ľڵ�
				//ȫ�ֽڵ��б�this.hosts�ں���createInitialHosts()���Ѿ����¹�
			}
			
			Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>> gridInfo = 
					new Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>>(null, null);
			boolean firstCalculationLable = true;
			for (DTNHost host : this.hosts){//����������ȫ���б����
				host.changeHostsList(hosts);
				/*��Ϊÿ���ڵ�ĳ�ʼ���������һ�������Ծ�����һ���̣�ֻ����һ�Σ�����ĸ��Ƽ���*/
				if (firstCalculationLable == true){
					gridInfo = host.initialzationRouter(firstCalculationLable, gridInfo);//·�ɳ�ʼ��
					firstCalculationLable = false;
				}
				else{
					host.initialzationRouter(firstCalculationLable, gridInfo);
				}

				/*��Ϊÿ���ڵ�ĳ�ʼ���������һ�������Ծ�����һ���̣�ֻ����һ�Σ�����ĸ��Ƽ���*/
			}
			break;
		}
		case 2:{//cluster, �ִ�·��--��Դ��ģ�ڵ�		
			Settings sat = new Settings(GROUP_NS);
			int TOTAL_SATELLITES = sat.getInt(NROF_HOSTS_S);//�ܽڵ���
			int TOTAL_PLANE = setting.getInt(NROFPLANE_S);//���ƽ����
			int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
			
					
			/**1.��������MEO����ڵ�**/
			/**ע�⣬�����ָ���ִصĻ���group���к�Ϊ1�Ľڵ�����һ������MEO����ڵ�**/
			List<DTNHost> hostsinMEO = new ArrayList<DTNHost>();
			hostsinMEO.addAll(createInitialHosts(nrofMEO, 1, "MEO"));//��ʼ������ָ�������Ľڵ�
			for (DTNHost host : hostsinMEO){
				host.changeHostsinMEO(hostsinMEO);//����MEO����ڵ��б�
			}
			
			/**2.֮�����ɷִص�LEO�ڵ�,��nrofGroups��������ʾ�ִص�����**/
			/**ע�⣬�����ָ���ִصĻ���group���к�Ϊ1�Ľڵ�����һ������MEO����ڵ㣬��2��ʼ��group������ͨ�͹�����LEO�ڵ�**/
			Settings s = new Settings(GROUP_NS);
			int nrofHosts = s.getInt(NROF_HOSTS_S);//ָ��������LEO�ڵ�
			
			HashMap<Integer, List<DTNHost>> hostsinEachPlane = new HashMap<Integer, List<DTNHost>>();/*�ҳ��������ƽ���ϵĽڵ�*/
			
			for (int i = 1; i <= nrofGroups; i++){
				List<DTNHost> hosts = new ArrayList<DTNHost>();
				hosts.addAll(createInitialHosts(nrofHosts, 1, "LEO"));//��ʼ������ָ��������LEO�ڵ�
				//ȫ�ֽڵ��б�this.hosts�ں���createInitialHosts()���Ѿ����¹�
				
				/*�ҳ��������ƽ���ϵĽڵ�*/
				
				for (int j = 1; j <= TOTAL_PLANE; j++){
					List<DTNHost> hostsinthePlane = new ArrayList<DTNHost>();
					for (DTNHost h : hosts){//�����е�LEO�ڵ㵱�У����ݹ��ƽ��Ĳ�ͬ���з���
						if (h.getNrofPlane() == j){
							hostsinthePlane.add(h);
							h.changeClusterNumber(j);
						}
					}
					hostsinEachPlane.put(j, hostsinthePlane);//�����ڵ�j�����ƽ���ϵĽڵ��б�
				}
				/*�ҳ��������ƽ���ϵĽڵ�,���ں����Ѹ������ڵĽڵ��б�浽���ڽڵ㵱��*/
								
				for (DTNHost host : hosts){					
					host.changeHostsinCluster(hostsinEachPlane.get(host.getNrofPlane()));//���뱾���ڵĽڵ��б�
					host.changeHostsinMEO(hostsinMEO);//����MEO����ڵ��б�
				}
			}	
			Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>> gridInfo = 
					new Tuple<HashMap <DTNHost, List<GridCell>>, HashMap <DTNHost, List<Double>>>(null, null);
			boolean firstCalculationLable = true;
			for (DTNHost host : this.hosts){//������MEO����LEO������ȫ�ֽڵ��б����
				host.changeHostsClusterList(hostsinEachPlane);//�������дصĽڵ��б�
				host.changeHostsList(hosts);
				/*��Ϊÿ���ڵ�ĳ�ʼ���������һ�������Ծ�����һ���̣�ֻ����һ�Σ�����ĸ��Ƽ���*/
				if (firstCalculationLable == true){
					gridInfo = host.initialzationRouter(firstCalculationLable, gridInfo);//·�ɳ�ʼ��
					firstCalculationLable = false;
				}
				else
					host.initialzationRouter(firstCalculationLable, gridInfo);
				/*��Ϊÿ���ڵ�ĳ�ʼ���������һ�������Ծ�����һ���̣�ֻ����һ�Σ�����ĸ��Ƽ���*/
			}
			break;
		}

		default:
			assert false : "the setting of userSetting.hostsMode error!";
		}
	}
	
	public double[] initialSatelliteParameters(int m, int NROF_SATELLITES, int NROF_PLANE){
		
		double[] parameters = new double[6];
		/*��������*/
		//Settings s = new Settings(GROUP_NS);
		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//�ܽڵ���
		//int NROF_PLANE = 3;//���ƽ����
		int NROF_S_EACHPLANE = NROF_SATELLITES/NROF_PLANE;//ÿ�����ƽ���ϵĽڵ���
		
		//Random random = new Random();
		//parameters[0]= random.nextInt(9000)%(2000+1) + 2000;
		/**����뾶Ϊ6371km**/
		parameters[0]= 6371 + 780;//��λ��km
		//this.parameters[0]=8000.0;
		parameters[1]= 0;//0.1ƫ���ʣ�Ӱ��ϴ�,e=c/a
		parameters[2]= 86.4;//64.8;
		parameters[3]= (360/NROF_S_EACHPLANE)*(Math.floor(m/NROF_S_EACHPLANE) + 1);//m��0��ʼ
		parameters[4]= 0;
		if ((Math.floor(m/NROF_S_EACHPLANE) + 1) % 2 == 1 )
			parameters[5] = (360/NROF_S_EACHPLANE)*(m-Math.floor(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE);
		else
			parameters[5] = (360/NROF_S_EACHPLANE)*(m-Math.floor(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE + 0.5);
		//parameters[4]= (360/period)*((m-Math.floor(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE)) + (360/period)*NROF_PLANE*(Math.floor(m/NROF_S_EACHPLANE));
		//parameters[4]= (360/NROF_S_EACHPLANE)*((m-Math.floor(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE)) + (360/NROF_SATELLITES)*(Math.floor(m/NROF_S_EACHPLANE));
		//parameters[5]= 0.0;
		
		System.out.println(m + "  " + parameters[3] + "  " +parameters[4] + "  " + parameters[5]);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//�����������ƽ����
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//�����ڹ��ƽ���ڵı��
		
		return parameters;
	}
	public double[] initialMEOParameters(int m, int NROF_SATELLITES, int NROF_PLANE){
		double[] parameters = new double[6];
		/*��������*/
		//Settings s = new Settings(GROUP_NS);
		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//�ܽڵ���
		//int NROF_PLANE = 3;//���ƽ����
		int NROF_S_EACHPLANE = NROF_SATELLITES/NROF_PLANE;//ÿ�����ƽ���ϵĽڵ���
		
		//Random random = new Random();
		//parameters[0]= random.nextInt(9000)%(2000+1) + 2000;
		/**����뾶Ϊ6371km**/
		parameters[0]= 6371 + 1500;//��λ��km
		//this.parameters[0]=8000.0;
		parameters[1]= 0;//0.1ƫ���ʣ�Ӱ��ϴ�,e=c/a
		parameters[2]= 90;
		//parameters[2] = random.nextInt(15);
		//parameters[3] = random.nextInt(15);
		parameters[3]= (360/NROF_PLANE)*(m/NROF_S_EACHPLANE);//0.0;
		parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_SATELLITES)*(m/NROF_S_EACHPLANE);//0.0;
		parameters[5]= 0.0;//0.0;
		
		System.out.println(m);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//�����������ƽ����
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//�����ڹ��ƽ���ڵı��
		
		return parameters;
	}
	
}