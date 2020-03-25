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

import java.util.HashMap;

import net.webstructor.al.AL;
import net.webstructor.al.Writer;
//import net.webstructor.android.free.R;
import net.webstructor.android.R;

import android.app.Activity;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.util.Log;

class PeersAdapter extends SimpleCursorAdapter implements OnClickListener, DataRequestor {

//TODO:!!!
//Contacts explained:
//http://androidexample.com/Get_Contact_Emails_By_Content_Provider_-_Android_Example/index.php?view=article_discription&aid=121&aaid=141

	final static String[] peer_names = {
		AL.name,
		//"surname",
		//"email",
		AL.share //"trust"
	};
	final static int[] peers_ids = {
		R.id.peers_name, 
		//R.id.peers_surname, 
		//R.id.peers_email, 
		R.id.peers_trust
	};

	//http://stackoverflow.com/questions/5322412/listview-simplecursoradapter-an-an-edittext-filter-why-wont-it-do-anything
	static Cursor getContacts(Activity activity, CharSequence pattern) {
		String[] PROJECTION = new String[] {
	    	ContactsContract.Contacts._ID,
	    	ContactsContract.Contacts.DISPLAY_NAME,
	        ContactsContract.Contacts.HAS_PHONE_NUMBER
	    };
	    String filter = null;
	    //http://stackoverflow.com/questions/5322412/listview-simplecursoradapter-an-an-edittext-filter-why-wont-it-do-anything
		if (pattern != null)
			//filter = "lower("+ContactsContract.Contacts.DISPLAY_NAME + ") LIKE '%"+pattern.toString().toLowerCase(Locale.getDefault())+"%' COLLATE NOCASE";
			//filter = "lower("+ContactsContract.Contacts.DISPLAY_NAME + ") LIKE '%"+pattern.toString().toLowerCase(Locale.getDefault())+"%'";
			//filter = ContactsContract.Contacts.DISPLAY_NAME + " LIKE '%"+pattern+"%' COLLATE NOCASE";
			filter = ContactsContract.Contacts.DISPLAY_NAME + " LIKE '%"+pattern+"%' COLLATE NOCASE";
	    Cursor c = activity.managedQuery(
	    	ContactsContract.Contacts.CONTENT_URI, PROJECTION, filter, null, ContactsContract.Contacts.DISPLAY_NAME+" ASC");
	    	//ContactsContract.Contacts.CONTENT_FILTER_URI, PROJECTION, filter, null, null);
	    return c;
	}
		
    //this works, but no email is displayed
	public static ListView create(final AigentsTabActivity activity) {
 	    PeersAdapter adapter = new PeersAdapter(activity,
	        R.layout.list_peers, getContacts(activity,null),
	            new String[] { 
		            ContactsContract.Contacts.DISPLAY_NAME,
		            //ContactsContract.Contacts.HAS_PHONE_NUMBER,
	        	}, 
	        new int[] { 
	    		R.id.peers_name, 
	    		//R.id.peers_surname
	    		//,
	    		//R.id.peers_email
	    	}
	    );
       ListView peers = (ListView) activity.findViewById(R.id.peers_list);
       peers.setAdapter(adapter);
       peers.setItemsCanFocus(false);//TODO:why?
       peers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

       adapter.setFilterQueryProvider(new FilterQueryProvider() {
           public Cursor runQuery(CharSequence constraint) {
               return getContacts(activity,constraint);
           }
       });

       return peers;
	}
	
	/*
	//this works with email but start very slowly
   	public static ListView create9(AigentsTabActivity activity) {
   		//TODO:get rid of this parameter
        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        ListView peers = (ListView) activity.findViewById(R.id.peers_list);
        ContentResolver cr = activity.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor cur1 = cr.query( 
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", 
                                new String[]{id}, null); 
                while (cur1.moveToNext()) { 
                    //to get the contact names
                    String name=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    Log.e(AigentsTabActivity.LOG_TAG, "Name:"+name);
                    String email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    Log.e(AigentsTabActivity.LOG_TAG,"Email:"+email);
                    if(email!=null){
                    	mylist.add(DataAdapter.newItem(peer_names, new Object[]{name,email,"dummy",true}));	
                    }
                } 
                cur1.close();
            }
        }
        DataAdapter la = new DataAdapter(activity, mylist, R.layout.list_peers, peer_names, peers_ids, "peers" );
        peers.setAdapter(la);
        peers.setItemsCanFocus(false);//TODO:why?
        peers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        return peers;
	}
	*/
   	

	//private static final String[] names = {"name","trust"};
	HashMap<String, Boolean> trusts = new HashMap<String, Boolean>();
	AigentsTabActivity activity;
	Cursor cursor;
	PeersAdapter(AigentsTabActivity context, int layout, Cursor cursor, String[] from, int[] to) {		
		super(context, layout, cursor, from, to);
		this.activity = context;
		this.cursor = cursor;
	}

