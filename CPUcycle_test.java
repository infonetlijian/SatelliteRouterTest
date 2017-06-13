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
	
	public static void reading_operation(){
		DTNHost host = new DTNHost();
	}
	public void updateGrid_without_OrbitCalculation(){
		if (gridLocation.isEmpty())//初始化只执行一次
			initializeGridLocation();
		
		ginterfaces.clear();//每次清空
		//Coord location = new Coord(0,0); 	// where is the host
		double simClock = SimClock.getTime();
		double time = simClock;
		
			HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
			for (DTNHost host : hosts){
				List<GridCell> gridCellList = this.gridLocation.get(host);
				List<Double> timeList = this.gridTime.get(host);
				double period = this.periodMap.get(host);
				double t0 = time;
				GridCell cell = new GridCell();
				boolean label = false;
				int iterator = 0;
				if (time >= period)
					t0 = t0 % period;//大于周期就取余操作
				for (double t : timeList){
					if (t >= t0){
						cell = gridCellList.get(iterator);
						label = true;
						break;
					}
					iterator++;//找到与timeList时间对应的网格所在位置,iterator 代表在这两个list中的指针						
				}				
				//System.out.println(host+" number "+cell.getNumber()[0]+cell.getNumber()[1]+cell.getNumber()[2]);
				//System.out.println(host+" error!!! "+label);
				assert label : "grid calculation error";
				
				this.ginterfaces.put(host.getInterface(1), cell);
				
				List<DTNHost> hostList = new ArrayList<DTNHost>();
				if (cellToHost.containsKey(cell)){
					hostList = cellToHost.get(cell);	
				}
				hostList.add(host);
				cellToHost.put(cell, hostList);
			}		
			cellmap.put(time, cellToHost);
			gridmap.put(time, ginterfaces);//预测未来time时间里节点和网格之间的对应关系
			//ginterfaces.clear();//每次清空
			ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空
			//CreateGrid(cellSize);//包含cells的new和ginterfaces的new
	}
	
	public void initializeGridLocation(){	
		this.host.getHostsList();
		for (DTNHost h : this.host.getHostsList()){//對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
			double period = getPeriodofOrbit(h);
			this.periodMap.put(h, period);
			System.out.println(this.host+" now calculate "+h+"  "+period);
			
			List<GridCell> gridList = new ArrayList<GridCell>();
			List<Double> intoTime = new ArrayList<Double>();
			List<Double> outTime = new ArrayList<Double>();
			GridCell startCell;//记录起始网格
			for (double time = 0; time < period; time += updateInterval){
				Coord c = h.getCoordinate(time);
				GridCell gc = cellFromCoord(c);//根據坐標找到對應的網格
				if (!gridList.contains(gc)){
					if (gridList.isEmpty())
						startCell = gc;//记录起始网格
					gridList.add(gc);//第一次检测到节点进入此网格（注意，边界检查！！！开始和结束的时候！！！!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!）
					intoTime.add(time);//记录相应的进入时间
				}	
				else{
					//if (gc. == startCell)
						//intoTime = time;
				}
			}
			gridLocation.put(h, gridList);//遍历完一个节点就记录下来
			gridTime.put(h, intoTime);
		}
		System.out.println(gridLocation);
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
  