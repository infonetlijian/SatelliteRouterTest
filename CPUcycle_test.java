import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import routing.GridRouter.GridNeighbors.GridCell;
import movement.SatelliteMovement;
import core.Coord;
import core.DTNHost;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;

/** 
 * Project Name:SatelliteRouterTest 
 * File Name:CPUcycle_test.java 
 * Package Name: 
 * Date:2017年3月26日下午12:19:10 
 * Copyright (c) 2017, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
 */

/** 
 * ClassName:CPUcycle_test <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2017年3月26日 下午12:19:10 <br/> 
 * @author   USTC, LiJian
 * @version   
 * @since    JDK 1.7 
 * @see       
 */
public class CPUcycle_test {
	
	private HashMap<NetworkInterface, GridCell> ginterfaces;
	
	private List<GridCell[][][]> GridList = new ArrayList<GridCell[][][]>();
	private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();
	private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
	
	/*用于初始化时，计算各个节点在一个周期内的网格坐标*/
	private HashMap <DTNHost, List<GridCell>> gridLocation = new HashMap<DTNHost, List<GridCell>>();//存放节点所经过的网格
	private HashMap <DTNHost, List<Double>> gridTime = new HashMap<DTNHost, List<Double>>();//存放节点经过这些网格时的时间
	private HashMap <DTNHost, Double> periodMap = new HashMap <DTNHost, Double>();//记录各个节点轨道的周期
	
	public static void main(String[] args) {
		double t0 = System.currentTimeMillis();
		System.out.println(t0);
		int m=0;
		int dx = 1,dy = 2 ,dz = 3;
		int distance = 100;
		boolean label;
		for (int i = 0; i<100000000;i++){
			if (Math.sqrt(dx*dx + dy*dy +dz*dz) > distance){
				label = true;
			}
			else
				label = false;
		}
		double t1 = System.currentTimeMillis();
		System.out.println(t1-t0);
		//orbit_test();
	}
	
	public static void orbit_test(){
		int TOTAL_SATELLITES = 1;//总节点数
		int TOTAL_PLANE = 1;//轨道平面数
		int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//每个轨道平面上的节点数
		int NROF_PLANE = 1;
		int NROF_SATELLITES = 1;

		
		double t0 = System.currentTimeMillis();
		System.out.println(t0);
		Settings set = new Settings();
		for(int m = 0; m < TOTAL_SATELLITES; m++){
			SatelliteMovement s = new SatelliteMovement(set);
			
			double[] parameters = new double[6];
			parameters[0]= 6371 + 785;//单位是km
			//this.parameters[0]=8000.0;
			parameters[1]= 0;//0.1偏心率，影响较大,e=c/a
			parameters[2]= 90;
			//parameters[2] = random.nextInt(15);
			//parameters[3] = random.nextInt(15);
			parameters[3]= (360/NROF_PLANE)*(m/NROF_S_EACHPLANE);//0.0;
			parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_SATELLITES)*(m/NROF_S_EACHPLANE);//0.0;
			parameters[5]= 0.0;//0.0;
			
			s.setOrbitParameters(parameters);	
				//s.calculateOrbitCoordinate(parameters, time);
			double[] coord = s.getSatelliteCoordinate(m);
			
//			double distance = 100;
//			boolean label;
//			double dx = coord[0], dy = coord[1], dz = coord[2];
//			if (Math.sqrt(dx*dx + dy*dy +dz*dz) > distance){
//				label = true;
//			}
//			else
//				label = false;
		}
		double t1 = System.currentTimeMillis();
		System.out.println("cost:  "+ (t1-t0));
	}
	


	

	
	public class GridCell {
		// how large array is initially chosen
		private static final int EXPECTED_INTERFACE_COUNT = 18;
		//private ArrayList<NetworkInterface> interfaces;//GridCell就是依靠维护网络接口列表，来记录在此网格内的节点，对于全局网格来说，需要保证同一个网络接口不会同时出现在两个GridCell中
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
  