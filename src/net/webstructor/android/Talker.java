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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import android.util.Log;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Parser;
import net.webstructor.comm.Communicator;
import net.webstructor.peer.Session;

public class Talker extends Communicator {
	public static final long HEARTBEAT_MILLIS = 60000; 
//	public static final long HEARTBEAT_MILLIS = 15000; //debug only
	
	AigentsTabActivity activity;
	Session session = null;
	boolean logged = false;
	LinkedList<DataRequestor> requestors = new LinkedList<DataRequestor>();
	
	public static DataRequestor noEcho = null; /*new DataRequestor() {
		@Override
		public void update(String string) {}
		@Override
		public String type() {return null;}
		@Override
		public void clear() {}
		@Override
		public String updateRequest() {return null;}
		@Override
		public String addRequest(String str) {return null;}
		@Override
		public String deleteRequest(int position) {return null;}
	};*/
	
	public Talker(AigentsTabActivity apper, Body body)
	{
		super(body);
		this.activity = apper;
		input("");//seed conversation

		//TODO: enable this back later, when other problems are solved
		/*
		final Handler handler=new Handler();
		final Runnable checker = new Runnable() {
		    public void run() {
		    	if (logged) {
		    		//TODO: sort this out, it should work together!
		    		//if resync is not done
		    		//if (!WebProfiler.doProfile(activity)) 
		    		if (activity.isVisible()) {
		    			//update news tape
		    			DataAdapter da = (DataAdapter)activity.mNewsList.getAdapter();
		    			request(da, da.updateRequest());
		    		}
		    	}
		        handler.postDelayed(this, HEARTBEAT_MILLIS);
		    }
		};
		handler.postDelayed(checker, HEARTBEAT_MILLIS);
		*/
		
	}
	
	synchronized void request(DataRequestor adapter, String message) {
		//activity.outputPeerText(message);//TODO: make self text echoable based on configuration
		if (adapter != null)
			requestors.add(adapter);
		input(message);
		Log.d(AigentsTabActivity.LOG_TAG,"Say: "+message);
	}
	
	public synchronized void output(Session session, String text) throws IOException {
		//TODO: use session

		final String messageText = text; 
		activity.runOnUiThread(new Runnable() {
			public void run() {

		String message = messageText;
		if (requestors.size() == 0)
			activity.outputSelfText(message);
		if (message.startsWith(Body.APPNAME) && message.indexOf(Body.COPYRIGHT) != -1)
			message = message.substring(message.indexOf('\n')+1);
		Log.d(AigentsTabActivity.LOG_TAG,"Get: "+message);
		if (!logged) { 
			requestors.poll();//cleanup the queue of handlers just in case?
			if (message.startsWith("Ok.") || message.startsWith("Hello ")) {
				logged = true;
				activity.enableWork();
				//TODO: start checker for release or make it configurable for user!!!
				//checker.start();
			}
			else
			if (message.startsWith("What your ")) {
				activity.displayStatus(message);
				ask(message.substring("What your ".length()));
			}
		}
		else { //logged
			//TODO: dispatch output across multiple requestors by type?
			//DataModel requestor = (DataModel)requestors.peek();
			DataRequestor requestor = (DataRequestor)requestors.poll();
			if (requestor != null) {
				String pattern = "Your "+requestor.type()+" ";
				if (message.startsWith(pattern)) {
					requestor.update(message.substring(pattern.length()));
				}
				else
				if (message.startsWith("Your not")) {
					requestor.clear();//TODO: do nothing?
				}
				else
				if (message.startsWith("There ")) {
					requestor.update(message.substring("There ".length()));
				}
				else
				if (message.startsWith("Ok.")) { //if taken, repeat refresh
					//requestor.confirm();
					request(requestor,requestor.updateRequest());
				}
				else {
					String error = AL.error(message);
					//if (message.startsWith("Error") || message.startsWith("No")) {
					if (error != null) {
						activity.displayStatus(message);
						/*
						if (logged)
							App.getApp().showWorkTabs();
						else
							App.getApp().showTalkTabs();
							*/
					} else
						requestor.update(message);
				}
			}
		}

			}
		});		
	}
	
	//builds the form request fields from list of fields to ask
	//TODO: redo using proper Query reader?
	public void ask(String message) {
    	//String q = "My";
    	//message = message.replaceAll("\\?","");
    	//String[] p = message.split(", "); 	
    	ArrayList<String> list = new ArrayList<String>();
    	Parser p = new Parser(message);
    	while (!p.end()) {
    		StringBuilder name = new StringBuilder();
        	while (!p.end()) {
        		String s = p.parse();
        		if (AL.punctuation.indexOf(s) != -1)
        			break;
        		if (name.length() > 0)
        			name.append(' ');
    			name.append(s);
        	}
        	if (name.length() > 0)
        		list.add(name.toString());
    	}
    	if (!AL.empty(list)) {
    		String[] fields = list.toArray(new String[]{});
    		activity.editProperties("My",fields,false);
    	}
	}

	public void input(String message) {
		try {
			if (session == null)
				session = body.sessioner.getSession(this,"1");//"0" is reserved by Cmdliner
        	body.conversationer.handle(this, session, message);
        } catch (IOException e) {
        	body.error("Session error", e);
        }
	}

}
