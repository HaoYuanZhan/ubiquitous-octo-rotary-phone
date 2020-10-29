package com.sznewbest.printerforandroid;


import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sznewbest.printerforandroid.common.MessageType;

import java.io.UnsupportedEncodingException;


public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    //线程运行标志 the running flag of thread
    private boolean runFlag = true;
    //打印机检测标志 the detect flag of printer
    private boolean detectFlag = false;
    //打印机连接超时时间 link timeout of printer
    private float PINTER_LINK_TIMEOUT_MAX = 30*1000L;
    DetectPrinterThread mDetectPrinterThread;

    EditText edittext_print;

    String ipStr="test.buybal.com";
    int portNo=28888;

    String android_id="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android_id = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        edittext_print = (EditText) findViewById(R.id.edittext_print);
        edittext_print.setText("gfhgfh");
        Button button_print = (Button) findViewById(R.id.button_print);
        button_print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = edittext_print.getText().toString() + "\n\n\n";
                String utf8Str = "UTF8编码打印:\n" + str;
                byte[] btUTF8 = new byte[0];
                try {
                    btUTF8 = utf8Str.getBytes("UTF-8");

                    //modify printer encoding to utf-8
                    mIzkcService.sendRAWData("print", new byte[]{0x1C, 0x43, (byte) 0xFF});
                    //must sleep，wait setting and save success
                    SystemClock.sleep(100);
                    mIzkcService.sendRAWData("print", btUTF8);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });

        mDetectPrinterThread = new DetectPrinterThread();
        mDetectPrinterThread.start();

        //ipStr="192.168.1.25";
        //tcpClass=new TcpClass(ipStr,portNo);

    }


    public void permitPrint() throws RemoteException {

        //唤醒打印机
        mIzkcService.sendRAWData("",new byte[] {0x00,0x00,0x00,0x00,0x00});
        SystemClock.sleep(100);
		/* 关闭自动休眠
		 * 1E 02 N1 N2 N3 N4 N5
		 * 说明: N1=1. 开启自动进入休眠功能;
		 * N1=0. 关闭自动进入休眠功能;*/
        mIzkcService.sendRAWData("",new byte[] {0x1E, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00});
        SystemClock.sleep(100);

        //关闭自动禁止打印
        mIzkcService.sendRAWData("",new byte[]{0x1E , 0x04 , 0x00 , (byte) 0xBF , (byte) 0xD8 , (byte) 0xD6 , (byte) 0xC6});
        SystemClock.sleep(100);

		/*
		 * 设置允许打印
		*协议: 1E 03 N BF D8 D6 C6
		说明: N = 1 允许打印. 单片机返回"bPtCtrl Enable !\r\n"
			  N = 0 禁止打印. 单片机返回"bPtCtrl Disable !\r\n"*/
        mIzkcService.sendRAWData("",new byte[] {0x1E, 0x03, 0x01,
                (byte) 0xBF, (byte) 0xD8, (byte) 0xD6, (byte) 0xC6 });
        SystemClock.sleep(100);
    }
    @Override
    protected void handleStateMessage(Message message) {
        super.handleStateMessage(message);
        switch (message.what){
            //服务绑定成功 service bind success
            case MessageType.BaiscMessage.SEVICE_BIND_SUCCESS:
				Toast.makeText(this, "service bind success", Toast.LENGTH_SHORT).show();
                try {
                    mIzkcService.setModuleFlag(8);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            //服务绑定失败 service bind fail
            case MessageType.BaiscMessage.SEVICE_BIND_FAIL:
				Toast.makeText(this, " service bind fail", Toast.LENGTH_SHORT).show();
                break;
            //打印机连接成功 printer link success
            case MessageType.BaiscMessage.DETECT_PRINTER_SUCCESS:
                String msg = (String) message.obj;
                Toast.makeText(this, "printer link success", Toast.LENGTH_SHORT).show();
                break;
            //打印机连接超时 printer link timeout
            case MessageType.BaiscMessage.PRINTER_LINK_TIMEOUT:
                Toast.makeText(this, "printer link timeout", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    class DetectPrinterThread extends Thread{
        @Override
        public void run() {
            super.run();

            while(runFlag){
                float start_time = SystemClock.currentThreadTimeMillis();
                float end_time = 0;
                float time_lapse = 0;
                if(detectFlag){
                    //检测打印是否正常 detect if printer is normal
                    try {
                        if(mIzkcService!=null){
                            String printerSoftVersion = mIzkcService.getFirmwareVersion1();
                            if(TextUtils.isEmpty(printerSoftVersion)){
                                mIzkcService.setModuleFlag(0);
                                end_time = SystemClock.currentThreadTimeMillis();
                                time_lapse = end_time - start_time;
                                if(time_lapse>PINTER_LINK_TIMEOUT_MAX){
                                    detectFlag = false;
                                    //打印机连接超时 printer link timeout
                                    sendEmptyMessage(MessageType.BaiscMessage.PRINTER_LINK_TIMEOUT);
                                }
                            }else{
                                //打印机连接成功 printer link success
                                sendMessage(MessageType.BaiscMessage.DETECT_PRINTER_SUCCESS, printerSoftVersion);
                                detectFlag = false;
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }
                SystemClock.sleep(1000);
            }

        }
    }

    @Override
    protected void onResume() {
        detectFlag=true;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        runFlag = false;
        mDetectPrinterThread.interrupt();
        mDetectPrinterThread = null;

    }
}
