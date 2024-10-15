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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import net.webstructor.agent.Body;
import net.webstructor.al.AL;
import net.webstructor.al.Writer;
//import net.webstructor.android.free.R;
import net.webstructor.android.R;
import net.webstructor.peer.Peer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
//import androidx.core.content.ContextCompat;
import android.Manifest;

public class AigentsTabActivity extends TabActivity implements Apper {

	static final String LOG_TAG = "Aigents";
	static final int MAX_LOG = 65000;
	
	static final String fields_name[] = {"name"};

	private EditText mThingsInput;
    private EditText mSitesInput;
	private EditText mNewsInput;
    private EditText mPeersInput;

    private TextView mTalksLog;

    private EditText mTalksInput;
    private ListView mThingsList;
    private ListView mSitesList;
    ListView mNewsList;
    private ListView mPeersList;
    private AnimationDrawable mAnimation = null;
    
   	private AigentsService mService;
   	private ServiceConnection mConnection;
	Talker mTalker;
	protected Boolean visible = Boolean.valueOf(false);
	
	String filter = null;
	
	LinkedList<String> pendingRequests = new LinkedList<String>();

   	private static Class<?> activityClass = null;//TODO: this in better way?
	
	/*
	//TODO:animation
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
	   super.onWindowFocusChanged(hasFocus);
	   if (hasFocus) {
		   mAnimation.start();
	   }
	}
	*/
   	public static Class<?> getActivityClass() {
   		return activityClass;
   	}
 
  	public static Class<?> getActivityClass(Context context) {
/*
  		try {
  			//remember origin class of the main activity because it may be different (free or premium)
  			String packageName = getPackageName();
  			Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
  			String className = launchIntent.getComponent().getClassName();
  			AigentsService.activityClass = Class.forName(className);
  		} catch (Exception e) {}
 */
  		String rootPackage = context.getApplicationContext().getPackageName();
  		Class<?> cls = null; 
  	   	try {
    		cls = Class.forName(rootPackage+".AigentsTabActivity");
    	} catch (Exception e) {}
  		return cls;
  	}
  	
	protected boolean isVisible() {
		boolean visible;
		synchronized (this.visible) { visible = this.visible.booleanValue(); }
		return visible;
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	synchronized (visible) { visible = Boolean.TRUE; }
    	Intent intent= getIntent();
    	if (intent != null) {
    		Bundle extras = getIntent().getExtras();
    		if (extras != null) {    		
	    		//TODO:
	    		String req = (String)extras.get("req");
	    		if (!AL.empty(req)) {
	    			if (mTalker != null)
	    				mTalker.request(Talker.noEcho,(String)req);
	    			else
	    				this.pendingRequests.add(req);
	    		}
	    		
	    		if ("news".equals(extras.get("tab"))) {
	    			if (getTabHost().getCurrentTab() == 2)
	    				refreshTab(getTabHost().getCurrentTab());
	    			else
	    				getTabHost().setCurrentTab(2); 
	    		}
    		}
    	}
        Log.d(LOG_TAG,"Resumed");
    }

    @Override
    protected void onPause() {
    	synchronized (visible) { visible = Boolean.FALSE; }
    	super.onPause();
        Log.d(LOG_TAG,"Paused");
    }
    
    @Override
    protected void onDestroy () {
    	if (mConnection != null) {
    		unbindService(mConnection);
    		mConnection = null;
    	}
    	super.onDestroy();
        Log.d(LOG_TAG,"Destroyed");
    }

