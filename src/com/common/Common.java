package com.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Common {
	//开发/运行模式
	public static final int RUNMODE = 1;		//0-开发模式; 1-运行模式
	/**继电器控制**/
	public static final String CODE_SET_RELAY = "001";
	/**获取土壤数据**/
	public static final String CODE_GET_SOIL_MOISTURE = "002";
	/**获取人体活动传感数据**/
	public static final String CODE_GET_HUMAN = "003";
	/**LED控制**/
	public static final String CODE_SET_LED = "004";
	/**舵机控制**/
	public static final String CODE_SET_SERVO = "005";

	

	/**
	 * 计算2点间的直线距离(单位为米), 忽略地球是圆的问题,也忽略地理上的障碍物因素
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double convertToMeter(double lat1, double lng1, double lat2, double lng2){
		double avgMeter = 40000/360;
		lat1 = (lat1*avgMeter)*1000;	//换算成米
		lng1 = (lng1*avgMeter)*1000;
		lat2 = (lat2*avgMeter)*1000;	//换算成米
		lng2 = (lng2*avgMeter)*1000;
		double ab = Math.abs(lat1-lat2);
		double ac = Math.abs(lng1-lng2);
		//System.out.println(">ab:"+ab);System.out.println(">ac:"+ac);
		double bcPF = ab*ab + ac*ac;		//bc的平方
		//System.out.println(">bc的平方:"+bcPF);
		double bc = Math.sqrt(bcPF);
		//System.out.println(">bc的平方开根号:"+bc);
		bc = Math.round(bc);
		System.out.println(">bc单位换算成米,并去掉小数:"+bc);
		
		return bc;
	}
	
	
	/**
     * 提供精确的小数位四舍五入处理。
     * @param v 需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
	public static double round(double v,int scale){
		if(scale<0){throw new IllegalArgumentException("The scale must be a positive integer or zero");}
		BigDecimal b = new BigDecimal(Double.toString(v));
		BigDecimal one = new BigDecimal("1");
		return b.divide(one,scale,BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	/**
	 * Re-maps a number from one range to another. That is, a value of fromLow would get mapped to toLow, a value of fromHigh to toHigh, values in-between to values in-between, etc.
	 * @param x
	 * @param in_min
	 * @param in_max
	 * @param out_min
	 * @param out_max
	 * @return
	 */
	public static long map(long x, long in_min, long in_max, long out_min, long out_max){
		  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
	
    /**
     * 执行简单命令
     * @param cmd	命令
     * @param tp		1-只执行不返回结果  0-执行并返回结果
     * @return
     * 			if tp==1    执行成功返回1，失败返回－1
     * 			if tp==0	   返回执行（输出）结果
     */
    public static String runCommand(String cmd,int tp){
    	StringBuffer buf = new StringBuffer(1000);
    	String rt="-1";
    	try{
    		Process pos = Runtime.getRuntime().exec(cmd);
    		pos.waitFor();
    		if(tp==1){
    			if(pos.exitValue()==0){
    				rt="1";
    			}
    		}else{
    			InputStreamReader ir = new InputStreamReader(pos.getInputStream());
    		    LineNumberReader input = new LineNumberReader(ir);
    		    String ln="";
    		    while ((ln =input.readLine()) != null) {
    		        buf.append(ln+"<br>");
    		    }
    		    rt = buf.toString();
    		    input.close();
    		    ir.close();
    		}
    	}catch(Exception e){
    		rt=e.toString();
    	}
    	return rt;
    }
    
	/**
	 * json输出工具方法
	 * @param o 要转成json的对象
	 */
	public static String renderJSON(Object o){
//		JsonConfig jsonConfig = new JsonConfig();   
//		jsonConfig.registerJsonValueProcessor(Date.class, new JsonProcessor("yyyy-MM-dd HH:mm"));
//		//jsonConfig.registerJsonValueProcessor(User.class, new JsonProcessor());
//		JSONObject jsonObject = JSONObject.fromObject(o,jsonConfig);
//		return jsonObject.toString();
		return null;
	}
	
	/**
	 * 判断字串是否数字
	 * @param str		目标字串
	 * @return			返回布尔值
	 */
	public static boolean isNumeric(String str){
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if( !isNum.matches()){
			return false;
		}
		return true;
	}
	/**
	 * 写入日志文件(文件已存在则追加)
	 * @param logs 日志内容
	 * @param logfile	日志文件(含物理路径)
	 */
	public static void recordLogs(StringBuffer logs, String logfile){
		RandomAccessFile bw = null;
		try {
			File f = new File(logfile);
			if(f.exists()){
				bw = new RandomAccessFile(logfile,"rw");
				
		        if(f.length()>5242880){		//日志文件大于5MB时,删除旧的重新创建
		        	f.delete();
		        	bw = new RandomAccessFile(f, "rw");
		        }else bw.seek(bw.length());	//将指针移动到文件末尾
			}else{
				bw = new RandomAccessFile(f, "rw");
			}
			String strGBK = logs.toString();
			byte[] strBytes = strGBK.getBytes("gbk");
			bw.write(strBytes);
		} catch (IOException es) {
			System.out.print("IO读写异常!");
		} finally {
			try {
				if(bw!=null)bw.close();
			} catch (IOException e) {
				System.out.print("资源回收失败!");
			}
		}
	}
	/**
	 * 写入日志文件(文件已存在则追加)
	 * @param logs 日志内容
	 * @param logfile	日志文件(含物理路径)
	 */
	public static void recordLogs(StringBuffer logs, String logfile,boolean isReplace){
		RandomAccessFile bw = null;
		try {
			File f = new File(logfile);
			if(f.exists()){
				bw = new RandomAccessFile(logfile,"rw");
				
		        if(isReplace){		//删除旧的重新创建
		        	f.delete();
		        	bw = new RandomAccessFile(f, "rw");
		        }else bw.seek(bw.length());	//将指针移动到文件末尾
			}else{
				bw = new RandomAccessFile(f, "rw");
			}
			String strGBK = logs.toString();
			byte[] strBytes = strGBK.getBytes("gbk");
			bw.write(strBytes);
		} catch (IOException es) {
			System.out.print("IO读写异常!");
		} finally {
			try {
				if(bw!=null)bw.close();
			} catch (IOException e) {
				System.out.print("资源回收失败!");
			}
		}
	}
	

  	/**
	 * 根据配置文件路径返回配置文件对象
	 * 通过 properties.getProperty(key) 或 properties.getProperty(key, defaultValue) 取值
	 * @param urlStr
	 * @return
	 */
	public static Properties getProperties(String urlStr){
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try {
			InputStream in = classLoader.getResource(urlStr).openStream();
			Properties properties = new Properties();
			properties.load(in);
			return properties;
		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }
	
	/**
	 * 把Integer型取反后返回结果
	 * @param x		0~255
	 *  int rr = Integer.valueOf("10000000",2);
	 * @return
	 */
	public static int IntReverse(int x){
		String str=Integer.toBinaryString(~x);			//将给定的数取反转为二进制字串
		String strx=str.substring(str.length()-8);		//将取反后的二进制是32位, 只需要取末8位即可
		int res=Integer.parseInt(strx, 2);				//将它转换为十进制整数
		return res;
	}

}
