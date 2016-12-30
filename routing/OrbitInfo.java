/** 
 * Project Name:SatelliteRouterTest 
 * File Name:OrbitInfo.java 
 * Package Name:routing 
 * Date:2016年12月29日上午11:29:21 
 * Copyright (c) 2016, LiJian9@mail.ustc.mail.cn. All Rights Reserved. 
 * 
*/  
  
package routing;  

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import core.DTNHost;

/** 
 * ClassName:OrbitInfo <br/> 
 * Function: TODO ADD FUNCTION. <br/> 
 * Reason:   TODO ADD REASON. <br/> 
 * Date:     2016年12月29日 上午11:29:21 <br/> 
 * @author   USTC, LiJian
 * @version   
 * @since    JDK 1.7 
 * @see       
 */
public class OrbitInfo {
	private DTNHost thisHost;
	/**卫星轨道参数初始化赋值**/
	private  double[] orbitParameters;	
	/**全局卫星节点列表**/
	private List<DTNHost> hosts;
	/**同一个簇内的节点列表**/
	private List<DTNHost> hostsinCluster = new ArrayList<DTNHost>();
	/**管理卫星的节点列表**/
	private List<DTNHost> hostsinMEO = new ArrayList<DTNHost>();
	private HashMap<Integer, List<DTNHost>> ClusterList;
	private int ClusterNumber;
	
	public OrbitInfo(DTNHost host, List<DTNHost> allHosts, double[] parameters){
		/**绑定卫星节点，表示本轨道信息结构是属于哪个节点所保存的**/
		thisHost = host;
		/**全局卫星节点列表**/
		hosts = new ArrayList<DTNHost>();
		hosts = allHosts;
		
		/**卫星轨道参数初始化赋值**/
		orbitParameters = new double[parameters.length];
		for (int i = 0; i < parameters.length; i++)
			orbitParameters[i] = parameters[i];
		
		ClusterList = new HashMap<Integer, List<DTNHost>>();
	}
	/**
	 * 返回绑定的卫星节点
	 * @return
	 */
	public DTNHost getThisHost(){
		return thisHost;
	}
	/**
	 * 返回全局节点列表
	 * @return
	 */
	public List<DTNHost> getHosts(){
		return hosts;
	}
	
}
  