	//"official horrible hack" - attaching handlers to each of checkboxes
	//http://schimpf.es/listview-with-checkboxes-inside/
	//http://stackoverflow.com/questions/5530778/how-to-capture-the-checkbox-event-in-listview
	//http://www.vogella.com/tutorials/AndroidListView/article.html
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View v = super.getView(position, view, viewGroup); 
        CheckBox cb = (CheckBox) v.findViewById(R.id.peers_trust);
        TextView tv = (TextView) v.findViewById(R.id.peers_name);
    	if (view == null) {//if first time on this view position
    		cb.setOnClickListener(this);
    		tv.setOnClickListener(this);
    	}
    	cb.setTag(Integer.valueOf(position));    	
    	CursorWrapper cw = (CursorWrapper)getItem(position);
        //String name=cw.getString(cw.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        String name=cw.getString(cw.getColumnIndex(ContactsContract.Contacts._ID));
        if (name != null) {
        	Boolean check = trusts.get(name.toLowerCase());
        	//if (name.equals("95"))
        		//Log.d(AigentsTabActivity.LOG_TAG,"check="+check+" id="+name);
        	cb.setChecked(check != null && check.booleanValue());    	
        }
    	return v;
    }

	@Override
	public void onClick(View v) {
    	if (v instanceof CheckBox) {
    		int position = ((Integer)v.getTag()).intValue();
    		CursorWrapper cw = (CursorWrapper)getItem(position);
    		boolean check = ((CheckBox)v).isChecked();
    		//String value = check ? "true" : "false";
            String name=cw.getString(cw.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String id=cw.getString(cw.getColumnIndex(ContactsContract.Contacts._ID));
            String phone = phone(id);
            String email = email(id);
    		//Log.d(AigentsTabActivity.LOG_TAG,"check="+value+" object="+name+" id="+id);
            if (AL.empty(name))
            	Log.e(AigentsTabActivity.LOG_TAG, "Unnamed contact.");
            else
    		//if unchecked, remove from the peers
    		if (!check) {
    			//String request = "is peer, name "+Writer.toString(name)+" trust false, share false.";
    			//activity.mTalker.request(null,request);
    			//StringBuilder request = new StringBuilder("is peer, id "+id+" name ");
    			StringBuilder request = new StringBuilder("id "+id+", is peer name ");
    			Writer.quote(request,name).append(", trust false, share false.");
    			activity.mTalker.request(Talker.noEcho,request.toString());
				activity.displayStatus(Writer.toString(name)+activity.getString(R.string._is_not_getting_your_news));
    		} else {
        		//if checked
    			if (AL.empty(phone) && AL.empty(email)) {
            		// - if has neither phone nor email, display toast and talk entry and uncheck
    				((CheckBox)v).setChecked(false);
    				notifyDataSetChanged();
    				activity.displayStatus(Writer.toString(name)+activity.getString(R.string._has_no_email_no_phone));
    			} else {
            		// - if has phone or email, add to peers
    				StringBuilder request = new StringBuilder();
    				/**
    				if (trusts.get(name.toLowerCase()) != null) { //if existing peer
    					request.append("is peer, name ");
    					Writer.toString(request, name);    					
       					request.append(" trust true, share true");
        				request.append(", email ");
        				Writer.toString(request, email == null ? "\'\'" : email);
        				request.append(", phone ");
       					Writer.toString(request, phone == null ? "\'\'" : phone);
    				} else { // if new peer
    					request.append("there is peer, name ");
    					Writer.toString(request, name);
    					request.append(",");
       					request.append(" trust true, share true");
        				request.append(", email ");
        				Writer.toString(request, email == null ? "\'\'" : email);
        				request.append(", phone ");
       					Writer.toString(request, phone == null ? "\'\'" : phone);
    				}
    				**/
    				/**/
    				if (trusts.get(id) != null) { //if updating existing peer
    					//request.append("is peer, id "+id+" name ");
    					request.append("id "+id+", is peer name ");
    				}
    				else { //if creating new peer
    					request.append("there is peer, id "+id+", name ");
    				}
    				Writer.quote(request, name).append(", trust true, share true");
        			request.append(", email ");
        			Writer.toString(request, email == null ? "\'\'" : email);
        			request.append(", phone ");
       				Writer.toString(request, phone == null ? "\'\'" : phone);
   					request.append(".");
        			activity.mTalker.request(null,request.toString());
    				activity.displayStatus(Writer.toString(name)+activity.getString(R.string._is_getting_your_news));
    			}
    		}   		
    	}
    	else
    	if (v instanceof TextView) {
    		activity.openContextMenu(v);
    	}
	}	
	
	//http://tausiq.wordpress.com/2012/08/23/android-get-contact-details-id-name-phone-photo/
   	String phone(String contactID) {
   		String contactNumber = null;
        Cursor cursorPhone = activity.getContentResolver()
        	.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                        ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                new String[]{contactID},
                null);
        if (cursorPhone.moveToFirst())
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        cursorPhone.close();
        return contactNumber;
   	}
   	
   	String email(String contactID) {
   		String contactEmail = null;
        Cursor cursor = activity.getContentResolver()
        	.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactID},
                null); 
        if (cursor.moveToFirst())
            contactEmail = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA)); 
        cursor.close();
        return contactEmail;
   	}

	@Override
	public String updateRequest() {
		//return "what is peer name, share?";	
		return "what is peer id, share?";	
	}

	@Override
	public void update(String string) {
		trusts.clear();
		//Object[][] data = AL.parseToGrid(string,peer_names,",");
		Object[][] data = AL.parseToGrid(string,new String[]{AL.id,AL.share},",");
		for (int i = 0; i < data.length; i++)
			if (!AL.empty((String)data[i][0]))
				trusts.put(((String)data[i][0]).toLowerCase(), (Boolean)data[i][1]);
		notifyDataSetChanged();		
	}

	@Override
	public String type() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
	}

	@Override
	public String addRequest(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String deleteRequest(int position) {
		// TODO Auto-generated method stub
		return null;
	}
   	
}