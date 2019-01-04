/*
 * MIT License
 * 
 * Copyright (c) 2014-2019 by Anton Kolonin, Aigents
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.webstructor.android;

import java.io.File;
import java.util.ArrayList;

import net.webstructor.agent.Body;
import net.webstructor.agent.Farm;
import net.webstructor.al.Period;
import net.webstructor.self.Siter;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

class Cell extends Farm {

	AigentsService service;
	SmsManager smsManager = null;
	long lastSyncTime = 0; 
	
	Cell(AigentsService service) {
		//super(new String[]{},false,false,false,false,1);//no activities, 1 conversation
		//enable all except logger and console, 2 conversationers to enable console attachments
		super(new String[]{},false,false,true,true,true,false,2);//no social
		this.service = service;
	}
	
	@Override
	public int checkMemory(){//in range 0-100 percents
	    ActivityManager activityManager = (ActivityManager) service.getSystemService(Context.ACTIVITY_SERVICE);
	    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
	    activityManager.getMemoryInfo(memoryInfo);//in bytes, total system-wide
	    //https://developer.android.com/topic/performance/memory#java
	    //https://github.com/eclipse/paho.mqtt.android/issues/292
	    //https://developer.android.com/guide/topics/manifest/application-element#largeHeap
	    //https://developer.android.com/reference/android/app/ActivityManager#getMemoryClass()
	    //int megabytes_available = activityManager.getMemoryClass();
	    return memoryInfo.lowMemory || memoryInfo.availMem < memoryInfo.threshold ? 100 : 50;
	}
	
	@Override
	public void output(String str) {
		Log.i(AigentsTabActivity.LOG_TAG, str);
	}
	
	@Override
	public synchronized void error(String str,Throwable e) {
		Log.e(AigentsTabActivity.LOG_TAG, str, e);
	}

	@Override
	public void debug(String str) {
		Log.d(AigentsTabActivity.LOG_TAG, str);
	}
	
	@Override
	public File getFile(String path) {
		//String name = new File(path).getName();
		//File file = new File(service.getFilesDir(), name);
		//return file;
		File base = service.getFilesDir();
		File f = new File(path);
		File p = f.getParentFile();
		File r = p == null
			? new File(base, f.getName())
			: new File(getFile(p.getPath()), f.getName());
		return r;
	}

	//http://www.tutorialspoint.com/android/android_sending_sms.htm
	@Override
	public void textMessage(String phone, String subject, String message) {
		if (smsManager == null)
			smsManager = SmsManager.getDefault();
		//TODO: use subject
		try {
			//http://stackoverflow.com/questions/4580952/why-do-i-get-nullpointerexception-when-sending-an-sms-on-an-htc-desire-or-what
			//smsManager.sendTextMessage(phone, null, message, null, null);
			
			ArrayList<String> parts = smsManager.divideMessage(message);
			smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
			
			Log.i(AigentsTabActivity.LOG_TAG,"SMS sent to "+phone);
		} catch (Exception e) {
			Log.e(AigentsTabActivity.LOG_TAG,"SMS not sent to "+phone,e);
		}
	}
	
	//say for Android, display count of news specific to self owner (1st one trusted peer) 
	@Override
	public void updateStatus(boolean now) {
		super.updateStatus(now);
		int pending = Siter.pendingNewsCount(this);
		debug("Pending "+pending);
		if (pending > 0) {
			AigentsTabActivity.displayPending(service, service.getActivityClass(), pending);
		}
		
		//TODO:move this out somewhere
		long currentTime = System.currentTimeMillis();
		if ((currentTime - lastSyncTime) > Period.HOUR) {
			lastSyncTime = currentTime;
			boolean profiled = WebProfiler.doProfileOnBack(this);
			debug("Profiled : "+profiled);
		}
	}
	
}

public class AigentsService extends Service {
  
  /*
  public class AigentsBinder extends Binder {
      AigentsService getService() {
          return AigentsService.this;
      }
  }
  */
	Cell mCell;
	private Class<?> activityClass; //TODO: this in better way
  
  	private final IBinder mBinder = new AigentsBinder();
  	public class AigentsBinder extends Binder {
  		public AigentsService getService() {
  			return AigentsService.this;
	    }
  	}
	
  	public Class<?> getActivityClass() {
  		if (activityClass == null)
  			activityClass = AigentsTabActivity.getActivityClass(this);
  		return activityClass;
  	}
  	
  	public void onCreate() {
    	Log.d(AigentsTabActivity.LOG_TAG, "onCreate");
	  	super.onCreate();
	  	try {
	  		mCell = new Cell(this);
	  		mCell.start();
	  	} catch (Exception e) {
        	String s=Body.toString(e);
        	Log.e(AigentsTabActivity.LOG_TAG, s);
	  	}
  	}
  
  	public int onStartCommand(Intent intent, int flags, int startId) {
	  	int res = super.onStartCommand(intent, flags, startId);
	  	Log.d(AigentsTabActivity.LOG_TAG, "onStartCommand "+res);
	  	return res;
  	}

  	public void onDestroy() {
  		mCell.save();
	  	super.onDestroy();
	  	Log.d(AigentsTabActivity.LOG_TAG, "onDestroy");
  	}

  	public IBinder onBind(Intent intent) {
		mCell.updateStatus(false);
	  	Log.d(AigentsTabActivity.LOG_TAG, "onBind");
	    return mBinder;
  	}
  
  	String input(String text) {
  		return "You said: "+text+".";
  	}
  
}