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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import net.webstructor.al.AL;
import net.webstructor.al.Time;
import net.webstructor.al.Writer;
import net.webstructor.android.R;
import net.webstructor.util.Array;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

class DataComparator implements Comparator<String> {
	public int compare(String a, String b) {
		try {
			return a.compareTo(b);
		} catch (Exception e) {} //oops, relax
		return 0;
	}
	public boolean equals(Object obj) {		
		return obj instanceof NewsDataComparator;
	}
}

abstract class DataAdapter extends SimpleAdapter implements DataRequestor {
	protected Object[][] values = null; //cached original data from backend
	ArrayList<HashMap<String, Object>> data; //data to render after filtering
	String[] names; 
	String type;
	AigentsTabActivity activity;
	EditText filter;
	DataAdapter(AigentsTabActivity activity, ArrayList<HashMap<String, Object>> data, 
			int resource, String[] names, int[] to, String type, EditText filter) {
		super(activity,data,resource,names,to);
		this.activity = activity;
		this.data = data;
		this.type = type;
		this.names = names;
		this.filter = filter;
	}

	abstract protected Comparator<HashMap<String, Object>> getComparator();
	
	protected static int indexInListView(View v) {
		for (;;) {
			ViewGroup parent = (ViewGroup)v.getParent();
			if (parent == null)
				break;;
			if (parent instanceof ListView)
				return parent.indexOfChild(v);
			v = parent;
		}
		return -1;
	}
	
    public static HashMap<String, Object> newItem(String[] names, Object[] values) {
    	HashMap<String, Object> map = new HashMap<String, Object>();
    	for (int j = 0; j < names.length; j++)
    		map.put(names[j], values[j]);
    	map.put("obj",values);
    	return map;
    }
  
    public void delete(int position) {
    	activity.mTalker.request(Talker.noEcho,deleteRequest(position));  
    	remove(position);
    }
    
	public void update() {
		if (filter.getText().length() > 0)
			getFilter().filter(filter.getText());
		else {
			data.clear();
			for (int i = 0; i < values.length; i++)
				data.add(DataAdapter.newItem(names,values[i]));
			//Collections.sort(data,getComparator());
	    	notifyDataSetChanged();
		}
	}

