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

	private int LEOtotalSatellites;//�ܽڵ���
	private int LEOtotalPlane;//���ƽ����
	private int nrofPlane;//�����������ƽ����
	private int nrofSatelliteINPlane;//�����ڹ��ƽ���ڵı��
	
	private List<DTNHost> hosts = new ArrayList<DTNHost>();//ȫ�����ǽڵ��б�
	private List<DTNHost> hostsinCluster = new ArrayList<DTNHost>();//ͬһ�����ڵĽڵ��б�
	private List<DTNHost> hostsinMEO = new ArrayList<DTNHost>();//�������ǵĽڵ��б�
	
	private int ClusterNumber;//�����ڵ��������Ĵ����
	
	private HashMap<Integer, List<DTNHost>> ClusterList = new HashMap<Integer, List<DTNHost>>();//ÿ���صı�ţ��Ͷ�Ӧ�Ĵ��ڽڵ��б�
	
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
	 * ��ʼ��ʱ����һ�Σ����ù�����������Ϣ
	 * @param LEOtotalSatellites
	 * @param LEOtotalPlane
	 * @param nrofPlane
	 * @param nrofSatelliteInPlane
	 * @param parameters
	 */
	public void setOrbitParameters(int LEOtotalSatellites, int LEOtotalPlane, int nrofPlane, int nrofSatelliteInPlane, double[] parameters){
		setOrbitParameters(parameters);
		
		/*��������*/
		this.LEOtotalSatellites = LEOtotalSatellites;//�ܽڵ���
		this.LEOtotalPlane = LEOtotalPlane;//���ƽ����
		this.nrofPlane = nrofPlane;//�����������ƽ����
		this.nrofSatelliteINPlane = nrofSatelliteInPlane;//�����ڹ��ƽ���ڵı��
	}
	/**
	 * �������ǹ��6����
	 * @param parameters
	 */
	public void setOrbitParameters(double[] parameters){
		assert parameters.length >= 6 : "��������ǹ��������ȫ";
			
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
	 * ��ȡָ��ʱ�����������
	 * @param time
	 * @return
	 */
	public double[] getSatelliteCoordinate(double time){
		double[][] coordinate = new double[1][3];
		double[] xyz = new double[3];
		
		Settings s = new Settings("MovementModel");
		int worldSize[] = s.getCsvInts("worldSize");
		
		coordinate = satelliteOrbit.getSatelliteCoordinate(time);
		/**ONE�еľ��뵥λΪmeter������JAT�еĹ���뾶��λΪkm������ڵõ���������Ӧ��*1000����ת��**/
//		xyz[0] = (coordinate[0][0]*1000 + worldSize/2);//�����Sƽ��
//		xyz[1] = (coordinate[0][1]*1000 + worldSize/2);
//		xyz[2] = (coordinate[0][2]*1000 + worldSize/2);
		/**ONE�еľ��뵥λΪmeter������JAT�еĹ���뾶��λΪkm����˴���ͳһ���ţ���ONE�еľ��뵥λҲ����km��ͬʱ����ƽ��������Ϊworld��С��һ��**/
		xyz[0] = (coordinate[0][0] + worldSize[0]/2);//�����Sƽ��
		xyz[1] = (coordinate[0][1] + worldSize[0]/2);
		xyz[2] = (coordinate[0][2] + worldSize[0]/2);
		
		return xyz;
	}
	/**
	 * ����ָ���������������ָ��ʱ��Ĺ������
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
	 * ���������������ƽ���Ų���
	 */
	public int getNrofPlane(){
		return this.nrofPlane;
	}
	/**
	 * ���������ڹ��ƽ���ڵı��
	 */
	public int getNrofSatelliteINPlane(){
		return this.nrofSatelliteINPlane;
	}
	/**
	 * �����������������������ǹ������
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
