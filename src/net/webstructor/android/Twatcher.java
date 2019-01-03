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

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Filterable;
import android.widget.ListView;

//http://stackoverflow.com/questions/7117209/how-to-know-key-presses-in-edittext
class Twatcher implements TextWatcher {
	ListView listView;
	Twatcher(ListView listView) {
		this.listView = listView;
	}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {                                   
	                ((Filterable)listView.getAdapter()).getFilter().filter(s);
                }                       
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}   
                @Override
                public void afterTextChanged(Editable s) {}
}