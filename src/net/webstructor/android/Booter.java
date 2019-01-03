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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

//http://stackoverflow.com/questions/4562734/android-starting-service-at-boot-time
//http://stackoverflow.com/questions/4459058/alarm-manager-example/8801990#8801990
public class Booter extends BroadcastReceiver {   
    @Override
    public void onReceive(Context context, Intent intent) {
    	if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
    		Intent myIntent = new Intent(context, AigentsService.class);
    		//myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//TODO: or not to do?
    		Log.d(AigentsTabActivity.LOG_TAG, "onBoot");
    		context.startService(myIntent);
    	}
    }
}