    //TODO:this is not needed to enforce News tab opening on resume?
    @Override
    protected void onNewIntent (Intent intent) {
    	super.onNewIntent(intent);
    	setIntent(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	activityClass = this.getClass();
 
    	//IntentFilter intentFilter = new IntentFilter();
    	//intentFilter.addAction(AIGENTS_WAKEUP);
    	//LocalBroadcastManager.getInstance(this).registerReceiver(waker, intentFilter);
   
    	/*
    	Handler handler = new Handler();
    		handler.postDelayed(new Runnable(){
    			public void run(){
    				init();
    			}}, 1);
    }
    void init() {
 */
    	
       try {
	        final TabHost tabHost = getTabHost();
	        LayoutInflater.from(this).inflate(R.layout.tabs, tabHost.getTabContentView(), true);

	        //TODO PeersList permissions properly
			//https://developer.android.com/training/permissions/requesting
			//https://stackoverflow.com/questions/29915919/permission-denial-opening-provider-com-android-providers-contacts-contactsprovi
			//https://stackoverflow.com/questions/2356084/read-all-contacts-phone-numbers-in-android/3
			//https://stackoverflow.com/questions/32151603/scan-results-available-action-return-empty-list-in-android-6-0/32151901
//TODO
		    /*
		   	if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
				   != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				// Permission is not granted
				// Should we show an explanation?
				if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
						Manifest.permission.READ_CONTACTS)) {
					// Show an explanation to the user *asynchronously* -- don't block
					// this thread waiting for the user's response! After the user
					// sees the explanation, try again to request the permission.
				} else {
					// No explanation needed, we can request the permission.
					ActivityCompat.requestPermissions(thisActivity,
							arrayOf(Manifest.permission.READ_CONTACTS),
							MY_PERMISSIONS_REQUEST_READ_CONTACTS)

					// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
					// app-defined int constant. The callback method gets the
					// result of the request.
				}
			} else {
				// Permission has already been granted
			}
			*/

		   //TODO:animation
	        /*
	    	//http://developer.alexanderklimov.ru/android/animation/frameanimation.php
	        ImageView image = (ImageView)findViewById(R.id.image); 
	        image.setBackgroundResource(R.drawable.animation); 
	        //image.setScaleType(ScaleType.CENTER);
	        mAnimation = (AnimationDrawable)image.getBackground();
	        */	    	
	        
	        tabHost.addTab(tabHost.newTabSpec(getString(R.string.things))
	                .setIndicator(getString(R.string.things), getResources().getDrawable(R.drawable.things))
	                .setContent(R.id.things));
	        tabHost.addTab(tabHost.newTabSpec(getString(R.string.sites))
	                .setIndicator(getString(R.string.sites), getResources().getDrawable(R.drawable.sites))
	                .setContent(R.id.sites));
	        tabHost.addTab(tabHost.newTabSpec(getString(R.string.news))
	                .setIndicator(getString(R.string.news), getResources().getDrawable(R.drawable.news))
	                .setContent(R.id.news));
	        tabHost.addTab(tabHost.newTabSpec(getString(R.string.peers))
	                .setIndicator(getString(R.string.peers), getResources().getDrawable(R.drawable.folks_gb))
	                .setContent(R.id.peers));
	        tabHost.addTab(tabHost.newTabSpec(getString(R.string.talks))
	                .setIndicator(getString(R.string.talks), getResources().getDrawable(R.drawable.talks))
	                .setContent(R.id.talks));
	        tabHost.setOnTabChangedListener(new OnTabChangeListener() {
                @Override
                public void onTabChanged(String arg0) {         
                    //Log.i(LOG_TAG, "Im currently in tab with index::" + tabHost.getCurrentTab());
                    refreshTab(tabHost.getCurrentTab());
                }       
            });  
	        
	        //TODO: move somewhere	        
	        mThingsList = createList(R.id.things_list,R.layout.list_things, "topics",
		        new String[] {"name", "trust"},new int[] {R.id.things_name, R.id.things_check},
		       	new Object[][] {},R.id.things_check,R.id.things_name,
		       	mThingsInput = (EditText) findViewById(R.id.things_input)
		       	);
	        mThingsInput.setHorizontallyScrolling(true);
	        ((ImageButton) findViewById(R.id.things_add)).setOnClickListener(mThingsAddListener);
	        final EditText thingsinput = mThingsInput;
	        final ListView thingslist = mThingsList;
	        thingsinput.setOnKeyListener(new OnKeyListener() {
	            @Override
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
	                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                	input(thingsinput,thingslist);
	                	return true;
	                }
	                return false;
	            }
	        });
	        thingsinput.addTextChangedListener(new Twatcher((ListView)mThingsList));
	        /*
	        		new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {                                   
	                ((Filterable)((ListView)mThingsList).getAdapter()).getFilter().filter(s);
                }                       
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}   
                @Override
                public void afterTextChanged(Editable s) {}
            });
            */
	        //mThingsList.setOnTouchListener(new Swiper());
	        
	        mSitesList = createList(R.id.sites_list,R.layout.list_sites, "sites",
		        new String[] {"name", "trust"}, new int[] {R.id.sites_name, R.id.sites_check},
		        new Object[][] {},R.id.sites_check,R.id.sites_name,
		        mSitesInput = (EditText) findViewById(R.id.sites_input)
		        );
	        mSitesInput.setHorizontallyScrolling(true);
	        ((ImageButton) findViewById(R.id.sites_add)).setOnClickListener(mSitesAddListener);        
	        final EditText sitesinput = mSitesInput;
	        final ListView siteslist = mSitesList;
	        sitesinput.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
	                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                	input(sitesinput,siteslist);
	                	return true;
	                }
	                return false;
	            }
	        });
	        sitesinput.addTextChangedListener(new Twatcher((ListView)mSitesList));
	        //mSitesList.setOnTouchListener(new Swiper());
	        
	        mNewsList = createList(R.id.news_list,R.layout.list_news,
	        	"news",
	        	new String[] {"times", "sources", "text", "trust"},
	        	new int[] {R.id.news_time, R.id.news_source, R.id.news_text, R.id.news_trust},
	        	new Object[][] {},0,0,//trust_id and text_id aren't used
	        	mNewsInput = (EditText)findViewById(R.id.news_input)
	        	);	
	        mNewsInput.addTextChangedListener(new Twatcher((ListView)mNewsList));
	        //mNewsList.setOnTouchListener(new Swiper());
	        
	        /*
	        //TODO: http://www.ezzylearning.com/tutorial/handling-android-listview-onitemclick-event
	        mNewsList.setOnItemClickListener(new OnItemClickListener()
	        {
	           @Override
	           public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) 
	           {
	                 String value = (String)adapter.getItemAtPosition(position); 
	                 // assuming string and if you want to get the value on click of list item
	                 // do what you intend to do on click of listview row
		    	     Log.d(LOG_TAG,"Touch:"+value);
	           }
	        });
	        */

	        //TODO
		   /*
	        mPeersList = PeersAdapter.create(this);
	        mPeersInput = (EditText)findViewById(R.id.peers_input);
	        mPeersInput.addTextChangedListener(new Twatcher((ListView)mPeersList));
	        */

	        mTalksLog = (TextView) findViewById(R.id.talks_log);
	        mTalksLog.setMovementMethod(new ScrollingMovementMethod());
	        /*
	        //TODO:20150418 - that was working
	        mTalksLog.setFocusable(false);//TODO: make selectable?
	        mTalksLog.setClickable(false);//TODO: make selectable?   
	        */
	        //TODO:make click and context menu with cleanup and selection possible

	        mTalksInput = (EditText) findViewById(R.id.talks_input);
	        mTalksInput.setHorizontallyScrolling(true);
	        ((ImageButton) findViewById(R.id.say)).setOnClickListener(mTalksSayListener);
	        //((ImageButton) findViewById(R.id.del)).setOnClickListener(mTalksClearListener);        
	        final EditText talksinput = mTalksInput;
	        talksinput.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
	                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                	input(talksinput);
	                	return true;
	                }
	                return false;
	            }
	        });
	        
	        //register to handle all menus
	        registerForContextMenu(mThingsList);
	        registerForContextMenu(mSitesList);
	        registerForContextMenu(mNewsList);
	        //registerForContextMenu(mPeersList);
	        registerForContextMenu(mTalksLog);
	        
	        getTabHost().setCurrentTab(2);//select News by default

	        Log.d(LOG_TAG,"Interface created");

	        //TODO: don't stop service on exit activity
	        //http://stackoverflow.com/questions/4225847/why-remote-service-is-destroyed-when-main-activity-is-closed
	        //Intent bindIntent = new Intent(this,AigentsService.class);
	        //ComponentName cn = startService(bindIntent);	        
	     	mConnection = new ServiceConnection() {
	    		@Override
	    		public void onServiceConnected(ComponentName name, IBinder binder) {
	    			mService = ((AigentsService.AigentsBinder)binder).getService();
//TODO:make this working upon activity resume 
	    			mTalker = new Talker(AigentsTabActivity.this,mService.mCell);
	    			while (pendingRequests.size() > 0) {
	    				mTalker.request(Talker.noEcho, pendingRequests.poll());
	    			}
	    	        Log.d(LOG_TAG,"connected");
	    		}
	    		@Override
	    		public void onServiceDisconnected(ComponentName name) {
	    	        mService = null;
	    	        Log.d(LOG_TAG,"disconnected");
	    		}
	    	};	
	 
	    	Intent serviceIntent = new Intent(this,AigentsService.class);
	        ComponentName started = startService(serviceIntent);
	        Log.d(LOG_TAG,"Service started "+started);
	        //boolean bound = bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);//if not started explicitly above
	        boolean bound = bindService(serviceIntent, mConnection, 0);
	        Log.d(LOG_TAG,"Service bound "+bound);
	                
        } catch (Exception e) {
        	String s=e.toString();
        	Log.e(LOG_TAG, s, e);
        }        
    }

    void refreshTab(int tab) {
    	ListView list = tab == 0 ? mThingsList : tab == 1 ? mSitesList : 
    		tab == 2 ? mNewsList : tab == 3 ? mPeersList : null;
    	EditText input = tab == 0 ? mThingsInput : tab == 1 ? mSitesInput : 
    		tab == 2 ? mNewsInput : tab == 3 ? mPeersInput : null;
    	if (list != null && mTalker != null) {
    		DataRequestor da = (DataRequestor)list.getAdapter();
    		if (da != null) {
        		String request = da.updateRequest();
    			mTalker.request(da, request);
    		}    		
    	}
    	if (input != null)
    		input.setText("");
    	if (mAnimation != null && !mAnimation.isRunning())
    		mAnimation.start();
    }
    
    public ListView createList(int id,int resource,String type,String[] names,int[] fields,Object[][] data, int trust_id, int text_id, EditText filter) {
        ListView listview = (ListView) findViewById(id);
        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < data.length; i++)
        	mylist.add(DataAdapter.newItem(names,data[i]));
        //https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/layout/simple_list_item_2_single_choice.xml
        DataAdapter la = type.equals("news") ? 
        	new NewsDataAdapter(this, mylist, resource, names, fields, type, filter) :
        	new TrustDataAdapter(this, mylist, resource, names, fields, type, trust_id, text_id, filter);
        listview.setAdapter(la);
        //listview.setItemsCanFocus(false);//TODO:why?
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);        
        return listview;
    }
    
    String getMenuInfoString(ContextMenuInfo menuInfo,int resource) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        CharSequence cs = (CharSequence) ((TextView) info.targetView.findViewById(resource)).getText();
    	return cs != null ? cs.toString() : "";
    }

    boolean getMenuInfoCheck(ContextMenuInfo menuInfo,int resource) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        return ((CheckBox) info.targetView.findViewById(resource)).isChecked();
    }
    
    /*
    public void onClickEvent(View sender)
    {
    	Log.d(LOG_TAG, "click");
        //registerForContextMenu(sender); 
        //openContextMenu(sender);
        //unregisterForContextMenu(sender);
    }
    */
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        String[] menuItems = new String[]{};
	    if (v.getId() == R.id.things_list) {
	        menu.setHeaderIcon(R.drawable.things);
	        menu.setHeaderTitle(getMenuInfoString(menuInfo,R.id.things_name));
	        menuItems = new String[] {getString(R.string.edit),getString(R.string.del)};
		}
	    if (v.getId() == R.id.sites_list) {
	        menu.setHeaderIcon(R.drawable.sites);
	        menu.setHeaderTitle(getMenuInfoString(menuInfo,R.id.sites_name));
		    menuItems = new String[] {getString(R.string.open),getString(R.string.edit),getString(R.string.del)};
		}
	    if (v.getId() == R.id.peers_list) {
	    	//TODO:
	        menu.setHeaderIcon(R.drawable.folks_gb);
	        menu.setHeaderTitle(getMenuInfoString(menuInfo,R.id.peers_name));
	        menuItems = new String[] {getString(R.string.open),getString(R.string.edit)};
		}
	    if (v.getId() == R.id.news_list) {
	        menu.setHeaderIcon(R.drawable.news);
	        menu.setHeaderTitle(getMenuInfoString(menuInfo,R.id.news_text));
	        menuItems = //getMenuInfoCheck(menuInfo,R.id.news_trust) ?
	        	//new String[] {getString(R.string.open)}:
	        	new String[] {getString(R.string.open),getString(R.string.del)};
		}
	    if (v.getId() == R.id.talks_log) {
	        menu.setHeaderIcon(R.drawable.talks);
	        menu.setHeaderTitle(getString(R.string.talks));
	        menuItems = new String[] {getString(R.string.clear)};
		}
        for (int i = 0; i<menuItems.length; i++)
        	menu.add(Menu.NONE, i, i, menuItems[i]);
    } 
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	int index = item.getItemId();
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	if (info == null) {//TODO:clean hack - this is talks/log menu
    		if (index == 0)
    			mTalksLog.setText("");
    		return true;
    	}
    	ViewParent v = info.targetView.getParent();
    	if (v instanceof ListView) {
    		ListView lv = (ListView)v;
            if (lv.getId() == R.id.things_list ) {
		    	DataAdapter a = (DataAdapter)lv.getAdapter();
    	        String t = getMenuInfoString(info,R.id.things_name);
    		    if (index == 0) {
    		    	new PropertiesDialog(this, R.drawable.things, R.string.thing, 
    		    		"name "+Writer.toString(t), fields_name, null, new String[]{t}, true, a, index).show();
    		    } else
    		    if (index == 1)
    		    	a.delete(info.position);
     	    	return true;
            }
            if (lv.getId() == R.id.sites_list ) {
		    	DataAdapter a = (DataAdapter)lv.getAdapter();
    	        String t = getMenuInfoString(info,R.id.sites_name);
    		    if (index == 0) {
    	    		mTalker.request(Talker.noEcho,"You read site "+t+"!"); 
    		    	//startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(t)));
    		    	WebViewer.open(this, t, null, null);
    		    }
    		    else
    		    if (index == 1)
    		    	new PropertiesDialog(this,R.drawable.sites, R.string.site, 
    		    		"name "+t, fields_name, null, new String[]{t}, true, a, index).show();
    		    else
    		    if (index == 2)
    		    	a.delete(info.position);
    	    	return true;
            }
            if (lv.getId() == R.id.peers_list ) {
    		    SimpleCursorAdapter a = (SimpleCursorAdapter)lv.getAdapter();
    		    Cursor c = (Cursor)a.getItem(info.position);
    		    String uid = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
    		    Intent intent = new Intent(index == 0 ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
     		    intent.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, uid));
    		    this.startActivity(intent);
    		    return true;
            }
            if (lv.getId() == R.id.news_list ) {
		    	DataAdapter a = (DataAdapter)lv.getAdapter();
    	        String site = getMenuInfoString(info,R.id.news_source);
    	        String time = getMenuInfoString(info,R.id.news_time);
    	        String text = getMenuInfoString(info,R.id.news_text);
    		    if (index == 0)
    		    	//startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(t)));
		    		WebViewer.open(this, site, time, text);
    		    else
    		    if (index == 1)
    		    	a.delete(info.position);
    	    	return true;
            }
    	}
    	return false;
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,0,0,R.string.your_props).setIcon(R.drawable.folks_bb);
        menu.add(0,1,0,R.string.my_props).setIcon(R.drawable.aigent_btn);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override  
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
            case 0:
            	peerProperties();
            	return true;       
            case 1:  
            	selfProperties();
                return true;     
            default:  
                return super.onOptionsItemSelected(item);  
        }  
    }
    
    OnClickListener mTalksClearListener = new OnClickListener() {
        public void onClick(View v) {
            mTalksLog.setText("");
        }
    };
    
    OnClickListener mTalksSayListener = new OnClickListener() {
        public void onClick(View v) {
        	input(mTalksInput);
        }
    };
    
    OnClickListener mThingsAddListener = new OnClickListener() {
        public void onClick(View v) {
        	input(mThingsInput,mThingsList);
        }
    };
    
    OnClickListener mSitesAddListener = new OnClickListener() {
        public void onClick(View v) {
        	input(mSitesInput,mSitesList);
        }
    };
    
        
    void selfProperties() {
    	new PropertiesDialog(this, R.drawable.aigent_btn, R.string.my_props, "your", 
    		Body.strings, null, null, true, null, 0).show();
    }
    
    void peerProperties() {
    	String[] fields = new String[]{
        	AL.name,Peer.surname,Peer.birth_date,AL.email,
        	Peer.secret_question,Peer.secret_answer,/*Peer.phone,*/Peer.check_cycle};
    	int[] resources = {R.string.name,R.string.surname,R.string.birth_date,R.string.email,
    		R.string.secret_question,R.string.secret_answer,R.string.check_cycle};
    	new PropertiesDialog(this, R.drawable.folks_bb, R.string.your_props, "my", fields, resources, null, true, null, 0).show();
    }
    
    void input(EditText input) {
    	String text = input.getText().toString();
    	if (text.length() > 0) {
 	        input.setText("");
    		outputPeerText(text);
    		mTalker.input(text);
    	} 
    	else {
    		Context context = input.getContext();
        	//Toast toast = Toast.makeText(context, context.getText(R.string.hint_enter_text), Toast.LENGTH_SHORT);
        	//toast.setGravity(Gravity.BOTTOM|Gravity.LEFT, 0, 0);
        	//toast.show();
    		displayStatus(context.getText(R.string.hint_enter_text).toString());
    	}
    }

    void input(EditText input, ListView listview) {
    	CharSequence text = input.getText();
    	if (text.length() > 0) {
	    	DataAdapter a = (DataAdapter)listview.getAdapter();
	    	String t = input.getText().toString();
	    	if (listview == this.mSitesList) {
	    		//a.data.add(DataAdapter.newItem(new String[]{"site","check"},new Object[]{t,false}));
		   		//a.notifyDataSetChanged();
        		mTalker.request(a,a.addRequest(t));
	    	}
	    	else
		    if (listview == this.mThingsList) {
        		mTalker.request(a,a.addRequest(t));
		    }
	        input.setText("");
    	} 
    	else {
    		Context context = input.getContext();
        	//Toast toast = Toast.makeText(context, context.getText(R.string.hint_enter_text), Toast.LENGTH_SHORT);
        	//toast.setGravity(Gravity.BOTTOM|Gravity.LEFT, 0, 0);
        	//toast.show();
    		displayStatus(context.getText(R.string.hint_enter_text).toString());
    	}
    }

    //TODO: avoid memory overrun, but also avoid performance penalty
	//synchronized 
	void log(String text) {
		if (mTalksLog.length() > MAX_LOG) //half-truncate 
			mTalksLog.setText(mTalksLog.getText().subSequence(MAX_LOG/2, mTalksLog.length()));
		else
			mTalksLog.append(text);
	}

	public static void displayStatus(Context context, String text) {
		displayStatus(context, text, true);
	}
	
	public static void displayStatus(Context context, String text, boolean lenghthly) {
    	Toast toast = Toast.makeText(context, text, lenghthly ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.BOTTOM|Gravity.LEFT, 0, 0);//TODO: where?
    	toast.show();
	}
	
	@Override
	public void displayStatus(String text) {
		displayStatus(this, text);
	}

	@Override
	public void outputPeerText(String text) {
		log("\n>"+Writer.capitalize(new StringBuilder(text)));
	}

	@Override
	public void outputSelfText(String text) {
		log("\n"+Writer.capitalize(new StringBuilder(text)));
	}

	@Override
	public void enableWork() {
		refreshTab(getTabHost().getCurrentTab());
		// TODO what else?
	}

	@Override
	public void editProperties(String qualifier, String[] fields, boolean fill) {
	    new PropertiesDialog(this,R.drawable.folks_bb, R.string.your_props, 
	    	qualifier, fields, null, null, fill, null, 0).show();
	}

	//http://developer.android.com/reference/android/app/Notification.html
    //http://stackoverflow.com/questions/8835518/displaying-an-icon-in-status-bar-when-my-task-is-running
	//http://developer.alexanderklimov.ru/android/notification.php
	//http://stackoverflow.com/questions/2424488/android-new-intent-starts-new-instance-with-androidlaunchmode-singletop
	private static int NOTIFICATION_ID = 987654321; //Any unique number for this notification
	private static int mPendingCount = 0; 
	static void displayPending(Context context, Class<?> cls, int count) {
	    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (count > 0) { //on any non-zero count, update status
		    CharSequence info = String.valueOf(count)+" "+context.getText(R.string.aigents_news);
		    CharSequence text = context.getText(R.string.follow_aigents);
		    /*
		    Notification notification = new Notification(R.drawable.aigent_btn, info, System.currentTimeMillis());
		    //Intent intent = new Intent(this, AigentsTabActivity.class);
		    Intent intent = new Intent(context, cls);
		    intent.putExtra("tab", "news");
		    PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);// PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setLatestEventInfo(context, info, text, pending);
			*/
//TODO
			//https://stackoverflow.com/questions/32345768/cannot-resolve-method-setlatesteventinfo/33085754
			Intent intent = new Intent(context, cls);
			intent.putExtra("tab", "news");
			PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);// PendingIntent.FLAG_UPDATE_CURRENT);
			Notification.Builder builder = new Notification.Builder(context);
			builder.setAutoCancel(false);//
			builder.setTicker(text);
			builder.setContentTitle(info);
			builder.setContentText(text);
			builder.setSmallIcon(R.drawable.aigent_btn);
			builder.setContentIntent(pending);
			builder.setOngoing(true);//
			//?builder.setSubText("This is subtext..."); //  //API level 16
			builder.setNumber(100);//TODO?
			builder.build();
			Notification notification = builder.getNotification();

		    if (count > mPendingCount) { //sound only if more counted 
		    	//http://stackoverflow.com/questions/4441334/how-to-play-an-android-notification-sound
		    	//TODO: return default sound back once problem with false notifications is fixed 
		    	//notification.defaults |= Notification.DEFAULT_SOUND; 
		    	//TODO:later - fix problems with volume and quality
		    	/*
		    	try {
		    		String RES_PREFIX = "android.resource://net.webstructor.android/";
			    	MediaPlayer mp = new MediaPlayer();
			    	//MediaPlayer mp = MediaPlayer.create(this, R.raw.okay);
			    	mp.reset();
			    	mp.setDataSource(getApplicationContext(),Uri.parse(RES_PREFIX + R.raw.okay));
					mp.prepare();
			    	mp.start();		    	
				} catch (Exception e) {
					Log.e(LOG_TAG,"Notification failed",e);
					notification.defaults |= Notification.DEFAULT_SOUND;					
				}
				*/
		    }
		    nm.notify(NOTIFICATION_ID, notification);		
		    mPendingCount = count;
		}
		else {
			nm.cancel(NOTIFICATION_ID);
		}
	}

}