	@Override
	public Filter getFilter() {
		return new Filter () {
			@SuppressLint("DefaultLocale")
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				String filter = constraint == null ? null : constraint.toString().toLowerCase(Locale.getDefault());
				FilterResults result = new FilterResults();
				ArrayList<HashMap<String, Object>> filtered = new ArrayList<HashMap<String, Object>>();
				if (values != null)
					for (int i = 0; i < values.length; i++)
			        	if (filter == null || filterPassed(values[i],filter))
			        		filtered.add(DataAdapter.newItem(names,values[i]));
				//Collections.sort(filtered,getComparator());
	            result.values = filtered;
	            result.count = filtered.size();
	            return result;
	        }
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
		        data.clear();
		        if (results.count > 0) {
			        data.addAll((ArrayList<HashMap<String, Object>>) results.values);
		            notifyDataSetChanged();
		        } else {
		            notifyDataSetInvalidated();
		        }   
			} 
		};
	}

	static boolean filterPassed(Object[] fields, CharSequence filter) {
		Locale locale = Locale.getDefault();
		if (filter != null) {
			for (int i = 0; i < fields.length; i++) {
				Object obj = fields[i];
				if (obj instanceof String && ((String)obj).toLowerCase(locale).contains(filter))
					return true;
			}
			return false;
		}
		return true;
	}

	/*
	void remove(Object[] data) {
		for (int i = 0; i < this.values.length; i++)
			if (this.values[i] == data) {
				Object[][] temp = new Object[this.values.length - 1][];
				//copy everything before i
				for (int j = 0; j < i; j++)
					temp[j] = values[j];
				//copy everything after i
				for (int k = i + 1; k < this.values.length; k++)
					temp[k-1] = values[k];
				this.values = temp;
			}
	}
	*/
	
    //quick remove not waiting for refresh
    void remove(int pos) {
    	synchronized (this) {
    		//Object[] temp = Array.remove(values,data.get(pos).get("obj"));
    		//values = (Object[][])temp;
    		values = Array.remove(values,(Object [])data.get(pos).get("obj"));
    	}
    	data.remove(pos);//remove from filtered and sorted display data
    	notifyDataSetChanged();//no re-sorting is needed
    }
    
	@Override
	public String type() {
		return type;
	}

	@Override
	public void clear() {
		data.clear();
    	notifyDataSetChanged();
	}
	
	@Override
	public void update(String string) {
		synchronized (this) {
			values = AL.parseToGrid(string,Array.toLower(names),",");
		}
		update();
	}	

	@Override
	public String addRequest(String str) {
		return AL.empty(str) ? null : "My "+type()+" "+Writer.toString(str)+". "+Writer.toString(str)+" trust true.";	    
	}

	@Override
	public String updateRequest() {
		return "What my "+type()+"?";	    
	}

	String objectQualifier(HashMap<String, Object> o) {
		return (String)o.get(names[0]);//name as first column
	}
	
	@SuppressLint("DefaultLocale")
	void buildKey(StringBuilder sb, HashMap<String, Object> o) {
		/**/
		for (int c = 0, keycount = 0; c < names.length; c++) {
			String name = names[c].toLowerCase();
			if (!name.equals(AL.trust)) {
				String val = (String)o.get(name);
				if (!AL.empty(val)) {
					if (keycount > 0)
						sb.append(AL.space).append("and").append(AL.space);
					keycount++;
					sb.append(name).append(AL.space);
					if (name.equals(AL.text))
						Writer.quote(sb,val);//encode texts unconditionally
					else
						Writer.toString(sb,val);
				}
			}
		}
		/**/
		//TODO: see if it works for NOT quoting if (name.equals(AL.text))
		//sb.append(AL.buildQualifier(names, o));
	}
	
	@Override
	public String deleteRequest(int position) {
		HashMap<String, Object> o = data.get(position);
		if (o != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("My ").append(type()).append(AL.space);
			//for (int i=0; i<names.length; i++) {
				//if (i > 0)
				//	sb.append(AL.lister[0]).append(AL.space);
				sb.append(AL.not[1]).append(AL.space);
				Writer.toString(sb,objectQualifier(o));        				
			//}
			sb.append(AL.period);
//TODO: remove thing itself, not knows link only or do GC on given thing otherwise
			return sb.toString();
		}
		return null;
	}
	
	String editRequest(HashMap<String, Object> o, String name, String value) {
		StringBuilder sb = new StringBuilder();
		buildKey(sb,o);
		sb.append(AL.space);
		sb.append(name).append(AL.space);
		Writer.toString(sb,value);
		sb.append(AL.period);
        return sb.toString();
	}

	public static int compare(Boolean a, Boolean b) {
		return a == b ? 0 : a ? 1 : -1; 
	}
}

class TrustDataComparator implements Comparator<Object[]> {
	public int compare(Object[] a, Object[] b) {
		try {
			//trusted - to top, untrusted - to bottom
			int trust = -DataAdapter.compare((Boolean)a[1],(Boolean)b[1]); 
			if (trust != 0) //by trust
				return trust;
			return ((String)a[0]).compareTo((String)b[0]);
		} catch (Exception e) {} //oops, relax
		return 0;
	}
	public boolean equals(Object obj) {		
		return obj instanceof TrustDataComparator;
	}
}

class TrustDataAdapter extends DataAdapter implements OnClickListener {
	private int trust_id;
	private int text_id;

