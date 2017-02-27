package movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import core.Coord;
import core.DTNHost;
import core.Settings;
import jat.orbit.SatelliteOrbit;

public class SatelliteMovement extends MovementModel{
	
	private double a = 8000.; // sma in km
	private double e = 0.1; // eccentricity
	private double i = 15; // inclination in degrees
	private double raan = 0.0; // right ascension of ascending node in degrees
	private double w = 0.0; // argument of perigee in degrees
	private double ta = 0.0; // true anomaly in degrees
	private double[] orbitParameters;
	
	private SatelliteOrbit satelliteOrbit;

	private int LEOtotalSatellites;//总节点数
	private int LEOtotalPlane;//轨道平面数
	private int nrofPlane;//卫星所属轨道平面编号
	private int nrofSatelliteINPlane;//卫星在轨道平面内的编号
	
	private List<DTNHost> hosts = new ArrayList<DTNHost>();//全局卫星节点列表
	private List<DTNHost> hostsinCluster = new ArrayList<DTNHost>();//同一个簇内的节点列表
	private List<DTNHost> hostsinMEO = new ArrayList<DTNHost>();//管理卫星的节点列表
	
	private int ClusterNumber;//代表本节点所归属的簇序号
	
	private HashMap<Integer, List<DTNHost>> ClusterList = new HashMap<Integer, List<DTNHost>>();//每个簇的编号，和对应的簇内节点列表
	
	public SatelliteMovement(Settings settings) {
		super(settings);
	}
	
	protected SatelliteMovement(SatelliteMovement rwp) {
		super(rwp);
	}
	public void setHostsinMEO(List<DTNHost> hostsinMEO){
		this.hostsinMEO = hostsinMEO;
	}
	public void setHostsinCluster(List<DTNHost> hostsinCluster){
		this.hostsinCluster = hostsinCluster;
	}
	public void setHostsClusterList(HashMap<Integer, List<DTNHost>> hostsinEachPlane){
		this.ClusterList = hostsinEachPlane;
	}
	public void setHostsList(List<DTNHost> hosts){
		this.hosts = hosts;
	}
	/**
	 * 初始化时调用一次，设置轨道参数相关信息
	 * @param LEOtotalSatellites
	 * @param LEOtotalPlane
	 * @param nrofPlane
	 * @param nrofSatelliteInPlane
	 * @param parameters
	 */
	public void setOrbitParameters(int LEOtotalSatellites, int LEOtotalPlane, int nrofPlane, int nrofSatelliteInPlane, double[] parameters){
		setOrbitParameters(parameters);
		
		/*新增参数*/
		this.LEOtotalSatellites = LEOtotalSatellites;//总节点数
		this.LEOtotalPlane = LEOtotalPlane;//轨道平面数
		this.nrofPlane = nrofPlane;//卫星所属轨道平面编号
		this.nrofSatelliteINPlane = nrofSatelliteInPlane;//卫星在轨道平面内的编号
	}
	/**
	 * 设置卫星轨道6参数
	 * @param parameters
	 */
	public void setOrbitParameters(double[] parameters){
		assert parameters.length >= 6 : "传入的卫星轨道参数不全";
			
		this.a = parameters[0]; // sma in km
		this.e = parameters[1]; // eccentricity
		this.i = parameters[2]; // inclination in degrees
		this.raan = parameters[3]; // right ascension of ascending node in degrees
		this.w = parameters[4]; // argument of perigee in degrees
		this.ta = parameters[5]; // true anomaly in degrees
		
		this.orbitParameters = new double[6];
		for (int j = 0 ; j < 6 ; j++){
			this.orbitParameters[j] = parameters[j];
		}

		this.satelliteOrbit = new SatelliteOrbit(this.orbitParameters);
	}
	/**
	 * 获取指定时间的卫星坐标
	 * @param time
	 * @return
	 */
	public double[] getSatelliteCoordinate(double time){
		double[][] coordinate = new double[1][3];
		double[] xyz = new double[3];
		
		Settings s = new Settings("MovementModel");
		int worldSize[] = s.getCsvInts("worldSize");
		
		coordinate = satelliteOrbit.getSatelliteCoordinate(time);
		/**ONE中的距离单位为meter，但是JAT中的轨道半径单位为km，因此在得到的坐标中应该*1000进行转换**/
//		xyz[0] = (coordinate[0][0]*1000 + worldSize/2);//坐標軸平移
//		xyz[1] = (coordinate[0][1]*1000 + worldSize/2);
//		xyz[2] = (coordinate[0][2]*1000 + worldSize/2);
		/**ONE中的距离单位为meter，但是JAT中的轨道半径单位为km，因此此做统一缩放，将ONE中的距离单位也视作km，同时坐标平移量保持为world大小的一半**/
		xyz[0] = (coordinate[0][0] + worldSize[0]/2);//坐標軸平移
		xyz[1] = (coordinate[0][1] + worldSize[0]/2);
		xyz[2] = (coordinate[0][2] + worldSize[0]/2);
		
		return xyz;
	}
	/**
	 * 根据指定轨道参数，计算指定时间的轨道坐标
	 * @param parameters
	 * @param time
	 * @return
	 */
	public double[] calculateOrbitCoordinate(double[] parameters, double time){
		double[][] coordinate = new double[1][3];
		
		SatelliteOrbit so = new SatelliteOrbit(parameters);
		coordinate = so.getSatelliteCoordinate(time);
		
		return coordinate[0];
	}
	/**
	 * 返回卫星所属轨道平面编号参数
	 */
	public int getNrofPlane(){
		return this.nrofPlane;
	}
	/**
	 * 返回卫星在轨道平面内的编号
	 */
	public int getNrofSatelliteINPlane(){
		return this.nrofSatelliteINPlane;
	}
	/**
	 * 新增函数，返回新增的卫星轨道参数
	 * @return
	 */
	public double[] getParameters(){
		return this.orbitParameters;
	}
	/**
	 * Returns a possible (random) placement for a host
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		Coord c = randomCoord();

		//this.lastWaypoint = c;
		return c;
	}
	
	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		//p.addWaypoint(lastWaypoint.clone());
		//Coord c = lastWaypoint;
		
		//for (int i=0; i<PATH_LENGTH; i++) {
		//	c = randomCoord();
		//	p.addWaypoint(c);	
		//}
		
		//this.lastWaypoint = c;
		return p;
	}
	
	@Override
	public SatelliteMovement replicate() {
		return new SatelliteMovement(this);
	}
	
	protected Coord randomCoord() {
		return new Coord(rng.nextDouble() * getMaxX(),
				rng.nextDouble() * getMaxY());
	}
	
}
