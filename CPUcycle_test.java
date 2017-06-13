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
 * Date:2017��3��26������12:19:10 
 * Copyright (c) 2017, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
 */

/** 
 * ClassName:CPUcycle_test <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2017��3��26�� ����12:19:10 <br/> 
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
	
	/*���ڳ�ʼ��ʱ����������ڵ���һ�������ڵ���������*/
	private HashMap <DTNHost, List<GridCell>> gridLocation = new HashMap<DTNHost, List<GridCell>>();//��Žڵ�������������
	private HashMap <DTNHost, List<Double>> gridTime = new HashMap<DTNHost, List<Double>>();//��Žڵ㾭����Щ����ʱ��ʱ��
	private HashMap <DTNHost, Double> periodMap = new HashMap <DTNHost, Double>();//��¼�����ڵ���������
	
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
		int TOTAL_SATELLITES = 1;//�ܽڵ���
		int TOTAL_PLANE = 1;//���ƽ����
		int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//ÿ�����ƽ���ϵĽڵ���
		int NROF_PLANE = 1;
		int NROF_SATELLITES = 1;

		
		double t0 = System.currentTimeMillis();
		System.out.println(t0);
		Settings set = new Settings();
		for(int m = 0; m < TOTAL_SATELLITES; m++){
			SatelliteMovement s = new SatelliteMovement(set);
			
			double[] parameters = new double[6];
			parameters[0]= 6371 + 785;//��λ��km
			//this.parameters[0]=8000.0;
			parameters[1]= 0;//0.1ƫ���ʣ�Ӱ��ϴ�,e=c/a
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
		if (gridLocation.isEmpty())//��ʼ��ִֻ��һ��
			initializeGridLocation();
		
		ginterfaces.clear();//ÿ�����
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
					t0 = t0 % period;//�������ھ�ȡ�����
				for (double t : timeList){
					if (t >= t0){
						cell = gridCellList.get(iterator);
						label = true;
						break;
					}
					iterator++;//�ҵ���timeListʱ���Ӧ����������λ��,iterator ������������list�е�ָ��						
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
			gridmap.put(time, ginterfaces);//Ԥ��δ��timeʱ����ڵ������֮��Ķ�Ӧ��ϵ
			//ginterfaces.clear();//ÿ�����
			ginterfaces = new HashMap<NetworkInterface, GridCell>();//ÿ�����
			//CreateGrid(cellSize);//����cells��new��ginterfaces��new
	}
	
	public void initializeGridLocation(){	
		this.host.getHostsList();
		for (DTNHost h : this.host.getHostsList()){//��ÿ�����c��vһ���L�ڣ�ӛ���һ���L�ڃȱ�v�^�ľW�񣬲��ҵ��������M����x�_�r�g
			double period = getPeriodofOrbit(h);
			this.periodMap.put(h, period);
			System.out.println(this.host+" now calculate "+h+"  "+period);
			
			List<GridCell> gridList = new ArrayList<GridCell>();
			List<Double> intoTime = new ArrayList<Double>();
			List<Double> outTime = new ArrayList<Double>();
			GridCell startCell;//��¼��ʼ����
			for (double time = 0; time < period; time += updateInterval){
				Coord c = h.getCoordinate(time);
				GridCell gc = cellFromCoord(c);//���������ҵ������ľW��
				if (!gridList.contains(gc)){
					if (gridList.isEmpty())
						startCell = gc;//��¼��ʼ����
					gridList.add(gc);//��һ�μ�⵽�ڵ���������ע�⣬�߽��飡������ʼ�ͽ�����ʱ�򣡣���!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!��
					intoTime.add(time);//��¼��Ӧ�Ľ���ʱ��
				}	
				else{
					//if (gc. == startCell)
						//intoTime = time;
				}
			}
			gridLocation.put(h, gridList);//������һ���ڵ�ͼ�¼����
			gridTime.put(h, intoTime);
		}
		System.out.println(gridLocation);
	}
	
	public class GridCell {
		// how large array is initially chosen
		private static final int EXPECTED_INTERFACE_COUNT = 18;
		//private ArrayList<NetworkInterface> interfaces;//GridCell��������ά������ӿ��б�����¼�ڴ������ڵĽڵ㣬����ȫ��������˵����Ҫ��֤ͬһ������ӿڲ���ͬʱ����������GridCell��
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
  