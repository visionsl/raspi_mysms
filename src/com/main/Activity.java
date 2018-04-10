package com.main;

import java.util.Date;
import java.util.Map;

import org.bson.Document;

import redis.clients.jedis.Jedis;

import com.common.DateTimeUtil;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * 在Linux(树莓派3)中执行:
 * 记录和分析环境中人的活动情况
 * 
 * 执行: 在目录/home/pi/link中:
 *   	java com.main.Activity
 * 依赖:
 * 		pi4j
 * 		Redis
 * 		MongoDB
 * 
 * 硬件:
 * 		- GPIO1 人体热感(预留)
 *   
 * 功能点:
 * 		- 记录每小时人活动的情况(触发传感器的次数)
 * 
 * @author S.L
 *
 */
public class Activity {
	//Mongodb
	static MongoClient _mongoClient;
	static MongoDatabase _database;
	static MongoCollection<Document> _collection;
	//Redis
	public  static String _REDISSERVER;
	static Jedis _jedis;
	final static String _redis_human_key = "Activity_human";
	
	static GpioPinDigitalInput _human = null;				//传感器GPIO
	static String _nhour = null;							//每小时一个键值
	static boolean _haveHuman = false;						//是否有人活动

	static {
		//静态初始器仅仅在类装载的时候（第一次使用类的时候）执行一次，往往用来初始化静态变量
		_REDISSERVER = "localhost";
		_jedis = new Jedis(_REDISSERVER);
		_mongoClient = new MongoClient( "127.0.0.1" , 27017 );
		_database = _mongoClient.getDatabase("mydb");
		_collection = _database.getCollection("human");
	}

	public static void main(String[] args) {
		System.out.println("Activity is running...");

		/**监听IO输入**/
		final GpioController gpio = GpioFactory.getInstance();
		listenHW(gpio);

		String preDay = DateTimeUtil.dateToString(new Date(), "MMdd");
		String preHour = DateTimeUtil.dateToString(new Date(), "MMdd_HH");
		int nCount = 0;
		while(true){
			String nday = DateTimeUtil.dateToString(new Date(), "MMdd");
			_nhour = DateTimeUtil.dateToString(new Date(), "MMdd_HH");
			//System.out.println("redis._nhour:"+_nhour);

			Map<String, String> hmapget = _jedis.hgetAll(_redis_human_key);
			if(_haveHuman){
				nCount = (hmapget.size()<1 || null==hmapget.get(_nhour))?1:Integer.parseInt(hmapget.get(_nhour))+1;		//无Redis.Key时设置默认值"1"
				hmapget.put(_nhour, String.valueOf(nCount));
				_jedis.hmset(_redis_human_key, hmapget);
				_haveHuman = false;
				System.out.println("redis.hmapget:"+hmapget.toString());
			}
			
			//每小时保存一次数据
			//saveToDB(hmapget);
			if(!preHour.equals(_nhour)){
				saveToDB(hmapget);
				preHour = _nhour;
			}
			//如果进入第2天, 则清空旧数据
			if(!preDay.equals(nday)){
				_jedis.del(_redis_human_key);
				preDay = nday;
			}

			try {Thread.sleep(1000);} catch (InterruptedException e1) {e1.printStackTrace();}
		}

	}

	//传感器监听器
	public static void listenHW(GpioController gpio){

        //--用于人体红外感应(高电平-有物体靠近;  低电平-检测不到物体)
		_human = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
		_human.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
            	System.out.println(new Date()+" .--> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
                if(event.getState().isHigh()){
                	_haveHuman = true;
                }else{ 
                }
            }
        });
	}
	
	/**
	 * 
	 * @param hmapget	{0409_08=4, 0409_09=6}
	 */
	public static void saveToDB(Map<String, String> hmapget){
		String curyyyy = DateTimeUtil.dateToString(new Date(), "yyyy");		//当年， 2018
		//String curdate = DateTimeUtil.dateToString(new Date(), "yyyyMMdd");		//当天， 20180409
		for (Map.Entry<String, String> entry : hmapget.entrySet()) {
			//遍历收集到的数据
			//System.out.println("saveToDB.Key = " + entry.getKey() + ", Value = " + entry.getValue());
			String nhour = entry.getKey();		//MMdd
			if(null==nhour)continue;
			
			String nMMdd = nhour.split("_")[0];	//MMdd_HH -> MMdd
			String nyyyMMdd = curyyyy+nMMdd;
			nhour = nhour.split("_")[1];		//MMdd_HH -> HH
			int nCount = Integer.parseInt(entry.getValue());
			
			Document strWhere = new Document("ndate",nyyyMMdd).append("nhour", nhour);
			System.out.println("saveToDB.strWhere = " + strWhere.toString());			//Document{{ndate=20180409, nhour=09}}
	    	FindIterable<Document> iterable = _collection.find(strWhere).limit(1);
			//FindIterable<Document> iterable = _collection.find(and(eq(), eq())).limit(1);
	    	
	    	MongoCursor<Document> cursor = iterable.iterator();
	    	//System.out.println("saveToDB.cursor.hasNext() = " + cursor.hasNext());
	    	if(cursor.hasNext()) {
	    		Document human = (Document)cursor.next();
	    		System.out.println("update old data:"+human.toString());
	    		//System.out.println(human.get("_id")+">>"+human.get("ndate")+"."+human.get("nhour"));
	    		//System.out.println(human.toString());
	    		//update data					new Document("ndate",ndate).append("nhour", _nhour),
	    		_collection.updateOne(
	    				Filters.eq("_id", human.get("_id")),
	    				new Document("$set",new Document("count", nCount))
	    				);
	    	}else{
	    		//指定日期无数据
	    		System.out.println("insert new data");	    		
	        	Document doc = new Document("ndate", nyyyMMdd)
		            .append("nhour", nhour)
		            .append("count", nCount)
		            .append("createdate", new Date());
	        	_collection.insertOne(doc);
	    	}
	    	cursor.close();
		}
	}
}
