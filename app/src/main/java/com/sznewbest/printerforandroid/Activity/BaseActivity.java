package com.sznewbest.printerforandroid.Activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.smartdevice.aidl.IZKCService;
import com.sznewbest.printerforandroid.common.MessageCenter;
import com.sznewbest.printerforandroid.common.MessageType;

public class BaseActivity extends AppCompatActivity {
	
	public static String MODULE_FLAG = "module_flag";
	public static int module_flag = 0;
	public static int DEVICE_MODEL = 0;
	ScreenOnOffReceiver mReceiver = null;
	private Handler mhanlder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MessageCenter.getInstance().addHandler(getHandler());
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
		module_flag = getIntent().getIntExtra(MODULE_FLAG, 8);
		bindService();
//		mReceiver = new ScreenOnOffReceiver();
//		IntentFilter screenStatusIF = new IntentFilter();
//		screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
//		screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
//		registerReceiver(mReceiver, screenStatusIF);
	}
	protected void handleStateMessage(Message message) {}
	/** handler */
	protected Handler getHandler() {
		if (mhanlder == null) {
			mhanlder = new Handler() {
				public void handleMessage(Message msg) {
					handleStateMessage(msg);
				}
			};
		}
		return mhanlder;
	}

	protected void sendMessage(Message message) {
		getHandler().sendMessage(message);
	}

	protected void sendMessage(int what, Object obj) {
		Message message = new Message();
		message.what = what;
		message.obj = obj;
		getHandler().sendMessage(message);
	}

	protected void sendEmptyMessage(int what) {
		getHandler().sendEmptyMessage(what);
	}
	
	public boolean bindSuccessFlag = false;
	public static IZKCService mIzkcService;
	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e("client", "onServiceDisconnected");
			mIzkcService = null;
			bindSuccessFlag = false;
			Toast.makeText(BaseActivity.this, "service_bind_fail", Toast.LENGTH_SHORT).show();
			//发送消息绑定成功
			sendEmptyMessage(MessageType.BaiscMessage.SEVICE_BIND_FAIL);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.e("client", "onServiceConnected");
			mIzkcService = IZKCService.Stub.asInterface(service);
			if(mIzkcService!=null){
				try {
					Toast.makeText(BaseActivity.this, "service_bind_success", Toast.LENGTH_SHORT).show();
					DEVICE_MODEL = mIzkcService.getDeviceModel();
					mIzkcService.setModuleFlag(module_flag);
					if(module_flag==3){
						mIzkcService.openBackLight(1);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				bindSuccessFlag = true;
				//发送消息绑定成功
				sendEmptyMessage(MessageType.BaiscMessage.SEVICE_BIND_SUCCESS);
			}
		}
	};

	public void bindService() {
		//com.zkc.aidl.all为远程服务的名称，不可更改
		//com.smartdevice.aidl为远程服务声明所在的包名，不可更改，
		// 对应的项目所导入的AIDL文件也应该在该包名下
		Intent intent = new Intent("com.zkc.aidl.all");
		intent.setPackage("com.smartdevice.aidl");
		bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
	}

	public void unbindService() {
		unbindService(mServiceConn);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		if(module_flag==3){
			try {
				mIzkcService.openBackLight(0);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		try {
			unbindService();
		}catch (IllegalArgumentException e){
			e.printStackTrace();
		}
//		unregisterReceiver(mReceiver);
		super.onDestroy();
	}



	public class ScreenOnOffReceiver extends BroadcastReceiver {
		private static final String TAG = "ScreenOnOffReceiver";
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("android.intent.action.SCREEN_ON")) {
//				SCREEN_ON = true;
				try {
					//打开电源+
					if(mIzkcService!=null){
						mIzkcService.setModuleFlag(8);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else if (action.equals("android.intent.action.SCREEN_OFF")) {
//				SCREEN_ON = false;
//				try {
//					//关闭电源
//					mIzkcService.setModuleFlag(8);
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				}
			}
		}
	}

}
