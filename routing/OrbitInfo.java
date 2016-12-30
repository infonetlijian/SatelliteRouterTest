/** 
 * Project Name:SatelliteRouterTest 
 * File Name:OrbitInfo.java 
 * Package Name:routing 
 * Date:2016��12��29������11:29:21 
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
 * Date:     2016��12��29�� ����11:29:21 <br/> 
 * @author   USTC, LiJian
 * @version   
 * @since    JDK 1.7 
 * @see       
 */
public class OrbitInfo {
	private DTNHost thisHost;
	/**���ǹ��������ʼ����ֵ**/
	private  double[] orbitParameters;	
	/**ȫ�����ǽڵ��б�**/
	private List<DTNHost> hosts;
	/**ͬһ�����ڵĽڵ��б�**/
	private List<DTNHost> hostsinCluster = new ArrayList<DTNHost>();
	/**�������ǵĽڵ��б�**/
	private List<DTNHost> hostsinMEO = new ArrayList<DTNHost>();
	private HashMap<Integer, List<DTNHost>> ClusterList;
	private int ClusterNumber;
	
	public OrbitInfo(DTNHost host, List<DTNHost> allHosts, double[] parameters){
		/**�����ǽڵ㣬��ʾ�������Ϣ�ṹ�������ĸ��ڵ��������**/
		thisHost = host;
		/**ȫ�����ǽڵ��б�**/
		hosts = new ArrayList<DTNHost>();
		hosts = allHosts;
		
		/**���ǹ��������ʼ����ֵ**/
		orbitParameters = new double[parameters.length];
		for (int i = 0; i < parameters.length; i++)
			orbitParameters[i] = parameters[i];
		
		ClusterList = new HashMap<Integer, List<DTNHost>>();
	}
	/**
	 * ���ذ󶨵����ǽڵ�
	 * @return
	 */
	public DTNHost getThisHost(){
		return thisHost;
	}
	/**
	 * ����ȫ�ֽڵ��б�
	 * @return
	 */
	public List<DTNHost> getHosts(){
		return hosts;
	}
	
}
  