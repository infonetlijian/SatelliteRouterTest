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
	 * 向配置文件中写入指定的属性和对应的值
	 * @param name
	 * @param value
	 */
	public void setSetting(String name,String value) {
		changeTextFile writeFile = new changeTextFile();
		writeFile.write(name, value);
	}
	
	/**
	 * 新构造的类，用于读取和修改文本文件
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
		 * 指定文件路径，同时调用文本读取函数
		 */
		public changeTextFile(){
			try{
				String str = txtDataAnalysis.class.getClass().getResource("txtData/Data.txt").getPath();//读取classpath根目录下的txt文件路径
				URLDecoder decoder = new URLDecoder();
				String path = decoder.decode(str,"utf-8");//路径的编码格式转换，以便支持路径中含有空格或者中文
				file = new File(path);
				
				/**先读取指定文本文件的所有行**/
				read();
				/**先读取指定文本文件的所有行**/
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		/**
		 * 返回文本的内容
		 * @return
		 */
		public List<String> getContextofTxt(){
			return content;
		}
		/**
		 * 读取指定文本的所有行内容到content列表下(以行方式读取)
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
		 * 复写之前读取的所有文本内容，在写的过程中检查是否是指定参数，如果是，则进行相应值的修改
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
  