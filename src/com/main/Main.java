package com.main;

import com.pi4j.io.serial.*;
import com.pi4j.util.CommandArgumentParser;

import java.io.Console;
import java.io.IOException;

/**
 * 核心主方法
 * 	包含功能：
 * 		- 接收短信
 *
 * 	硬件清单：
 * 		- 树莓派2B或以上
 * 		- SIM868
 * 		- 移动/联通GSM卡
 *
 * 	硬件接线：
 * 		GPIO15 - SIM868.Tx
 * 		GPIO16 - SIM868.Rx
 *
 * @author S.L
 *
 */
public class Main {

    public static void main(String args[]) {
        Console cs = System.console();
        if (cs == null) {throw new IllegalStateException("不能使用控制台");}
        System.out.println("Listen to the serial port (GPIO15-Tx / GPIO16-Rx) data");

        final Serial serial = SerialFactory.createInstance();
        try {
            SerialConfig config = new SerialConfig();
            System.out.println(">>>" + SerialPort.getDefaultPort());
            config.device(SerialPort.getDefaultPort())
                    // "/dev/ttyACM0"
                    .baud(Baud._115200).dataBits(DataBits._8)
                    .parity(Parity.NONE).stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);
            if (args.length > 0) {
                config = CommandArgumentParser.getSerialConfig(config, args);
            }
            System.out.println(" Connecting to: " + config.toString() +", Data received on serial port will be displayed below.");

            // 启动,写入串口配置,刷新该配置
            serial.open(config);
            serial.flush();
            System.out.println("serial.isOpen():" + serial.isOpen());

            System.out.println("选择工作模式[1-2]:");
            System.out.println(" 1 - 指令交互模式");
            System.out.println(" 2 - 短信监听模式");
            System.out.println(" 3 - 短信发送模式");
            String cin = cs.readLine();
            if(cin.equals("1")){
                System.out.println("***指令交互模式(退出程序请按Ctrl+c)***");
                while (true) {
                    cin = cs.readLine();
                    if(cin.toUpperCase().indexOf("AT")!=-1) {
                        String res = new String(sendCMD(serial, cin));
                        System.out.println(res);
                    }
                }
            }else if(cin.equals("2")){
                System.out.println("***短信监听模式***");
                listen_SMS(serial);
            }else if(cin.equals("3")){
                System.out.println("***短信发送模式***");
                System.out.println("======================");
                send_sms_mode(cs, serial);
            }else{
                System.out.println("***输入无效***");
            }

            serial.close();
        }catch(Exception e){
            e.printStackTrace();
        }finally {
        }
        System.out.println("***quit***");
    }

    /**
     * ***短信发送模式***
     * @param cs        Console
     * @param serial
     * @throws IOException
     */
    private static void send_sms_mode(Console cs, Serial serial) throws IOException {
        while(true) {
            System.out.print("手机号码：");
            String m = (cs.readLine()==null || cs.readLine().equals(""))?"18620671820":cs.readLine();
            System.out.print("短信内容：");
            String c = (cs.readLine()==null || cs.readLine().equals(""))?"this msg is from raspberry":cs.readLine();
            String res = new String(sendCMD(serial, "AT+CMGS=\"" + m + "\""));
            System.out.println(res);
            if (res.indexOf(">") != -1) {
                new String(sendCMD(serial, c));
                serial.write((byte) 0x1a);
            } else {
                System.out.println("SIM868无回应，或回应错误");
            }
        }
    }

    private static void listen_SMS(Serial serial) throws InterruptedException, IOException {
        /** 初始化GPRS模块 **/
        boolean isinit = initGPRS(serial);
        System.out.println("初始化GPRS模块:"+isinit);

        /** 初始化短信参数 **/
        isinit = initGPRS_SMS(serial);
        System.out.println("初始化短信参数:"+isinit);

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

                    /**
                     * 有新短信时(无来电功能时): +CIEV: "MESSAGE",1
                     * +CMTI: "SM",1
                     */
                    // 如果有+CMTI这个字符串,发送该内存的位置
                    if (cc.indexOf("+CMTI") != -1) {
                        index = getIndexFromNewSMS(cc);               //解析出索引位置
                        System.out.println("Find new SMS.index:" + index);
                        sendCMD(serial, "AT+CMGR=" + index);    //发送读取短信的指令
                    }
                    // 如果有+CMGR这个字符串,读取并发送短信内容到服务器后,删除该短信
                    // ************************如果没有发送成功(服务器或设备宕机),因为sendDataToServer发送后总是return true,意味着会删除*****************
                    if (cc.indexOf("+CMGR") != -1) {
                        // 从内存位置index开始读取短信内容
                        String[] contents = getContentFromIndex(index, cc);
                        System.out.println("[AT+CMGR=index]Read the contents of the message on the card. After analysis:");
                        if (contents != null) {
                            System.out.println("New SMS content:");
                            for (String tt : contents) {
                                System.out.println(tt);
                            }

                            /**
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
                if (v.indexOf("CMTI: \"SM\",") != -1) {
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
     * @return
     */
    public static boolean delSMSByIndex(Serial serial, int index) {
        String res = new String(sendCMD(serial, "AT+CMGD=" + index));
        System.out.println("AT+CMGD=" + index + ":" + res);
        // if(res.indexOf("OK")==-1){
        // System.out.println("删除["+index+"]位置的短信失败!");
        // return false;
        // }
        return true;
    }

    /**
     * 解析返回的短信内容
     *
     * @return
     */
    // data是从SIM卡读到的所有内容,cc是GBK格式的data字符串,传递到这个方法当做参数res
    public static String[] getContentFromIndex(int index, String res) {
        try {
            System.out.println("Try reading SMS...getContentFromIndex.res:" + res);
            // 原始数据data中包含'OK'的字符串
            if (res.indexOf("OK") != -1) {
                System.out.println("Get SMS success, parse content ...");
                /**
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
                    if (ccs[i].indexOf("CMGR:") != -1) {
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

            } else if (res.indexOf("CMS ERROR") != -1) {
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
     *
     * 初始化GPRS.设置短信模式及短信接收参数 AT+CMGF=1 0-PDU, 1-文本格式 AT+CSDH=1
     * AT+CPMS="SM","SM","SM" 将信息保存在SIM卡中, SM-表示存在SIM卡上 AT+CNMI=2,1,0,1,1
     * 收接通知,并存在指定位置(与AT+CPMS设置有关)
     *
     * 设置好后, 收到短信: +CIEV: "MESSAGE",1 +CMTI: "SM",1 表示存储位置index=1
     *
     * @return
     */
    private static boolean initGPRS_SMS(Serial serial) {
        if (!serial.isOpen()) {
            return false;
        } // 串口未准备好
        String res = new String();
        try {
            System.out.print("\r\nSet SMS format ...");
            res = new String(sendCMD(serial, "AT+CMGF=1"));
            if (res.indexOf("OK") == -1) {
                System.out.println("Setup failed!");
                return false;
            }
            System.out.println("...[text format]\r\n");
            Thread.sleep(100);

            System.out.print("\r\nAT+CSDH=1...");
            res = new String(sendCMD(serial, "AT+CSDH=1"));
            if (res.indexOf("OK") == -1) {
                System.out.println("Setup failed!");
                return false;
            }
            System.out.println(" ...[DONE]\r\n");
            Thread.sleep(100);

            System.out.print("\r\nSet SMS save location ...");
            res = new String(sendCMD(serial, "AT+CPMS=\"SM\",\"SM\",\"SM\""));
            if (res.indexOf("OK") == -1) {
                System.out.println("Setup failed!");
                return false;
            }
            System.out.println(" ...[SIM card]\r\n");
            Thread.sleep(100);

            System.out.print("\r\nReceive SMS, and save in the specified location ...");
            res = new String(sendCMD(serial, "AT+CNMI=2,1,0,1,1"));
            if (res.indexOf("OK") == -1) {
                System.out.println("Setup failed!");
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
     * @return
     */
    private static boolean initGPRS(Serial serial) {
        if (!serial.isOpen()) {
            return false;
        } // 串口未准备好

        byte[] buffs = new byte[128];

        try {
            System.out.println("try send AT to module...");
            // char cmd[] = {'A', 'T'};
            // byte cmd[] = "AT".getBytes();
            // buffs = sendCMD(serial, "AT".getBytes());
            System.out.print("\r\nGPRS module testing ...");
            buffs = sendCMD(serial, "AT");
            String res = new String(buffs);
            if (res.indexOf("OK") == -1) {
                System.out.println("GPRS module is not ready, please check the power and serial port baud rate is correct!");
                return false;
            }
            System.out.println(" ...[正常]\r\n");
            // System.out.println("AT.res:"+res);

            System.out.print("\r\nTesting SIM card...");
            res = new String(sendCMD(serial, "AT+CPIN?"));
            System.out.println(res);
            if (res.indexOf("READY") == -1) {
                System.out.println("SIM card is not ready!");
                return false;
            }
            System.out.println(" ...[正常]\r\n");
            // System.out.println("AT+CPIN?.res:"+res);

            System.out.print("\r\nSignal quality testing, the value of 0-31,31 said the best ...");
            res = new String(sendCMD(serial, "AT+CSQ"));
            if (res.indexOf("ERROR") != -1) {
                System.out.println("Signal quality test failed");
                return false;
            }
            /**
             * +CSQ: 24,99
             */
            String[] vs = res.split("\r\n");
            for (String v : vs) {
                if (v.indexOf(":") != -1) {
                    String x = v.substring(v.indexOf(":") + 1);
                    System.out.println("... signal strength:[" + x.trim() + "]\r\n");
                    if(x.equals("0")){
                        System.out.println("signal is to low, please check sim card.");
                    }
                }
            }
            // System.out.println("AT+CSQ.res:"+res);

            res = new String(sendCMD(serial, "AT+CCID"));
            System.out.println("AT+CCID.res:" + res);

            res = new String(sendCMD(serial, "AT+CREG?"));
            System.out.println("AT+CREG.res:" + res);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static byte[] sendCMD(Serial serial, String cmd) {
        long overtime = 10000; // 每条指令超时上限 5秒
        long timecount = 0; // 计时器
        byte[] buffs = new byte[128];

        try {
            serial.writeln(cmd + "\r");
            // serial.writeln("AT\r");
            timecount = 0;
            while (timecount < overtime) {
                // System.out.print(serial.available());
                if (serial.available() > 0) {
                    while (serial.available() > 0) {
                        buffs = serial.read();
                        // System.out.print(new String(serial.read()));
                        // System.out.print(new String(buffs));
                    }
                    serial.flush();
                    timecount = overtime; // exit while
                }
                timecount += 100;
                Thread.sleep(100);
            }
            // System.out.println("sendCMD:"+new String(buffs));
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return buffs;
    }
}
