package com.SIM8xx;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.serial.Serial;

import java.io.Console;
import java.io.IOException;

public class Tools {

    /**
     * 延时函数
     * @param second    秒
     */
    private static void delay(long second){
        try {
            for(long x=1; x<=second; x++) {
                System.out.print(".");
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从特定的字串中获取右侧的值
     * @param res       带":"的字符串,支持多行, example: +CSQ: 24,99
     * @return          上例中返回24,99
     */
    public static String getValueFromString(String res) {
        String signal = null;
        String[] vs = res.split("\r\n");
        for (String v : vs) {
            if (v.contains(":")) {
                signal = v.substring(v.indexOf(":") + 1);
                //System.out.print("... catch value:[" + signal.trim() + "]\r\n");
            }
        }
        return signal;
    }

    public static void rebootSIMPower(GpioPinDigitalOutput sim868_en){
        //超过10次都不成功, 则断电重来
        System.out.print("模块断电中...");
        sim868_en.setState(PinState.LOW);
        delay(5);
        sim868_en.setState(PinState.HIGH);
    }

    /**
     * 执行指令, 并在限定时间内等待结果(并加上预期的结尾字串判断)
     * @param serial
     * @param cmd           执行的指令
     * @param endRes        期待的正确结果
     * @param maxWaitSecond 最大等待时间(秒)
     * @return
     */
    public static String cmdAndWaitResult(Serial serial, String cmd, String endRes, long maxWaitSecond){
        long timecount = 0;
        System.out.println("cmdAndWaitResult.cmd:"+cmd);
        String res = new String(sendCMD(serial, cmd));
        while(timecount < maxWaitSecond) {
            if(res.toUpperCase().contains(endRes.toUpperCase())){
                //System.out.println("cmdAndWaitResult.找到期待结果:"+res);
                res += new String(listenSerial(serial));
                break;
            }
            res += new String(listenSerial(serial));
            timecount++;delay(1);
        }
        return res;
    }

    private static void checkGPRSStatus(Serial serial){
        //res = new String(Tools.sendCMD(serial, "AT+CIPSTATUS"));
        String res = cmdAndWaitResult(serial, "AT+CIPSTATUS", "STATE", 5);
        if(!res.contains("INITIAL")){
            res = cmdAndWaitResult(serial, "AT+CIPSHUT", "SHUT", 5);
            if(!res.contains("OK")){
                //调用失败
                gprsTest(serial);
            }
        }
    }

    public static void gprsTest(Serial serial){
        //准备网络条件
        String res = new String(Tools.sendCMD(serial, "AT+CIPSHUT"));   System.out.println(res);
        res = new String(Tools.sendCMD(serial, "AT+CSTT"));             System.out.println(res);
        res = new String(Tools.sendCMD(serial, "AT+CIICR"));            System.out.println(res);
        //此处只要能拿到本机IP, 后面的"CIPSTART"基本上可以100%成功
        res = new String(Tools.sendCMD(serial, "AT+CIFSR"));            System.out.println(res);        //返回10.121.176.158 或 ERROR
        //res = new String(Tools.sendCMD(serial, "AT+CIPATS=1,5"));       System.out.println(res);          //设置每5秒自动发送(未成功)


        while(true){
            res = cmdAndWaitResult(serial, "AT+CIPSTART=TCP,116.62.192.119,4002", "CONNECT", 10);
            System.out.println(res);
            if(res.contains("FAIL")){
                //检查状态
                checkGPRSStatus(serial);
                continue;
            }

            if (res.contains("CONNECT OK")) {
                System.out.println("连接成功!");
                res = cmdAndWaitResult(serial, "AT+CIPSEND", ">", 10);
                if (res.contains(">")) {
                    String v = "aaaaa";
                    res = cmdAndWaitResult(serial, v, v, 5);
                    if (res.contains(v)) {
                        try {
                            serial.write((byte) 0x1a);
                            res = cmdAndWaitResult(serial, "\r", "SEND OK", 5);
                            System.out.println(res);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            delay(1);
        }



/*
        res = new String(Tools.sendCMD(serial, "AT+CIPSTART=TCP,116.62.192.119,4002"));
        System.out.println(res);

        boolean isconnect = false;
        if(res!=null && !res.trim().equals("")){
            //if(res.trim().equals("ERROR")){
            //    checkSelf(serial);
            //    gprsTest(serial);
            //}
            while (timecount < 10000) {
                String s = new String(listenSerial(serial));
                System.out.println("检查连接情况...("+timecount+"):"+s);
                if(s.contains("CONNECT OK")){
                    System.out.println("连接成功");
                    isconnect = true; break;
                }
                timecount+=1000;
            }
        }

        if(!isconnect) {
            delay(6);
            checkSelf(serial);
            gprsTest(serial);
        }
        res = new String(Tools.sendCMD(serial, "AT+CIPSEND"));
        System.out.println(res);
        boolean readysend = false;
        long timecount = 0;
        while (timecount < 10000) {     //等待输入符">"出现
            if (res.contains(">")) {
                readysend = true;
            }
            timecount+=1000;
        }

        //循环发送数据
        while(readysend){

            res = new String(Tools.sendCMD(serial, "AT+CIPSEND"));
            System.out.println(res);
            timecount = 0;
            while (timecount < 10000) {     //等待输入符">"出现
                if (res.contains("ERROR")) {
                    res = new String(Tools.sendCMD(serial, "AT+CIPSTATUS"));
                    System.out.println(res);
                }
                if (res.contains(">")) {
                    break;
                }
                timecount+=1000;
            }


            System.out.print("send data:"+String.valueOf(i));
            //Tools.sendCMD(serial, String.valueOf(i)+"\r\n");
            try {
                //serial.writeln(String.valueOf(i) + "\r\n");
                serial.write(String.valueOf(i)+"\r");
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                System.out.println("listen serial:"+new String(listenSerial(serial)));

                //delay(1);
                serial.write((byte) 0x1a);
                System.out.println("...0x1A");
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                System.out.println("listen serial:"+new String(listenSerial(serial)));
                //res = new String(Tools.sendCMD(serial, "AT+CIPACK"));             System.out.println(res);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            delay(5);
        }
        System.out.println("SIM868无回应，或回应错误");
        res = new String(Tools.sendCMD(serial, "AT+CIPSHUT"));
        System.out.println(res);
        checkSelf(serial);
        gprsTest(serial);
*/
    }

    /**
     * 检查是否有信号, 以及网络注册情况
     * @param serial
     * @return
     */
    public static boolean checkSelf(Serial serial){
        boolean iswork = false;
        /**
         * 检查信号强度
         * +CSQ: 24,99
         */
        String res = new String(Tools.sendCMD(serial, "AT+CSQ"));    //发送读取短信的指令
        if (res.indexOf("ERROR") != -1) {
            System.out.println("Signal quality test ... failed");
        }else {
            try {
                int ig = Integer.parseInt(Tools.getValueFromString(res).split(",")[0].trim());
                System.out.println("Signal quality: " + ig);
                if(ig>0){iswork = true;}
            }catch(Exception e){
                System.out.println("Signal quality: " + Tools.getValueFromString(res));
                e.printStackTrace();
            }
        }
        if(!iswork){return iswork;}

        /**
         * 检查网络注册情况
         * +CREG: 0,1
         */
        res = new String(Tools.sendCMD(serial, "AT+CREG?"));
        //System.out.print("Register to network:");
        try {
            int ig = Integer.parseInt(Tools.getValueFromString(res).split(",")[1].trim());
            System.out.println("Register to network: "+ig);
            if(ig==1){iswork = true;}
            else{iswork = false;}
        }catch(Exception e){
            System.out.println("Signal quality  ... " + Tools.getValueFromString(res));
            e.printStackTrace();
            iswork = false;
        }
        return iswork;
    }





    /**
     * ***短信发送模式***
     * @param cs        Console
     * @param serial
     * @throws IOException
     */
    public static void send_sms_mode(Console cs, Serial serial) throws IOException {
        while(true) {
            System.out.print("手机号码：");
            String rcs = cs.readLine();
            String m = (rcs==null || rcs.equals(""))?"18620671820":rcs;
            System.out.print("短信内容：");
            rcs = cs.readLine();
            String c = (rcs==null || rcs.equals(""))?"this msg is from raspberry":rcs;
            String res = new String(Tools.sendCMD(serial, "AT+CMGS=\"" + m + "\""));
            System.out.println(res);
            if (res.contains(">")) {
                new String(Tools.sendCMD(serial, c));
                serial.write((byte) 0x1a);
            } else {
                System.out.println("SIM868无回应，或回应错误");
            }
        }
    }

    public static void listen_SMS(Serial serial) throws InterruptedException, IOException {
        int index = 1;
        byte[] data = new byte[1024]; // 数据缓冲区
        while (true) {
            System.out.print(".");
            if (!serial.isOpen()) {
                System.out.println("Serial port is not open, exit the program!");
                break;
            }
            if (serial.available() > 0) {
                while (serial.available() > 0) {
                    data = serial.read(); // 此处接收到的数据上限是1024
                    // System.out.print(new String(serial.read(), "utf-8"));
                }
                serial.flush();
            }
            if (data != null) {
                // 接收到数据
                String cc = new String(data, "GBK"); // 处理中文
                if (cc != null && !cc.trim().equals("")) {
                    // 处理数据
                    System.out.println("from SIM868's data:" + cc);

                    /*
                     * 有新短信时(无来电功能时): +CIEV: "MESSAGE",1
                     * +CMTI: "SM",1
                     */
                    // 如果有+CMTI这个字符串,发送该内存的位置
                    if (cc.contains("+CMTI")) {
                        index = getIndexFromNewSMS(cc);               //解析出索引位置
                        System.out.println("Find new SMS.index:" + index);
                        Tools.sendCMD(serial, "AT+CMGR=" + index);    //发送读取短信的指令
                    }
                    // 如果有+CMGR这个字符串,读取并发送短信内容到服务器后,删除该短信
                    // ************************如果没有发送成功(服务器或设备宕机),因为sendDataToServer发送后总是return true,意味着会删除*****************
                    if (cc.contains("+CMGR")) {
                        // 从内存位置index开始读取短信内容
                        String[] contents = getContentFromIndex(index, cc);
                        System.out.println("[AT+CMGR=index]Read the contents of the message on the card. After analysis:");
                        if (contents != null) {
                            System.out.println("New SMS content:");
                            for (String tt : contents) {
                                System.out.println(tt);
                            }

                            /*
                             * 保存读到的短信 -> 服务器
                             if (sendDataToServer(contents)) {
                             // 删除已读出的短信
                             System.out.println("Delete the new SMS has been read.index:" + contents[0]);

                             // 保存短信到本地mongodb服务器
                             if(MongoForSMS.save(mongoClient,DATABASE, COLLECTION, contents)){
                             System.out.println("SMS saved");
                             }else{
                             System.out.println("SMS saved unsuccessful");
                             }

                             // 从短信位置索引删除该短信
                             delSMSByIndex(serial, Integer.parseInt(contents[0]));
                             }
                             */
                            // 从短信位置索引删除该短信
                            delSMSByIndex(serial, Integer.parseInt(contents[0]));
                        } else {
                            System.out.println("new SMS content:null");
                        }
                    }
                }
            }

            data = null;
            Thread.sleep(1000);
        }
    }

    /**
     * 有新短信时,获取短信内容: +CIEV: "MESSAGE",1
     *
     * +CMTI: "SM",1
     * +CMTI: "SM",6
     *
     * @return index 短信所在的内存位置 index
     */
    public static int getIndexFromNewSMS(String cc) {
        try {
            String[] ccs = cc.split("\r\n");
            for (String v : ccs) {
                if (v.contains("CMTI: \"SM\",")) {
                    String c = v.substring(v.indexOf(",") + 1);
                    return Integer.parseInt(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 删除指定位置上的短信 AT+CMGD=4
     *
     * @param index
     *            短信索引位置
     */
    public static void delSMSByIndex(Serial serial, int index) {
        String res = new String(Tools.sendCMD(serial, "AT+CMGD=" + index));
        System.out.println("AT+CMGD=" + index + ":" + res);
        // if(res.indexOf("OK")==-1){
        // System.out.println("删除["+index+"]位置的短信失败!");
        // return false;
        // }
    }

    /*
     * 解析返回的短信内容
     *
     * @return
     */
    // data是从SIM卡读到的所有内容,cc是GBK格式的data字符串,传递到这个方法当做参数res
    public static String[] getContentFromIndex(int index, String res) {
        try {
            System.out.println("Try reading SMS...getContentFromIndex.res:" + res);
            // 原始数据data中包含'OK'的字符串
            if (res.contains("OK")) {
                System.out.println("Get SMS success, parse content ...");
                /*
                 * +CMGR:
                 * "REC READ","18620671820",,"2017/10/26,11:37:03+08",161,
                 * 17,0,0,"+8613010200500",145,25 just because the people11
                 *
                 * +CMGR:
                 * "REC READ","18620671820",,"2017/10/26,11:37:03+08",161,
                 * 17,0,0,"+8613010200500",145,25 ---------------- -------------
                 * - ---------- ----------- --- -- - - ---------------- --- --
                 * [0] [1] [2] [3] [4] [5] [6][7][8] [9] [10][11]
                 */

                // 一行一个数组元素
                String[] ccs = res.split("\r\n");
                String phone = new String();
                String sendDate = new String();
                String content = new String();
                boolean isvalid = false; // 数据获取成功

                // 将短信内容分割保存为数据
                for (int i = 0; i < ccs.length; i++) {
                    if (ccs[i].contains("CMGR:")) {
                        String[] temp1 = ccs[i].split(",");
                        phone = temp1[1];
                        sendDate = temp1[3] + " " + temp1[4];
                        content = ccs[i + 1];
                        isvalid = true;
                        break; // 只处理1条
                    }
                }
                if (!isvalid)
                    return null;
                // 处理双引号
                phone = phone.substring(1, phone.length() - 1);
                sendDate = sendDate.substring(1, sendDate.length() - 1);
                String[] resu = new String[4];
                resu[0] = String.valueOf(index);
                resu[1] = phone.trim();
                resu[2] = sendDate;
                resu[3] = content;
                return resu;

            } else if (res.contains("CMS ERROR")) {
                // CMS ERROR:321 表示所读取的内存位置出错,一般是指定位置无短信内容所致
                System.out.println("Get SMS failed, error content ...");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化SIM868
     */
    public static boolean initSIM868(Serial serial, GpioPinDigitalOutput sim868_en){
        int retry = 0;
        boolean isIG;
        do {
            /* 初始化GPRS模块 */
            isIG = Tools.initGPRS(serial);
            System.out.println("初始化GPRS模块:" + isIG);
            retry++;
            if (retry > 10) {
                //超过10次都不成功, 则断电重来
                rebootSIMPower(sim868_en);
                retry = 0;
            }
            if(!isIG)delay(5);
        } while (!isIG);
        return true;
    }

    /** 初始化短信参数 **/
    public static void initGPRS_SMS_pub(Serial serial, GpioPinDigitalOutput sim868_en) {
        long retry = 0;
        boolean isIG = false;
        while (!isIG) {
            retry++;
            if (retry > 10) {
                //超过10次都不成功, 则断电重来
                rebootSIMPower(sim868_en);
                initSIM868(serial, sim868_en);     //重头开始初始化
                retry = 0;
            }
            isIG = initGPRS_SMS(serial);
            System.out.println("初始化短信参数:" + isIG + " ...[" + retry + "]");
            if(!isIG)delay(5);
        }
    }
    /**
     *
     * 初始化GPRS.设置短信模式及短信接收参数 AT+CMGF=1 0-PDU, 1-文本格式 AT+CSDH=1
     * AT+CPMS="SM","SM","SM" 将信息保存在SIM卡中, SM-表示存在SIM卡上 AT+CNMI=2,1,0,1,1
     * 收接通知,并存在指定位置(与AT+CPMS设置有关)
     *
     * 设置好后, 收到短信: +CIEV: "MESSAGE",1 +CMTI: "SM",1 表示存储位置index=1
     *
     */
    public static boolean initGPRS_SMS(Serial serial) {
        if (!serial.isOpen()) {
            return false;
        } // 串口未准备好
        String res;
        try {
            System.out.print("\r\n[AT+CMGF?]检查短消息格式: ");
            res = new String(Tools.sendCMD(serial, "AT+CMGF?"));        //+CMGF: 1
            String v = (res.indexOf("OK") > -1)?getValueFromString(res):null;
            if(v==null || !v.trim().equals("1")) {
                System.out.print("\r\n[AT+CMGF=1]设置短消息格式: 0-PDU模式 / 1-文本模式 ...");
                res = new String(Tools.sendCMD(serial, "AT+CMGF=1"));
                if (!res.contains("OK")) {
                    System.out.println(" failed!");return false;
                }
                System.out.println("...[text format]\r\n");
            }
            Thread.sleep(100);

            //控制是否在文本模式下的结果码中显示详细的头信息
            System.out.print("\r\n[AT+CSDH?]检查短消息格式: ");
            res = new String(Tools.sendCMD(serial, "AT+CSDH?"));        //+CSDH: 1
            v = (res.indexOf("OK") > -1)?getValueFromString(res):null;
            if(v==null || !v.trim().equals("1")) {
                System.out.print("\r\n[AT+CSDH=1]设置显示文本格式: 0-不显示 / 1-显示 ...");
                res = new String(Tools.sendCMD(serial, "AT+CSDH=1"));
                if (!res.contains("OK")) {
                    System.out.println(" failed!");
                    return false;
                }
                System.out.println(" ...[DONE]\r\n");
            }
            Thread.sleep(100);

            System.out.print("\r\n[AT+CPMS=\"SM\",\"SM\",\"SM\"]设置短信保存的位置 ...");
            res = new String(Tools.sendCMD(serial, "AT+CPMS=\"SM\",\"SM\",\"SM\""));
            if (!res.contains("OK")) {
                System.out.println(" failed!");
                return false;
            }
            System.out.println(" ...[SIM card]\r\n");
            Thread.sleep(100);

            System.out.print("\r\n[AT+CNMI=2,1,0,1,1]新消息提示方式 ...");
            res = new String(Tools.sendCMD(serial, "AT+CNMI=2,1,0,1,1"));
            if (!res.contains("OK")) {
                System.out.println(" failed!");
                return false;
            }
            System.out.println(" ...[DONE]\r\n");
            Thread.sleep(100);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     *
     * 初始化GPRS.模块 AT 100ms 握手 / SIM卡检测等 AT+CPIN? 100ms 查询是否检测到SIM卡 AT+CSQ 100ms
     * 信号质量测试，值为0-31,31表示最好 AT+CCID 100ms
     * 读取SIM的CCID(SIM卡背面20位数字)，可以检测是否有SIM卡或者是否接触良好 AT+CREG? 500ms 检测是否注册网络
     *
     */
    public static boolean initGPRS(Serial serial) {
        if (!serial.isOpen()) {
            System.out.println("串口未准备好");
            return false;
        }

        //byte[] buffs = new byte[128];
        byte[] buffs;
        try {
            System.out.println("\r\ninitGPRS ...");

            System.out.print("1.同步波特率...");
            buffs = Tools.sendCMD(serial, "AT");
            String res = new String(buffs);
            if (!res.contains("OK")) {
                System.out.println("SIM868 is not ready, please check the power and serial port baud rate is correct!");
                return false;
            }
            System.out.println("[DONE]\r\n");

            System.out.print("\r\n2.查询是否检测到SIM卡...");
            res = new String(Tools.sendCMD(serial, "AT+CPIN?"));
            if (!res.contains("READY")) {
                System.out.println("[找不到SIM卡]!");
                return false;
            }
            System.out.println("[有SIM卡]\r\n");
            // System.out.println("AT+CPIN?.res:"+res);

            System.out.print("\r\n3.查询信号质量...");
            res = new String(Tools.sendCMD(serial, "AT+CSQ"));          //+CSQ: 24,99
            if (res.contains("ERROR")) {
                System.out.println("[失败]");return false;
            }
            int ig = Integer.parseInt(Tools.getValueFromString(res).split(",")[0].trim());
            System.out.println("["+ig+"]");

            System.out.print("\r\n4.查询模块是否注册网络...");
            res = new String(Tools.sendCMD(serial, "AT+CREG?"));        //+CREG: 0,1
            if (res.contains("ERROR")) {
                System.out.println("[失败]"); return false;
            }
            ig = Integer.parseInt(Tools.getValueFromString(res).split(",")[1].trim());
            if(ig==1)System.out.println("...["+ig+"],[DONE]");
            else System.out.println("...["+ig+"]");

            System.out.print("\r\n5.查询模块是否GPRS...");
            res = new String(Tools.sendCMD(serial, "AT+CGATT?"));       //+CGATT: 1
            if (res.contains("ERROR")) {
                System.out.println("[失败]"); return false;
            }
            System.out.println("...["+Tools.getValueFromString(res).trim()+"]");

            System.out.print("\r\n6.显示CCID(SIM卡背面20为数字)...");
            res = new String(Tools.sendCMD(serial, "AT+CCID"));
            System.out.println(res);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
        发送指令给SIM868
     */
    public static byte[] sendCMD(Serial serial, String cmd) {
        try {
            serial.writeln(cmd + "\r");
            return listenSerial(serial);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
        监听串口返回的数据
        每次监听时间上限是2秒
     */
    private static byte[] listenSerial(Serial serial){
        long overtime = 2 * 1000; // 每条指令超时上限 5秒
        long timecount = 0; // 计时器
        try {
            byte[] buffs = new byte[128];
            while (timecount < overtime) {
                // System.out.print(serial.available());
                if (serial.available() > 0) {
                    while (serial.available() > 0) {
                        buffs = serial.read();
                        // System.out.print(new String(serial.read()));
                        // System.out.print(new String(buffs));
                    }
                    serial.flush();
                    //System.out.println("sendCMD:"+new String(buffs));
                    if (new String(buffs).contains("OK")) {
                        timecount = overtime; // exit while
                    }
                }
                timecount += 100;
                Thread.sleep(100);
            }
            // System.out.println("sendCMD:"+new String(buffs));
            return buffs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
