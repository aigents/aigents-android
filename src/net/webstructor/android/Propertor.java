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

//TODO: make superclass of PropertiesDialog
public class Propertor implements DataRequestor {

	String qualifier;
	String[] names; 
	HashMap<String,String> map;
	
	Propertor(String qualifier, String[] names) {
		this(qualifier, names, null);
	}
	
	Propertor(String qualifier, String[] names, String[] values) {
		this.qualifier = qualifier;
		this.names = names;
		map = new HashMap<String,String>();
		if (!AL.empty(values))
			for (int i = 0; i < names.length; i++)
				if (!AL.empty(values[i]))
					map.put(names[i],values[i]);
	}
	
	String changeRequest() {
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
		for (int i = 0; i < data.length; i++) {
			if (!AL.empty((String)data[i][1]))
				map.put((String)data[i][0],(String)data[i][1]);
		}
		//notifyWaited();
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

	/*
	 //TODO: this does not work because Talker returns results in UI thread
	boolean waiting = false;
	public synchronized void waitNotified() throws InterruptedException {
		waiting = true;
    	while (waiting)
    		wait(1000);//TODO:what if timeout?
	}

	public synchronized void notifyWaited() {
		if (waiting) {
			waiting = false;
			notify();
		}
	}
	*/

}

