package com.main;

import com.pi4j.io.serial.*;
import com.pi4j.util.CommandArgumentParser;

import java.io.Console;

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
        if (cs == null) {
            throw new IllegalStateException("不能使用控制台");
        }
        System.out.println("Listen to the serial port (GPIO15-Tx / GPIO16-Rx) data");

        final Serial serial = SerialFactory.createInstance();
        byte[] data = new byte[1024]; // 数据缓冲区

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



            while (true) {
                String cin = cs.readLine();
                String res = new String(sendCMD(serial, cin));
                System.out.println(res);
            }


            /** 初始化GPRS模块 **/
            //boolean isinit = initGPRS(serial);
            //System.out.println("初始化GPRS模块:"+isinit);

            /** 初始化短信参数 **/
            //isinit = initGPRS_SMS(serial);
            //System.out.println("初始化短信参数:"+isinit);


            //System.out.println(cs.readLine());

            //System.out.println("end.");

        }catch(Exception e){
            e.printStackTrace();
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
     * @return
     */
    public static boolean initGPRS_SMS(Serial serial) {
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
    public static boolean initGPRS(Serial serial) {
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

    public static byte[] sendCMD(Serial serial, String cmd) {
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
