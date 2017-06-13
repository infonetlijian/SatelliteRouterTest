import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



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
public class txtDataAnalysis {
	

	
	public static void main(String[] args) {
		changeTextFile txtFile = new changeTextFile();
		List<String> txtContent = txtFile.getContextofTxt();
		int nrofLine = txtContent.size();
		for (int i = 0; i < nrofLine ;i++){
			String thisLine = txtContent.get(i);
			System.out.println(thisLine);
			System.out.println(thisLine.toCharArray()[0]);
		}
	}
	
	/**
	 * �������ļ���д��ָ�������ԺͶ�Ӧ��ֵ
	 * @param name
	 * @param value
	 */
	public void setSetting(String name,String value) {
		changeTextFile writeFile = new changeTextFile();
		writeFile.write(name, value);
	}
	
	/**
	 * �¹�����࣬���ڶ�ȡ���޸��ı��ļ�
	 */
	public class changeTextFile {
		private FileOutputStream outputStream;
		private InputStream inputStream;
		private PrintWriter writer;
		private BufferedReader reader;
		private List<String> content;
		private final String FILEPATH = "txtData/Data.txt";
		private File file;
		
		/**
		 * ָ���ļ�·����ͬʱ�����ı���ȡ����
		 */
		public changeTextFile(){
			try{
				String str = txtDataAnalysis.class.getClass().getResource("txtData/Data.txt").getPath();//��ȡclasspath��Ŀ¼�µ�txt�ļ�·��
				URLDecoder decoder = new URLDecoder();
				String path = decoder.decode(str,"utf-8");//·���ı����ʽת�����Ա�֧��·���к��пո��������
				file = new File(path);
				
				/**�ȶ�ȡָ���ı��ļ���������**/
				read();
				/**�ȶ�ȡָ���ı��ļ���������**/
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		/**
		 * �����ı�������
		 * @return
		 */
		public List<String> getContextofTxt(){
			return content;
		}
		/**
		 * ��ȡָ���ı������������ݵ�content�б���(���з�ʽ��ȡ)
		 */
		public void read(){
			try{
				inputStream = txtDataAnalysis.class.getClass().getResourceAsStream(FILEPATH);
				reader = new BufferedReader(new InputStreamReader(inputStream,"GBK"));
				content = new ArrayList<String>();
				String read;
				while ((read = reader.readLine()) != null) {
					
					content.add(read);
				}
				reader.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		/**
		 * ��д֮ǰ��ȡ�������ı����ݣ���д�Ĺ����м���Ƿ���ָ������������ǣ��������Ӧֵ���޸�
		 * @param name
		 * @param value
		 */
		public void write(String name, String value){		
			try{
				outputStream = new FileOutputStream(file);
				writer = new PrintWriter(outputStream);
				for (String s : content){
					if (s.startsWith(name)){
						writer.println(name + " = " + value);
					}
					else
						writer.println(s);
				}
				writer.close();	
			}
			catch(IOException e){
				e.printStackTrace();
			}			
		}
	}
}
  