	class TrustDataMapComparator implements Comparator<HashMap<String, Object>> {
		@Override
		public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
			try {
				//trusted - to top, untrusted - to bottom
				int trust = -DataAdapter.compare((Boolean)a.get(names[1]),(Boolean)b.get(names[1])); 
				if (trust != 0) //by trust
					return trust;
				return ((String)a.get(names[0])).compareTo((String)b.get(names[0]));
			} catch (Exception e) {} //oops, relax
			return 0;
		}
		public boolean equals(Object obj) {		
			return obj instanceof TrustDataComparator;
		}
	}

	TrustDataAdapter(AigentsTabActivity activity, ArrayList<HashMap<String, Object>> data, 
			int resource, String[] names, int[] to, String type, int trust_id, int text_id, EditText filter) {
		super(activity,data,resource,names,to,type,filter);
		this.trust_id = trust_id;
		this.text_id = text_id;
	}

	@Override
	protected Comparator<HashMap<String, Object>> getComparator() {
		return new TrustDataMapComparator();
	}
	
	@Override
	public String updateRequest() {
		return "What my "+type()+" name, trust?";	    
	}

	@Override
	public void update() {
		//TODO:do we need sorting before filtering!?
    	synchronized (this) {
    		Arrays.sort(values,new TrustDataComparator());
    	}
		super.update();
	}
	
	//TODO: delete trusted with trust removal!!!???
	
	//"official horrible hack" - attaching handlers to each of checkboxes
	//http://schimpf.es/listview-with-checkboxes-inside/
	//http://stackoverflow.com/questions/5530778/how-to-capture-the-checkbox-event-in-listview
	//http://www.vogella.com/tutorials/AndroidListView/article.html
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View v = super.getView(position, view, viewGroup); 
        CheckBox cb = (CheckBox) v.findViewById(trust_id);
        TextView tv = (TextView) v.findViewById(text_id);
    	if (view == null) { //if first time on this view position
    		cb.setOnClickListener(this);
    		tv.setOnClickListener(this);
    	}
    	cb.setTag(data.get(position));
    	return v;
    }
    
	@SuppressWarnings("unchecked")
	@Override
	public void onClick(View v) {
    	if (v instanceof CheckBox) {
    		HashMap<String, Object> o = (HashMap<String, Object>)v.getTag();
    		String value = ((CheckBox)v).isChecked() ? "true" : "false";
    		//activity.mTalker.request(this,editRequest(o,AL.trust,value));
    		activity.mTalker.request(Talker.noEcho,editRequest(o,AL.trust,value));
    		((Object[])o.get("obj"))[1] = ((CheckBox)v).isChecked();
    		update();
    		//Log.d(AigentsTabActivity.LOG_TAG,"index="+index+" check="+value+" name="+o.get(AL.text));
    	}
    	else 
        if (v instanceof TextView) {
    		activity.openContextMenu(v);
    	}
	}	
}

class NewsDataComparator implements Comparator<Object[]> {
	public int compare(Object[] a, Object[] b) {
		try {
			//trust, times, sources, text
			int trust = DataAdapter.compare((Boolean)a[3],(Boolean)b[3]);//by trust 
			if (trust != 0)
				return trust;
			int time = Time.compare((String)a[0],(String)b[0]);//by time
			if (time != 0)
				return time;
			int src = ((String)a[1]).compareTo((String)b[1]);//by source
			if (src != 0)
				return src;
			return ((String)a[2]).compareTo((String)b[2]);//by text
		} catch (Exception e) {} //oops, relax
		return 0;
	}
	public boolean equals(Object obj) {		
		return obj instanceof NewsDataComparator;
	}
}

class NewsDataAdapter extends DataAdapter implements OnClickListener /*, OnTouchListener*/ {

