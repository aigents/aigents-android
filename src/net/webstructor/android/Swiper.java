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

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

//http://stackoverflow.com/questions/6645537/how-to-detect-the-swipe-left-or-right-in-android
public class Swiper implements View.OnTouchListener {
	static final float THRESHOLD = 25;
	float startX = -1;
	float startY;
	int startPosition;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	float x,y;
    	switch(event.getAction()) {
          	case MotionEvent.ACTION_DOWN:
          	case MotionEvent.ACTION_MOVE:
          		x = event.getX(); 
          		y = event.getY();
          		startPosition = ((ListView)v).pointToPosition((int)x, (int)y);
          		if (startX == -1) {
          			startX = x;
          			startY = y;
          		}
          		break;
          	case MotionEvent.ACTION_UP:
          		if (startX != -1) {
              		x = event.getX(); y = event.getY();
          			float deltaX = x - startX;
          			float deltaY = y - startY;
          			int endPosition = ((ListView)v).pointToPosition((int)x, (int)y);
              		Log.d(AigentsTabActivity.LOG_TAG,"Swipe: deltaX="+deltaX+" position="+endPosition);
          			if (startPosition == endPosition && endPosition >= 0 
          				&& deltaX < -THRESHOLD && Math.abs(deltaY) < THRESHOLD) {
          				((DataAdapter)((ListView)v).getAdapter()).delete(endPosition);
          			}
          			startX = -1;
              		return true;
          		}
    	}           
        return false;       	    	        
    }
}
