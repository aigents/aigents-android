/*
 * MIT License
 * 
 * Copyright (c) 2014-2020 by Anton Kolonin, Aigents®
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

import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Period;
import net.webstructor.al.Time;
import net.webstructor.core.Thing;
import net.webstructor.peer.Peer;
import net.webstructor.util.Array;

import android.content.Context;
import android.database.Cursor;
import android.provider.Browser;
import android.util.Log;
import android.net.Uri;

//TODO: move to webstructor?
public class WebProfiler {
    static String[] googles = { "http://www.google.", "https://www.google.", "http://google.", "https://google." };
    static HashSet<String> bookmarks = new HashSet<String>();
    static HashSet<String> visits = new HashSet<String>();
    static HashSet<String> searches = new HashSet<String>();
    static long last_check = 0;
    static long check_cycle = Period.HOUR;//default
    static Propertor self;
    static Propertor user;
    //https://stackoverflow.com/questions/32535325/browser-bookmarks-uri-not-working-in-android-studio/33360583
	public static final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");
	public static final Uri SEARCHES_URI = Uri.parse("content://browser/searches");
	public static final String[] HISTORY_PROJECTION = new String[]{
			"_id", // 0
			"url", // 1
			"visits", // 2
			"date", // 3
			"bookmark", // 4
			"title", // 5
			"favicon", // 6
			"thumbnail", // 7
			"touch_icon", // 8
			"user_entered", // 9
	};
	public static final int HISTORY_PROJECTION_TITLE_INDEX = 5;
	public static final int HISTORY_PROJECTION_URL_INDEX = 1;
	public static final int HISTORY_PROJECTION_DATE_INDEX = 3;
	public static final int HISTORY_PROJECTION_BOOKMARK_INDEX = 4;
	public static final int HISTORY_PROJECTION_USER_ENTERED_INDEX = 9;

	//http://developer.android.com/reference/android/provider/Browser.html
	//http://stackoverflow.com/questions/2577084/android-read-browser-history
	//http://www.higherpass.com/Android/Tutorials/Accessing-Data-With-Android-Cursors/3/
    //http://stackoverflow.com/questions/19231715/programmatically-finding-chrome-browser-history
	public static void resyncProfile(Context a, int days) {
		bookmarks.clear();
		visits.clear();
		searches.clear();

		long since = Time.today(-days).getTime();
        getBookmarks(a,since);
        getSearches(a,since);
    	Log.d(AigentsTabActivity.LOG_TAG,"Sites:");
    	for (Iterator<String> it = bookmarks.iterator(); it.hasNext();)
    		Log.d(AigentsTabActivity.LOG_TAG,it.next());
    	Log.d(AigentsTabActivity.LOG_TAG,"Visits:");
    	for (Iterator<String> it = visits.iterator(); it.hasNext();)
    		Log.d(AigentsTabActivity.LOG_TAG,it.next());
    	Log.d(AigentsTabActivity.LOG_TAG,"Things:");
    	for (Iterator<String> it = searches.iterator(); it.hasNext();)
    		Log.d(AigentsTabActivity.LOG_TAG,it.next());
    	
        Log.d(AigentsTabActivity.LOG_TAG,"Profiling ended");
	}

	public static void getBookmarks(Context a, long since) {

		Cursor cursor = a.getContentResolver().query(BOOKMARKS_URI, null, null, null, null);
        final int indexUrl = HISTORY_PROJECTION_URL_INDEX;//cursor.getColumnIndex(Browser.BookmarkColumns.URL);
        final int indexDate = HISTORY_PROJECTION_DATE_INDEX;//cursor.getColumnIndex(Browser.BookmarkColumns.DATE);
        final int indexBookmark = HISTORY_PROJECTION_BOOKMARK_INDEX;//cursor.getColumnIndex(Browser.BookmarkColumns.BOOKMARK);
	    if (cursor.moveToFirst()) {
	        while (!cursor.isAfterLast()) {
	            String url = cursor.getString(indexUrl);
	            String date = cursor.getString(indexDate);
	            long time = !AL.empty(date) ? Long.parseLong(date) : 0;
	        	//Log.d(AigentsTabActivity.LOG_TAG,url);
	            boolean search = false;
	            String google = Array.startsWith(url,googles);//check if this is a google query
	            if (google != null) {
	            	int q;
	            	//try to find query
	            	if ((q = url.indexOf("&q=", google.length())) != -1 || (q = url.indexOf("?q=", google.length())) != -1) {
	            		int e = url.indexOf('&', q += 3 );
	            		String term = e != -1 ? url.substring(q, e) : url.substring(q);
	            		term = URLDecoder.decode(term/*,"UTF-8"*/);
	            		if (time > since)
	            			searches.add(term);
	            		search = true;
	            	}
	            	else
		            //sort out redirect
		            if ((q = url.indexOf("&url=", google.length())) != -1 || (q = url.indexOf("?url=", google.length())) != -1) {
	            		int e = url.indexOf('&', q += 5 );
	            		url = e != -1 ? url.substring(q, e) : url.substring(q);
	            	}
	            }
	            //TODO:validate URL properly, see http://stackoverflow.com/questions/1600291/validating-url-in-java	            
	            if (!search && AL.isURL(url)) {
		            String bookmark = cursor.getString(indexBookmark);
		            if (!AL.empty(bookmark) && bookmark.equals("1"))
		            	bookmarks.add(url);
		            else {
	            		if (time > since)
	            			visits.add(url);
		            }
	            }
	            cursor.moveToNext();
	        }
	    }
	    cursor.close();
	}
	
	public static void getSearches(Context a, long since) {
		//String sel = Browser.BookmarkColumns.DATE + ">" + since;
		String sel = "date >" + since;
		Cursor cursor = a.getContentResolver().query(SEARCHES_URI, null, sel, null, null);
	    if (cursor.moveToFirst()) {
	        while (!cursor.isAfterLast()) {
	            final int indexTerm = HISTORY_PROJECTION_USER_ENTERED_INDEX;//cursor.getColumnIndex(Browser.SearchColumns.SEARCH);
	           // String date = cursor.getString(indexDate);
	            String term = cursor.getString(indexTerm);
	            if (!AL.empty(term))
	            	searches.add(URLDecoder.decode(term));
	            //Log.d(AigentsTabActivity.LOG_TAG,getDate(Long.parseLong(date))+" "+term);
	            cursor.moveToNext();
	        }
	    }
	    cursor.close();
	}	
	
	//TODO: move this out somewhere?
	@SuppressWarnings("rawtypes")
	public static boolean doProfileOnBack(Cell cell) {
		try {
			java.util.Set trusts = cell.storager.get(AL.trusts, cell.self());
			if (!AL.empty(trusts)) {
				Thing peer = (Thing)trusts.iterator().next();
				//do the rest only for one peer trusted by self as found above
				int history_days = cell.attentionDays();
				resyncProfile(cell.service,history_days);
				for (String search : searches) {
					Collection things = cell.storager.getNamed(search);
					if (AL.empty(things)) {
						Thing t = new Thing(search);
						t.store(cell.storager);
						peer.addThing(AL.topics, t);
						peer.addThing(AL.trusts, t);
					}
				}
				for (String bookmark : bookmarks) {
					Collection things = cell.storager.getNamed(bookmark);
					if (AL.empty(things)) {
						Thing t = new Thing(bookmark);
						t.store(cell.storager);
						peer.addThing(AL.sites, t);
						peer.addThing(AL.trusts, t);
					}
				}
				for (String visit : visits) {
					Collection things = cell.storager.getNamed(visit);
					if (AL.empty(things)) {
						Thing t = new Thing(visit);
						t.store(cell.storager);
						peer.addThing(AL.sites, t);
						//peer.addThing(AL.trusts, t);//!not trusted
					}
				}
				return true;
			}
		} catch (Exception e) {
			cell.error("Profiling : ",e);
		}
		return false;
	}

	public static boolean doProfileOnFront(AigentsTabActivity activity) {
		//get current time
		//get self retention period
		//get user check cycle (hours)
		//get user retention period (days)
		//get user last check (time)
		//if current time > last check
			//sort out retention period
			//do resyncProfile
			//update user retention period
			//update user bookmarks (set trusts) and searches (unaffect trusts)
		long current_time = System.currentTimeMillis();
		try {
			if ((current_time - last_check) > check_cycle) {
				self = new Propertor("your",new String[]{Body.attention_period});
				activity.mTalker.request(self,self.updateRequest());
				//self.waitNotified();
				String self_attention_period = self.map.get(Body.attention_period);
				int attention_period_days = AL.empty(self_attention_period) ? 0 : new Period(self_attention_period).getDays();
				if (attention_period_days < 3)
					attention_period_days = 3;
				user = new Propertor("my",new String[]{Peer.check_cycle});
				activity.mTalker.request(user,user.updateRequest());
				//user.waitNotified();
				check_cycle = new Period(user.map.get(Peer.check_cycle)).getMillis();
				if (check_cycle < Period.HOUR)
					check_cycle = Period.HOUR;
				resyncProfile(activity,attention_period_days);
				last_check = current_time;
				activity.mTalker.request(null,"My sites "+propList(bookmarks)+".");
				activity.mTalker.request(null,"My topics "+propList(searches)+".");
				activity.mTalker.request(null,"My sites "+propList(visits)+".");
				return true;
			}
		} catch (Exception e) {
			Log.e(AigentsTabActivity.LOG_TAG,e.toString());
		}//TODO: see if need to catch exceptions on wait?
		return false;
	}

	public static String propList(HashSet<String> names) {
		return AL.propList(names.toArray(new String[]{}));
	}
	
}