	class NewsDataMapComparator implements Comparator<HashMap<String, Object>> {
		@Override
		public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
			try {
				//trust, times, sources, text
				int trust = DataAdapter.compare((Boolean)a.get(names[3]),(Boolean)b.get(names[3]));//by trust 
				if (trust != 0)
					return trust;
				int time = Time.compare((String)a.get(names[0]),(String)b.get(names[0]));//by time
				if (time != 0)
					return time;
				int src = ((String)a.get(names[1])).compareTo((String)b.get(names[1]));//by source
				if (src != 0)
					return src;
				return ((String)a.get(names[2])).compareTo((String)b.get(names[2]));//by text
			} catch (Exception e) {} //oops, relax
			return 0;
		}
		@Override
		public boolean equals(Object obj) {		
			return obj instanceof NewsDataMapComparator;
		}
	}
	
	
	NewsDataAdapter(AigentsTabActivity activity, ArrayList<HashMap<String, Object>> data, 
			int resource, String[] names, int[] to, String type,EditText filter) {
		super(activity,data,resource,names,to,type,filter);
	}
	
	@Override
	public String updateRequest() {
		return "What new true times, sources, text, trust?";
	}

	@Override
	protected Comparator<HashMap<String, Object>> getComparator() {
		return new NewsDataMapComparator();
	}
		
	@Override
	public void update() {
    	synchronized (this) {
    		Arrays.sort(values,new NewsDataComparator());
    	}
		super.update();
    	updatePending();
	}

    //while this is moved to Cell, we still need to update the number on re/checks
	public void updatePending() {
		int pending = 0;
		//for (int i = 0; i < data.size(); i++)
			//if (!(Boolean)data.get(i).get(AL.trust))
				//pending++;
		for (int i = 0; i < values.length; i++)
			if (!(Boolean)values[i][3])
				pending++;
		AigentsTabActivity.displayPending(activity,AigentsTabActivity.getActivityClass(),pending);
		//Log.d(AigentsTabActivity.LOG_TAG,"values="+values.length+", data="+data.size()+", unchecked="+pending);
	}
	
	@Override
	public String deleteRequest(int position) {
		HashMap<String, Object> o = data.get(position);
		StringBuilder sb = new StringBuilder();
//TODO: make insensitive to no/not/~ ?
//		sb.append(AL.not[1]).append(AL.space);
		//sb.append("No there ");
		buildKey(sb, o);
		//sb.append(" new false, trust false");//was: unreust on delete
		sb.append(" new false");//now: keep trust on delete so no GC is done
		sb.append(AL.period);
		return sb.toString();
	}
	
	//"official horrible hack" - attaching handlers to each of checkboxes
	//http://schimpf.es/listview-with-checkboxes-inside/
	//http://stackoverflow.com/questions/5530778/how-to-capture-the-checkbox-event-in-listview
	//http://www.vogella.com/tutorials/AndroidListView/article.html
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View v = super.getView(position, view, viewGroup); 
        CheckBox cb = (CheckBox) v.findViewById(R.id.news_trust);
        TextView tv = (TextView) v.findViewById(R.id.news_text);
    	if (view == null) { //if first time on this view position
    		cb.setOnClickListener(this);
    		tv.setOnClickListener(this);
    		//tv.setOnTouchListener(this);//TODO:for swipe
    	}
    	cb.setTag(data.get(position));        
    	return v;
    }

	@SuppressWarnings("unchecked")
	@Override
	public void onClick(View v) {
    	if (v instanceof CheckBox) {
    		//int index = indexInListView(v);
    		HashMap<String, Object> o = (HashMap<String, Object>)v.getTag();
    		String value = ((CheckBox)v).isChecked() ? "true" : "false";
    		activity.mTalker.request(Talker.noEcho,editRequest(o,AL.trust,value));
    		((Object[])o.get("obj"))[3] = ((CheckBox)v).isChecked();
    		update();
    		//Log.d(AigentsTabActivity.LOG_TAG,"index="+index+" check="+value+" name="+o.get(AL.text));
    	}
    	else
    	if (v instanceof TextView) {
    		//int index = indexInListView(v);
    		//Log.d(AigentsTabActivity.LOG_TAG,"tap "+index);
    		activity.openContextMenu(v);
    	}
	}

	/**
	//TODO: ensure it works
	private float eventX; 
	private static final float swipeX = 100;
	@Override
	public boolean onTouch(View v, MotionEvent event) {
	     switch(event.getAction()) {
	       case MotionEvent.ACTION_DOWN:
	    	   eventX = event.getX();                         
	           break; 
	       case MotionEvent.ACTION_MOVE:
	    	   if (onSwipe(v,event.getX()))
	    		   return true;
	           break;
	       case MotionEvent.ACTION_UP:
	    	   //if (onSwipe(v,event.getX()))
	    		 //  return true;
	           if (v instanceof TextView) {
	        	   onClick(v);
	        	   return true;
	           }
	     }           
	     return false;
	}
	
	private boolean onSwipe(View v, float x) {
        float dX = x - eventX;
        if (dX > swipeX) {
        	eventX = x;                         
     	   	activity.displayStatus("2right swipe "+Math.abs(dX));
     	   	return true;
        }
        if (dX < -swipeX) {
        	eventX = x;                         
	    	activity.displayStatus("2left swipe "+Math.abs(dX));
	    	return true;
        }
        return false;
	}
	**/
}
