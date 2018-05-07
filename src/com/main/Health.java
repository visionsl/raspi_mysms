package com.main;

import com.common.FileUtil;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * 设备健康管理进程
 *
     分类:
     - disk:     磁盘空间
     - memory:   内存情况
     - service:  服务状态(live/die/error)

     区域:
     - office2005: 办公室_2005
     - office2008: 办公室_2008
     - aliyun:      阿里云


     分类名+设备ID
     Health_00000000e87df053

     Key的实际结构:
     ==========================
     分类.区域.设备ID
     disk_office2005_00000000e87df053
 *      > 收集指定目录下的df磁盘占用情况的数据,
 */
public class Health {
    //Redis
    static Jedis _jedis;
    final static String _redis_Health_df_key = "disk_office2005";
    private static int _info_df_maxv = 20;  //磁盘占用超过x%后提交上传
    private String _info_df_fp;             //df信息文件路径, 在第1个参数中指定 args[0]
    private static String _cpu_serial;             //CPU序列号

    static {
        //静态初始器仅仅在类装载的时候（第一次使用类的时候）执行一次，往往用来初始化静态变量
        //_jedis = new Jedis("192.168.6.3");
        _cpu_serial = getCpuserial();
    }
    void Health(){

    }

    public static void main(String args[]) {

        Health health = new Health();
        if (args.length >= 4) {
            System.out.println("args[3]:"+args[3]);
            health._info_df_fp = args[3];

            _jedis = new Jedis(args[0], Integer.parseInt(args[1]));
            _jedis.auth(args[2]);
            System.out.println("connect to linkcloudtech.com..."+_jedis.isConnected());
        }else{
            System.out.println("请输入正确的参数： [redis服务器IP][redis服务器端口][redis服务器密码][df信息文件路径]");
            return ;
        }

        while(true) {

            //扫描DF信息文件
            scan_info_df(health);


            try {Thread.sleep(60000);} catch (InterruptedException e1) {e1.printStackTrace();}
        }

    }

    private static String getCpuserial(){
        /**读取CPU序列号**/
        String fn = "/proc/cpuinfo";
        String fc = FileUtil.getFileContent(fn);
        if(fc==null){
            System.out.println("can't found :"+fn);
            return null;
        }
        if(!fc.trim().equals("")){
            fc = fc.substring(fc.indexOf("Serial"));
            fc = fc.substring(fc.indexOf(":")+1);
            if(fc!=null){
                System.out.println(fc.trim());
                return fc.trim();
            }
        }else{
            //读取CPU序列号失败, 直接退出
            System.out.println("读取CPU序列号失败");
        }
        return null;
    }

    /*
        扫描DF信息文件(该文件由系统定时自动生成/更新)
        把磁盘占用大于指定百分比的数据上传到Redis服务器(有效存留时间是1小时)
     */
    private static void scan_info_df(Health health) {
        try {
            String rediskey = _redis_Health_df_key+"_"+_cpu_serial;
            FileReader fr = new FileReader(health._info_df_fp);
            BufferedReader br = new BufferedReader(fr);
            String str2 = br.readLine();        //第一行是标题, 没有用处, 读出后不处理
            while (str2 != null) {
                str2 = br.readLine();
                //System.out.println(str2);
                if(null == str2 || str2.trim().equals(""))continue;
                String[] vs6 = new String[6]; int x = 0;
                for(String v : str2.split(" ")){
                    if(null!=v && !v.trim().equals("")) {       //忽略空数据, 留下有效的数据: /dev/root      13956968 11311680 1913256   82% /
                        vs6[x] = v.trim();x++;
                    }
                }
                //for(String v : vs6){System.out.print(v.trim()); System.out.print("\t"); }
                if(null == vs6[4]){System.out.println("vs6[4]:"+vs6[4]);return;}
                int pre = Integer.parseInt(vs6[4].substring(0, vs6[4].indexOf("%")));
                if(pre >= Health._info_df_maxv){
                    //此处拿到有用的数据:vs6[5]-挂载点 ; vs6[4]-已用%
                    //System.out.print(vs6[5]);System.out.print("\t");System.out.print(vs6[4]);
                    Map<String, String> hmapget = _jedis.hgetAll(rediskey);
                    int inits = hmapget.size();
                    hmapget.put(vs6[5].replace("/", "__"), String.valueOf(pre));

                    Iterator<Map.Entry<String, String>> it = hmapget.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> doc = it.next();
                        System.out.println(doc.getKey() + ":" + doc.getValue());
                        if(Integer.parseInt(doc.getValue())<Health._info_df_maxv){
                            it.remove();
                        }
                    }

                    if(inits>hmapget.size()) {
                        System.out.println("redis.hmapget: 变小, 移除旧redis, 重新写入");
                        _jedis.del(rediskey);
                    }
                    _jedis.hmset(rediskey, hmapget);
                    int ot = 60 * 60;      //过期时间: 1小时
                    _jedis.expire(rediskey, ot);
                    System.out.println("redis.hmapget:"+hmapget.toString());
                }

            }

            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
