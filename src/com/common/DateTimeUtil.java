package com.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期操作类
 * @author 易勇
 *
 */
public class DateTimeUtil {
	public static void main(String args[]){
		System.out.println("asdf");
	}
	/**
	 * 将日期类型按指定格式转换成String型
	 * @param date
	 * @param pattern	"EEEE": 返回 星期几<br/>
	 * "yyyy-MM-dd HH:mm:ss.mmm"
	 * @return
	 */
	public static String dateToString(Date date,String pattern){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern,Locale.CHINA);
		return simpleDateFormat.format(date);
	}
	/**
	 * 将String类型的日期按指定格式转换成Date型
	 * @param strDate
	 * @param pattern	此处格式必须和strDate相对应
	 * @return
	 */
	public static Date stringToDate(String strDate,String pattern){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		try{
			return simpleDateFormat.parse(strDate);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 将String类型的日期按指定格式转换成String型
	 * @param date		要转的日期
	 * @param datePattern	日期对应的格式
	 * @param pattern	转换后的格式
	 * @return
	 */
	public static String stringToString(String date,String datePattern,String pattern){
		if(date==null||date.equals("")) return null;
		Date d=stringToDate(date,datePattern);
		if(d==null) return null;
		return dateToString(d,pattern);
	}
	/**
	 * 获取当前日期,Date型
	 * @param pattern	指定日期格式
	 * @return
	 */
	public static Date nowDate(String pattern){
		String s = nowString(pattern);
		return stringToDate(s,pattern);
	}
	/**
	 * 获取当前日期,String型
	 * @param pattern	"EEEE":返回星期几
	 * @return
	 */
	public static String nowString(String pattern){
		return dateToString(new Date(),pattern);
	}
	/**
	 * 获取与当前时间的距离，字符串表示
	 * 超过10天用日期表示： yyyy-MM-dd HH:mm
	 * @param d
	 * @return	 “**分钟前”，“**小时前”，“**天前”
	 */
	public static String getNowDiff(Date d){
		if(d==null)return "";
		Date now=new Date();
    	long diff=(now.getTime()-d.getTime())/1000/60;
    	String result="";
    	if(diff<60){
    		result = diff + "分钟前";
    	}else if(diff>=60 && diff<1440){
    		result=(diff/60)+"小时前";
    	}else if (diff>=1440 && diff<14400){
    		result=(diff/1440)+"天前";
    	}else{
    		result=dateToString(d,"yyyy-MM-dd HH:mm");
    	}
    	return result;
	}
	/**
	 * 获取与当前时间的距离，字符串表示
	 * 超过10天用日期表示： yyyy-MM-dd HH:mm
	 * @param d
	 * @return	 “**分钟前”，“**小时前”，“**天前”
	 */
	public static String getNowDiff(String d){
		if(d==null||d.equals(""))return "";
		Date now=new Date();
		Date small=stringToDate(d,"yyyy-MM-dd HH:mm");
    	long diff=(now.getTime()-small.getTime())/1000/60;
    	String result="";
    	if(diff<60){
    		result = diff + "分钟前";
    	}else if(diff>=60 && diff<1440){
    		result=(diff/60)+"小时前";
    	}else if (diff>=1440 && diff<14400){
    		result=(diff/1440)+"天前";
    	}else{
    		result=dateToString(small,"yyyy-MM-dd HH:mm");
    	}
    	return result;
	}
	/**
	 * 获取与当前时间的距离，字符串表示
	 * @param date
	 * @return	 “**天**时**分前”
	 */
	public static String getTimeBetweenByNow(Date date){
		if(date==null)return "";
		Date now=new Date();
    	long diff=(now.getTime()-date.getTime())/1000/60;
    	String result="";
    	if(diff<60){
    		result = diff + " 分钟前";
    	}else if(diff>=60 && diff<1440){
    		result=(diff/60)+" 时 "+(diff%60)+" 分钟前";
    	}else if (diff>=1440){
    		result=(diff/1440)+" 天 "+((diff%1440)/60)+" 时 "+((diff%1440)%60)+" 分钟前";
    	}
    	return result;
	}
	/**
     * 计算日期差，给出一个日期，再给出日期差（天数），得出相差的日期，比如日期：2008-5-2；日期差2；则结果是2008-5-4
     * @param date	日期
     * @param days	日期差
     * @return
     */
    public static Date dateDiff(Date date,int days){
    	int delay = (1000*3600*24)*days; //毫秒 
        Date result = new Date(date.getTime() - delay); 
        return result;
    }
	/**
	 * 得到两个日期间的间隔
	 * @param small
	 * @param big
	 * @param tp	'D':日，'H':时，'m':分，'s':秒
	 * @return
	 */
	public static long getDiff(Date small,Date big,char tp){
		if(small==null || big==null) return 0l;
		long diff=big.getTime()-small.getTime();
		if(tp=='s'){
			diff = diff/1000;
		}else if(tp=='m'){
			diff = diff/1000/60;
		}else if(tp=='H'){
			diff = diff/1000/60/60;
		}else if(tp=='D'){
			diff = diff/1000/60/60/24;
		}
		return diff;
	}
	/**
	 * 得到两个日期间的间隔
	 * @param small	（日期格式：yyyy-MM-dd HH:mm）
	 * @param big	（日期格式：yyyy-MM-dd HH:mm）
	 * @param tp	'D':日，'H':时，'m':分，'s':秒
	 * @return
	 */
	public static long getDiff(String small,String big,char tp){
		Date s=stringToDate(small,"yyyy-MM-dd HH:mm");
		Date b=stringToDate(big,"yyyy-MM-dd HH:mm");
		long diff=b.getTime()-s.getTime();
		if(tp=='s'){
			diff = diff/1000;
		}else if(tp=='m'){
			diff = diff/1000/60;
		}else if(tp=='H'){
			diff = diff/1000/60/60;
		}else if(tp=='D'){
			diff = diff/1000/60/60/24;
		}
		return diff;
	}
	/**
	 * 修改日期
	 * @param date	要修改的日期
	 * @param tp	要修改的类型  
	 * 'Y':年，'M':月，'D':日，'H':时，'m':分，'s':秒
	 * @param value	要修改的值 可取负数：在原基础上减去该值
	 * @return
	 */
	public static Date dateModified(Date date,char tp,int value){
		Calendar cal=Calendar.getInstance();
		cal.setTime(date);
		if(tp=='Y')
			cal.add(Calendar.YEAR, value);
		else if(tp=='M')
			cal.add(Calendar.MONTH, value);
		else if(tp=='D')
			cal.add(Calendar.DATE, value);
		else if(tp=='H')
			cal.add(Calendar.HOUR, value);
		else if(tp=='m')
			cal.add(Calendar.MINUTE, value);
		else if(tp=='s')
			cal.add(Calendar.SECOND, value);
		return cal.getTime();
	}
	/**
	 * 修改日期
	 * @param date	要修改的日期 （日期格式：yyyy-MM-dd HH:mm:ss）
	 * @param tp	要修改的类型  
	 * 'Y':年，'M':月，'D':日，'H':时，'m':分，'s':秒
	 * @param value	要修改的值 可取负数：在原基础上减去该值
	 * @param pattern	返回的字符串格式
	 * @return
	 */
	public static String dateModified(String date,char tp,int value,String pattern){
		Calendar cal=Calendar.getInstance();
		cal.setTime(stringToDate(date,"yyyy-MM-dd HH:mm:ss"));
		if(tp=='Y')
			cal.add(Calendar.YEAR, value);
		else if(tp=='M')
			cal.add(Calendar.MONTH, value);
		else if(tp=='D')
			cal.add(Calendar.DATE, value);
		else if(tp=='H')
			cal.add(Calendar.HOUR, value);
		else if(tp=='m')
			cal.add(Calendar.MINUTE, value);
		else if(tp=='s')
			cal.add(Calendar.SECOND, value);
		return dateToString(cal.getTime(),pattern);
	}
	/**
	 * 根据一个日期，返回该日期是星期几的字符串
	 * @param date
	 * @return	星期一。。。
	 */
	public static String getWeek(Date date){
		return dateToString(date,"EEEE");
	}
	/**
	 * 根据一个日期，返回该日期是星期几的字符串
	 * @param date
	 * @return
	 */
	public static String getWeek(String date){
		return dateToString(stringToDate(date,"yyyy-MM-dd"),"EEEE");
	}
	/**
	 * 判断是否润年
	 * @param year
	 * @return
	 */
	public static boolean isLeapYear(int year) { 
		boolean leapYear = false;   
    	if(year%4 == 0){   
    		if(year%100 != 0)   
    			leapYear = true;   
    		else if(year%400 == 0)   
    			leapYear = true;   
    	}   
    	return leapYear;
	}
	/**
	 * 检查check是否在start-end范围内，与起始时间相等也为在范围内
	 * @param check	要检查的日期
	 * @param start	起始日期
	 * @param end	终止日期
	 * @return
	 */
	public static boolean isContain(Date check,Date start,Date end){
		if(check.after(start) && check.before(end)){
    		return true;
    	}else{
    		if(check.compareTo(start)==0) return true;
    		if(check.compareTo(end)==0) return true;
    		return false;
    	}
	}
	/**
	 * 获取指定日期当年的最后一天
	 * @param date 
	 * @return
	 */
	public static Date lastDayByYear(String date){
		String y=stringToString(date, "yyyy-MM-dd", "yyyy");
        Calendar ca = Calendar.getInstance();    
        ca.setTime(stringToDate(y+"-12-31","yyyy-MM-dd"));
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));  
        return ca.getTime();
	}
	/**
	 * 获取指定日期当年的第一天
	 * @param date 
	 * @return
	 */
	public static Date firstDayByYear(String date){
		String y=stringToString(date, "yyyy-MM-dd", "yyyy");
        Calendar ca = Calendar.getInstance();    
        ca.setTime(stringToDate(y+"-01-01","yyyy-MM-dd"));
        //ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));  
        return ca.getTime();
	}
	
	 /**
	 * 解析Abacus格式的日期
	 * @param timeStr	01APR12-31DEC12
	 * @return 返回日期型数组
	 */
	public static Date[] getValidStatrtTime(String timeStr){
		if(timeStr==null || timeStr.equals(""))return null;
		Date[] rdate = new Date[2];
		if(timeStr.indexOf("---")!=-1){
			Calendar calendar=Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.add(Calendar.MONTH,-1);
			rdate[0] = calendar.getTime();
			calendar.add(Calendar.YEAR, 1);
			rdate[1] = calendar.getTime();
			return rdate;
		}
		
		String[] timeArray=timeStr.split("-");
		if(timeArray==null||timeArray.length<1)return null;
		rdate[0] = getValidDate(timeArray[0].trim(),false);
		if(timeArray.length>1){
			rdate[1] = getValidDate(timeArray[1].trim(),true);
		}
		return rdate;
	}
	/**
	 * 通过：02FEB12解析成日期
	 * @param dateStr	02FEB12
	 * @param isAdd		解析出来的日期是否加1年
	 * @return
	 */
	public static Date getValidDate(String dateStr,boolean isAdd){
		if(dateStr==null || dateStr.equals(""))return null;
		String ts = dateStr.substring(0,2);
		if(!Common.isNumeric(ts))dateStr=dateStr.substring(2);		//如果发现前2位不是数字(例:MO29JUL),则剪除之
		String[] dateString={"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
		Calendar nowDate=Calendar.getInstance();
		nowDate.setTime(new Date());
		int mon=0;
		String day="01";
		int year=nowDate.get(Calendar.YEAR);
		for(int i=0;i<dateString.length;i++){
			if(dateStr.indexOf(dateString[i])!=-1){
				day=dateStr.substring(0,dateStr.indexOf(dateString[i]));
				mon=i;break;
			}
		}
		if(mon<nowDate.get(Calendar.MONTH)&&isAdd){
			year++;
		}
		nowDate.set(Calendar.YEAR, year);
		nowDate.set(Calendar.MONTH,mon);
		int dateDay=Integer.parseInt(day);
		nowDate.set(Calendar.DAY_OF_MONTH,dateDay);
		return nowDate.getTime();
	}

}
