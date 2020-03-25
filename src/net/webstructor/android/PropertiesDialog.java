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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PropertiesDialog implements DataRequestor {

	AigentsTabActivity activity;
	String qualifier;
	String[] names;
	int[] resources;
	String[] values;
	int iconid, titleid;
	LinearLayout ll;
	DataAdapter adapter;
	int position;
	
	PropertiesDialog(AigentsTabActivity activity, int iconid, int titleid, 
		String qualifier, String[] names, int[] resources, String[] values, boolean fill,
		DataAdapter adapter, int position) {
		this.activity = activity;
		this.iconid = iconid;
		this.titleid = titleid;
		this.qualifier = qualifier;
		this.names = names;
		this.resources = resources;
		this.values = values;
		this.adapter = adapter;
		this.position = position;//TODO: not needed, get rid of it?
		if (fill)
			activity.mTalker.request(this, updateRequest());
	}
	
    void show() {
    	try {
    		LayoutParams ll_params = 
    			new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
       		LayoutParams tv_params = 
        		new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    		final ScrollView sv = new ScrollView(activity);
	        sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	        sv.setPadding(20, 0, 20, 0);
	        ll = new LinearLayout(activity);
	        ll.setOrientation(LinearLayout.VERTICAL);
	        ll.setLayoutParams(ll_params);
	        for (int i = 0; i < names.length; i++) {
	        	TextView tv = new TextView(activity);
	        	tv.setTextColor(Color.LTGRAY);
	        	String label;
	        	if (resources != null) //if explicitly given resource ids
	        		label = activity.getString(resources[i]);
	        	else {
	        		String name = names[i].replace(' ', '_');
	        		int resource = activity.getResources().getIdentifier(name, "string", activity.getPackageName());
	        		if (resource > 0) //localize with resource 
	        			label = activity.getString(resource);
	        		else //capitalize as is
	        			label = Writer.capitalize(names[i]);
	        	}
	            tv.setText(label);	            
	            tv.setLayoutParams(tv_params);
	            tv.setGravity(Gravity.LEFT);
	            ll.addView(tv);
	        	EditText et = new EditText(activity);
	        	if (values != null && i < values.length && values[i] != null)
	        		et.setText(values[i]);
	        	//TODO: set password fields style 
	        	//if (names[i].indexOf("password") != -1) {
	        	//	et.setHint(Emailer.DEFAULT_PASSWORD);
	        	//	et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
	        	//}
	            et.setLayoutParams(tv_params);
	            et.setHorizontallyScrolling(true);
	            et.setGravity(Gravity.FILL_HORIZONTAL);
	            if (names.length == 1) {
	            	//http://stackoverflow.com/questions/15032870/create-a-multiline-edittext-programatically
	            	et.setSingleLine(false);   	
	            	et.setMaxLines(3);
	            }
	            else
	            	et.setSingleLine();
	            ll.addView(et);
	        }
	        sv.addView(ll);
	        View view = sv;	 
	        android.app.AlertDialog d = new AlertDialog.Builder(activity)
	            .setIcon(iconid)
	            .setTitle(titleid)
	            .setView(view)
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	//TODO:
	            		String request = changeRequest();
	                    if (request != null) {
	                    	/*
	                    	 before 20150426
	                    	activity.mTalker.request(null,request);
	                    	if (adapter != null)
	                    		activity.mTalker.request(adapter, adapter.updateRequest());
	                    		*/
	                    	activity.mTalker.request(adapter,request);//since 20150426 - autorefresh
	                    }
	                    else
	                    	;//TODO: show again or cancel closing!?
	                }
	            })
	            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	//TODO: nothing?
	                }
	            })
	            .create();
        	d.show();
        	//TODO: set dialog buttons?
        	//Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
        	//ok.setBackgroundResource(R.drawable.ok);
        	//Button cancel = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        	//cancel.setBackgroundResource(R.drawable.del);	
        } catch (Exception e) {
        	String s=e.toString();
        	Log.i("oops", s);
        }        
    }

	String changeRequest() {
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i = 0; i < names.length; i++) {
			int ii = i*2 + 1;
			View child = ll.getChildAt(ii);
			String value = ((EditText)child).getText().toString().trim();
			if (!AL.empty(value) && (values == null || !value.equals(values[i])))
				map.put(names[i],value);
		}
		return map.size() == 0 || qualifier == null ? null :
			qualifier + " " + AL.buildQualifier(names,map)+".";
	}
	
	@Override
	public String updateRequest() {
		return qualifier == null ? null : "What "+qualifier+" "+AL.propList(names)+"?"; 
	}

	@Override
	public String type() {
		return "properties";
	}
	
	@Override
	public void update(String string) {
		Object[][] data = AL.parseToSheet(string,names,",");
		values = new String[names.length]; 
		//TODO:
		//Arrays.sort(data,new StringArrayComparator());
		for (int i = 0; i < data.length; i++) {
			int ii = i*2 + 1;
			View child = ll.getChildAt(ii);
			values[i] = (String)data[i][1];
			((EditText)child).setText(values[i]);
		}
		//TODO: notfy?
		//fireTableDataChanged();
	}

	@Override
	public String addRequest(String str) {
		return "";//TODO:what else, null, exception?	    
	}

	@Override
	public String deleteRequest(int position) {
		return "";//TODO:what else, null, exception?	    
	}

	@Override
	public void clear() {
		//TODO: clean entry form fields		
	}
	
}

