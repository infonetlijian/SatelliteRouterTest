package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import jat.orbit.SatelliteOrbit;

public class EASRNeighbors {
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	/** scenario name -setting id ({@value})*/
	public static final String SCENARIONAME_S = "Scenario";
	/** simulation end time -setting id ({@value})*/
	public static final String SIMULATION_END_TIME = "endTime";
	
	private static final double INTERVAL = 10;
	private static final double MIN_PREDICT_TIME = 100;
	private double PREDICT_TIME = 600;
	
	private DTNHost host;
	private double simEndTime;
	private double msgTtl;
	private double	transmitRange;//���õĿ�ͨ�о�����ֵ
	private HashMap<DTNHost, double[]> neighborsLiveTime= new HashMap<DTNHost, double[]>();
	private HashMap<DTNHost, double[]> potentialNeighborsStartTime= new HashMap<DTNHost, double[]>();
	
	private List<DTNHost> neighbors = new ArrayList<DTNHost>();//�ھӽڵ��б� 
	private List<DTNHost> hosts = new ArrayList<DTNHost>();//ȫ�����ǽڵ��б�
	private List<NetworkInterface> potentialNeighbors = new ArrayList<NetworkInterface>();
	/**
	 * ��ʼ������
	 * @param host
	 */
	public EASRNeighbors(DTNHost host){
		this.host = host;
		Settings s = new Settings(INTERFACENAME_S);
		transmitRange = s.getDouble(TRANSMIT_RANGE_S);//�������ļ��ж�ȡ��������
		Settings set = new Settings(SCENARIONAME_S);
		simEndTime = set.getDouble(SIMULATION_END_TIME);
		Settings se = new Settings("Group");
		msgTtl = se.getDouble("msgTtl");
		//System.out.println(msgTtl);
		PREDICT_TIME = msgTtl;
	}
	public EASRNeighbors(List<DTNHost> hosts){
		this.hosts=hosts;
	}
	/**
	 * ����ȫ�ֽڵ��б�
	 * @return
	 */
	public List<DTNHost> getHosts(){
		return this.hosts;
	}
	/**
	 * �޸�Ǳ���ھӽڵ�(��δ�����ܳ�Ϊ�ھӵĽڵ�)�б�����ConnectivityGrid.java�е�getNearInterfaces()��������
	 * @param potentialNeighbors
	 */
	public void changePotentialNeighbors(List<NetworkInterface> potentialNeighbors){
		this.potentialNeighbors = potentialNeighbors;
	}
	/**
	 * ����ȫ�ֽڵ��б�
	 * @param hosts
	 */
	public void changeHostsList(List<DTNHost> hosts){
		this.hosts=hosts;
	}
	/**
	 * ���ص�ǰ�ڵ�������ھӽڵ��б�
	 * @return
	 */
	public List<DTNHost> getNeighbors(){
		return this.neighbors;
	}
	/**
	 * ����Ԥ���ھӽڵ㵽���ھӷ�Χ��ʱ��
	 * @return
	 */
	public HashMap<DTNHost, double[]> getPotentialNeighborsStartTime(){
		return this.potentialNeighborsStartTime;
	}
	/**
	 * �����ھӽڵ�Ĵ���ʱ��
	 * @return
	 */
	public HashMap<DTNHost, double[]> getNeighborsLiveTime(){
		return this.neighborsLiveTime;
	}
	/**
	 * �޸��ھӽڵ��б��Լ���Ӧ������ʱ��
	 * @param nei
	 */
	public void changeNeighbors(List<DTNHost> nei){
		this.neighbors = nei;
		this.neighborsLiveTime.clear();
		for (DTNHost host : nei){
			double[] liveTime = new double[2];
			liveTime[0] = SimClock.getTime();
			liveTime[1] = SimClock.getTime();
			this.neighborsLiveTime.put(host, liveTime);//���µ������ھӽڵ�����б���	
		}
	}
	/**
	 * ��SatelliteLaserInterface.java������ά���뿪���ھӽڵ�
	 * @param host
	 */
	public void removeNeighbor(DTNHost host){
		this.neighbors.remove(host);
		this.neighborsLiveTime.remove(host);
	}
	/**
	 * ��ӵ�ǰ�ڵ���ھӽڵ�
	 * @param host
	 */
	public void addNeighbor(DTNHost host){
		if (host != this.host){
			if (this.neighbors.contains(host))
				;
			else
				this.neighbors.add(host);
			if (this.neighborsLiveTime.containsKey(host))
				;
			else{	
				double[] liveTime = new double[2];
				liveTime[0] = SimClock.getTime();
				liveTime[1] = SimClock.getTime();
				this.neighborsLiveTime.put(host, liveTime);//���µ������ھӽڵ�����б���				
				}
		}
	}
	/**
	 * �ھӽڵ���µ���ڣ�ʵʱ���½ڵ���ھӣ���Ԥ���������ھӽڵ�
	 * @param ni
	 * @param connections
	 */
	public void updateNeighbors(DTNHost host, List<Connection> connections){		
		updateNeighborsEndTime(this.neighborsLiveTime);
		predictAllStartTime();
		predictAllEndTime();
		//for (DTNHost h : this.neighbors)
		//	System.out.println(this.host+"  "+h+"  "+SimClock.getTime()+"  "+this.neighborsLiveTime.get(h)[1]);
		/*Collection<DTNHost> hosts = this.potentialNeighborsStartTime.keySet();
		for (DTNHost h : hosts){
			for (int i = 0; i < 2; i++){
				System.out.print(h+"  "+this.potentialNeighborsStartTime.get(h)[i]+"  ");
			}
			System.out.println("");
		}
	*/
	}
	/**
	 * �����ھӽڵ�δ���뿪��ʱ��
	 * @param host
	 * @return
	 */
	public boolean getNeighborsLeaveTime(DTNHost host){
		if (this.potentialNeighborsStartTime.containsKey(host)){
			double[] liveTime = this.potentialNeighborsStartTime.get(host);
			if (liveTime[0] != liveTime[1] && liveTime[1] > SimClock.getTime()){
				changeNeighborsLiveTime(host, liveTime[1]);//����˽ڵ�ĵ���֮ǰ�Ѿ�Ԥ�����ֱ�Ӱ���������
			}
		}
		
		double timeNow = SimClock.getTime();
		for (double time = timeNow; time < SimClock.getTime()+msgTtl; time += INTERVAL){//������������������
			if (JudgeNeighbors(this.host.getCoordinate(time), host.getCoordinate(time)) == false){//�����Ȼ���ھӷ�Χ�ھͼ�������ʱ�䣬��������
				for (double var = time - INTERVAL; var <= time; var += 0.1){//�ҵ��˴�ŷ�Χ֮���Ϊϸ����������ϵͳ��updateInterval��Ϊ��������
					if (JudgeNeighbors(this.host.getCoordinate(var), host.getCoordinate(var)) == false){
						changeNeighborsLiveTime(host, var);
						return true;//����ѭ��
					}
				}
			}
		}
		return false;
	}
	/**
	 * �����ھӽڵ���뿪ʱ��
	 * @param neighborsLiveTime
	 */
	public void updateNeighborsEndTime(HashMap<DTNHost, double[]> neighborsLiveTime){
		double endTime = SimClock.getTime() + PREDICT_TIME;
		if (!this.neighborsLiveTime.isEmpty() && !this.neighbors.isEmpty()){
			for (DTNHost host : this.neighbors){
				double[] Time = neighborsLiveTime.get(host);
				//double timeNow = SimClock.getTime();
				if (Time[0] != Time[1] || Time[1] == this.simEndTime)//��Ϊ����Ҫ�ظ�Ԥ�⣬���������������������Ԥ��ʱ�䣬����������
					continue;
				else{
					if (getNeighborsLeaveTime(host) == false)
						changeNeighborsLiveTime(host, this.simEndTime);//���������ȫ��ʱ�䶼û���ҵ��뿪ʱ�䣬����˽ڵ�һֱ���ھӷ�Χ��
					else
						continue;
				}	
				
			}		
		}
	}
	/**
	 * �޸��ھ�����ʱ����뿪��ʱ��
	 * @param time
	 */
	public void changeNeighborsLiveTime(DTNHost host, double time){
		double[] liveTime = new double[2];
		liveTime[0] = SimClock.getTime();
		liveTime[1] = time;
		this.neighborsLiveTime.put(host, liveTime);//����ԭ��host��Ӧ��liveTimeֵ
	}
	/**
	 * �޸ĵ�ǰ�����ھӵĽڵ㣬�䵽��ͽ�����ʱ��
	 * @param time
	 */
	public void changePotentialNeighborsTime(DTNHost host, double time1, double time2){
		double[] liveTime = new double[2];
		liveTime[0] = time1;
		liveTime[1] = time2;
		this.potentialNeighborsStartTime.put(host, liveTime);
	}
	/**
	 * ��Ԥ�⵽��Ľڵ㼯���У��Ѿ���Ϊ�ھӵĽڵ��Ƴ�
	 */
	public void removeExistNeighbors(){
		List<DTNHost> existNeighbors = new ArrayList<DTNHost>();
		existNeighbors.clear();
		Collection<DTNHost> potentialNeighborsStartTime = this.potentialNeighborsStartTime.keySet();
		for (DTNHost host : potentialNeighborsStartTime){
			if (this.potentialNeighborsStartTime.get(host)[0] <= SimClock.getTime())//���ڵ���Ԥ��ʱ�����ѳ�Ϊ�ھӵĽڵ���Ƴ�
				if (this.neighbors.contains(host))
					existNeighbors.add(host);
				else
					assert false : this.host+" ����Ԥ��ʱ�䵫�ǻ�û�г�Ϊ�ھӽڵ� "+ host;
		}
		for (DTNHost host : existNeighbors){
			this.potentialNeighborsStartTime.remove(host);
		}
	}
	/**
	 * ����Ԥ��ȫ���ڵ��Ϊ�ھӵ�ʱ��
	 */
	public void predictAllStartTime(){

		//List<DTNHost> hosts = new ArrayList<DTNHost>(); 
		//hosts = this.hosts;
		//hosts.removeAll(this.neighbors);//ȥ���ھӽڵ�
		
		removeExistNeighbors();//ȥ���ѳ�Ϊ�ھӵ�Ԥ��ڵ�!!!
		
		boolean findLabel = false;
		for (DTNHost host : this.hosts){
			findLabel = false;
			if (this.neighbors.contains(host) || host.getAddress() == this.host.getAddress()){//�Ѿ����ھӽڵ�����Ԥ��
				continue;
			}
			else{
				if (this.potentialNeighborsStartTime.containsKey(host) == false){//�Ѿ�Ԥ����ľͲ���������	
					for (double time = SimClock.getTime(); time < SimClock.getTime()+msgTtl; time += INTERVAL){
						if (JudgeNeighbors(this.host.getCoordinate(time) , 
								host.getCoordinate(time)) == true){//�ж�ʲôʱ��Ż��Ϊ�ھ�
							for (double var = time - INTERVAL; var < time; var += 0.1){
								if (JudgeNeighbors(this.host.getCoordinate(time) , 
										host.getCoordinate(time)) == true){//�ж�ʲôʱ��Ż��Ϊ�ھ�
									if (var > 0){
										findLabel = true;
										changePotentialNeighborsTime(host, time, time);
										//System.out.print(this.host+"   "+host+"  "+SimClock.getTime()+"  allNeighborsStartTime is ");
										//System.out.println(time+"  "+this.neighbors + "  "+this.potentialNeighborsStartTime+"  "+this.neighborsLiveTime);
										break;
									}
								}
							}
							break;
						}
					}
					if (findLabel == true)
						continue;
					else {
						changePotentialNeighborsTime(host, -1, -1);//��������ȫ��Ҳû�ҵ�����˲����ܳ�Ϊ�ھӽڵ�
						//System.out.println(this.host+"   "+host+"  NeighborsStartTime is -1  "+this.potentialNeighborsStartTime);
					}
				}
			}
		}

	}
	/**
	 * Ԥ����ܳ�Ϊ�ھӵĽڵ����뿪��ʱ��
	 */
	public void predictAllEndTime(){
		boolean findLabel = false;
		for (DTNHost host : this.potentialNeighborsStartTime.keySet()){
			findLabel = false;
			assert !this.neighbors.contains(host) : "Ԥ��ڵ㱻���������ھӽڵ���";
			if (this.potentialNeighborsStartTime.get(host)[0] == 
					this.potentialNeighborsStartTime.get(host)[1] && 
					this.potentialNeighborsStartTime.get(host)[0] > 0){//��֤��δ�����ܳ�Ϊ�ھӵĽڵ㲻���ظ�Ԥ�⣬ͬʱҲҪ�ų������ܳ�Ϊ�ھӵĽڵ�
				for (double time = this.potentialNeighborsStartTime.get(host)[0]; time < SimClock.getTime()+msgTtl; time += INTERVAL){
					if (JudgeNeighbors(this.host.getCoordinate(time), 
							host.getCoordinate(time)) == false){//�ж�ʲôʱ��Ż��뿪
						for (double var = time - INTERVAL; var < time; var += 0.1){
							if (JudgeNeighbors(this.host.getCoordinate(time), 
									host.getCoordinate(time)) == false){//�ж�ʲôʱ��Ż��뿪
								if (var > 0){
									findLabel = true;
									changePotentialNeighborsTime(host, 
											this.potentialNeighborsStartTime.get(host)[0], var);
									//System.out.print(this.host+"   "+host+"  allNeighborsEndTime is ");
									//System.out.println("  "+this.potentialNeighborsStartTime.get(host)[0]+"  "+this.potentialNeighborsStartTime.get(host)[1]);
									break;
								}
							}
						}break;
					}
				}
				if (findLabel == true)
					continue;
				else 			
					changePotentialNeighborsTime(host, 
							this.potentialNeighborsStartTime.get(host)[0], simEndTime);//˵����ȫ��ʱ��������δ��ĳ��ʱ���Ϊ�ھӺ󲻻����뿪��				
			}
		}
	}
	/**
	 * Ԥ����Щ�����������е��������ھӽڵ������
	 * @param potentialNeighbors
	 */
	public void updatePotentialNeighborsStartTime(List<NetworkInterface> potentialNeighbors){	
		double endTime = SimClock.getTime() + PREDICT_TIME;
		//for (DTNHost host : this.neighbors){
		//	potentialNeighbors.remove(host.getInterface(0));//ȥ����Щ�Ѿ����ھӽڵ��
		//}
		Collection<DTNHost> potentialNeighborsStartTime = this.potentialNeighborsStartTime.keySet();//��ʱ�ľͰ����޳� 
		for (DTNHost host : potentialNeighborsStartTime){
			if (this.potentialNeighborsStartTime.get(host)[0] > SimClock.getIntTime())
				this.potentialNeighborsStartTime.remove(host);
		}
		
		for (NetworkInterface ni : potentialNeighbors){
			if (!this.potentialNeighborsStartTime.containsKey(ni.getHost()) && 
					!this.neighborsLiveTime.containsKey(ni.getHost())){//��֤ÿ���ڵ�ֻ��Ԥ��һ��Ԥ�⣬���Ѿ����ھӵĽڵ㲻��Ԥ��
				for (double time = SimClock.getTime(); time < endTime; time += INTERVAL){
					if (JudgeNeighbors(this.host.getCoordinate(time) , 
							ni.getHost().getCoordinate(time))){//�ж�ʲôʱ��Ż��Ϊ�ھ�
						for (double var = time - INTERVAL; var < time; var += 0.1){
							if (JudgeNeighbors(this.host.getCoordinate(var) , 
									ni.getHost().getCoordinate(var))){
								double[] liveTime = new double[2];
								liveTime[0] = var;
								liveTime[1] = var;
								this.potentialNeighborsStartTime.put(ni.getHost(), liveTime);
								System.out.print(this.host+"   "+ni.getHost()+"  potentialNeighborsStartTime is ");
								System.out.println(liveTime[0]);
								break;//����ѭ��
							}
						}
					}
					break;//�����ڶ���ѭ��
				}
			}	
		}	
		//HashMap<DTNHost,double[]> potentialNeighborsTime = new HashMap<DTNHost,double[]>();
		for (DTNHost host : this.neighbors){
			this.potentialNeighborsStartTime.remove(host);//�Ѿ���Ϊ�ھӽڵ��Ԥ�����ɾ��
			//System.out.println("test!");
		}	
	}	
	/**
	 * ������ʱ��tʱ�����ǽڵ���ھӽڵ㲢�洢��neighbors��
	 * @param parameters
	 * @param t
	 */
	public void CalculateNeighbor(DTNHost host,double t){
		double[][] myCoordinate=new double[1][3];	
		
		myCoordinate=GetCoordinate(host.getParameters(),t);
		int nrofhosts=this.hosts.size();
		
		int index=this.hosts.indexOf(host);
		ChangeMyHost(index);
		
		for(int n=1;n<nrofhosts;n++){//�б��нڵ��������nrofhosts����ĩβ�Ǹ��Ǳ��ڵ㣬���Բ�����
			if(JudgeNeighbors(GetCoordinate(hosts.get(n).getParameters(),t),myCoordinate)){
				neighbors.add(hosts.get(n));
			}
		}
		//System.out.print(hosts.get(index));//ϵͳ�����ӡ���ھӵĽڵ�
		//System.out.println(neighbors);
	}
	/**
	 * ���б��б�DTNHost�ŵ���̬�����ĩβȥ��ֻ�����������ڵ�֮��ľ���
	 * @param index
	 */
	public void ChangeMyHost(int index){
		DTNHost host=this.hosts.get(index);
		this.hosts.remove(index);
		this.hosts.add(host);//�ı䱾DTNHost�ڱ��е�˳��
	}
	/**
	 * ������������ʱ�䣬�õ���Ӧ��ά����λ�ã���ά����[1][3]
	 * @param parameters
	 * @param t
	 * @return
	 */
	public double[][] GetCoordinate(double[] parameters,double t){
		SatelliteOrbit saot=new SatelliteOrbit(parameters);
		double[][] myCoordinate;
		myCoordinate=saot.getSatelliteCoordinate(t);
		return myCoordinate;
	}
	/**
	 * ��Coord��������о������
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean JudgeNeighbors(Coord c1,Coord c2){

		double distance = c1.distance(c2);
		if (distance <= this.transmitRange)
			return true;
		else
			return false;
	}	
	/**
	 * ����true�������ھӽڵ㣬��ͨ�ŷ�Χ�ڣ�����false����ͨ�ŷ�Χ֮��
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean JudgeNeighbors(double[][] c1,double[][] c2){
		double var;
		var=(c1[0][0]-c2[0][0])*(c1[0][0]-c2[0][0])+(c1[0][1]-c2[0][1])*(c1[0][1]-c2[0][1])+(c1[0][2]-c2[0][2])*(c1[0][2]-c2[0][2]);
		var=EnsurePositive(var);
		if (Math.sqrt(var) <= this.transmitRange)
			return true;
		else 
			return false;
	}
	public double EnsurePositive(double var){
		if (var>=0);
		else
			var=-var;
		return var;
	}
}
