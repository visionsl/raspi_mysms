package com.main;

import com.SIM8xx.Tools;
import com.pi4j.io.gpio.*;
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
    static GpioPinDigitalOutput sim868_en;

    public static void main(String args[]) {
        Console cs = System.console();
        if (cs == null) {throw new IllegalStateException("不能使用控制台");}
        System.out.println("Listen to the serial port (GPIO15-Tx / GPIO16-Rx) data");

        final GpioController gpio = GpioFactory.getInstance();
        final Serial serial = SerialFactory.createInstance();

        // create and register the serial data listener
        /*
        serial.addListener(new SerialDataEventListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {
                // NOTE! - It is extremely important to read the data received from the
                // serial port.  If it does not get read from the receive buffer, the
                // buffer will continue to grow and consume memory.
                // print out the data received to the console
                try {
                    System.out.println("[HEX DATA]   " + event.getHexByteString());
                    System.out.println("[ASCII DATA] " + event.getAsciiString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        */

        Pin pin_sim868_en = CommandArgumentParser.getPin(RaspiPin.class,RaspiPin.GPIO_07,args);
        sim868_en = gpio.provisionDigitalOutputPin(pin_sim868_en, "pin_sim868_en", PinState.HIGH);
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

            Tools.initSIM868(serial, sim868_en);
            System.out.println();
            do {
                System.out.println(" 1 - 指令交互模式");
                System.out.println(" 2 - 短信监听模式");
                System.out.println(" 3 - 短信发送模式");
                System.out.println(" 4 - 自检");
                System.out.println(" 5 - 网络测试");
                System.out.println(" 9 - 重启模块");
                System.out.print("选择工作模式[1-4]:");
                String cin = cs.readLine();
                switch (cin.trim()) {
                    case "1":
                        System.out.println("***指令交互模式(退出程序请按Ctrl+c)***");
                        while (true) {
                            cin = cs.readLine();
                            if (cin.toUpperCase().equals("QUIT")) {
                                System.out.println("退出指令交互模式");
                                break;
                            } else if (cin.trim().equals("*")) {
                                serial.write((byte) 0x1A);
                                System.out.println("send 0x1A");
                            } else {
                                String res = new String(Tools.sendCMD(serial, cin));
                                System.out.println(res);
                            }
                        }
                        break;
                    case "2":
                        Tools.initGPRS_SMS_pub(serial, sim868_en);
                        System.out.println("***短信监听模式***");
                        Tools.listen_SMS(serial);
                        break;
                    case "3":
                        Tools.initGPRS_SMS_pub(serial, sim868_en);
                        System.out.println("***短信发送模式***");
                        System.out.println("======================");
                        Tools.send_sms_mode(cs, serial);
                        break;
                    case "4":
                        System.out.println("***SIM868自检模式***");
                        System.out.println("======================");
                        Tools.checkSelf(serial);
                        break;
                    case "5":
                        System.out.println();System.out.println("***SIM868网络Socket测试***");
                        System.out.println("======================");
                        Tools.gprsTest(serial);
                        break;
                    case "9":
                        Tools.rebootSIMPower(sim868_en);
                        System.out.println(" ...done!");
                        if (!Tools.initSIM868(serial, sim868_en)) {
                            System.out.println("exit !");
                            break;
                        }
                        System.out.println("======================");
                        System.out.println();
                        break;
                    default:
                        System.out.println("***输入无效***");
                }
            } while (true);

            //serial.close();
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            System.out.println("Done");
        }
        System.out.println("***quit***");
    }



}
