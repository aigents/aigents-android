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

import java.lang.reflect.Method;
import java.util.Stack;

import net.webstructor.al.AL;
import net.webstructor.al.Reader;
import net.webstructor.al.Writer;
import net.webstructor.android.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
 
public class WebViewer extends Activity {
 
	private WebView webView;
	private EditText webInput;
	String site, time, text;
	Stack<String> history = new Stack<String>();
	Class<?> activityClass;
	
	//TODO:Selection
	//TODO:Keeping history and going back
	
    @Override
    protected void onNewIntent (Intent intent) {
    	super.onNewIntent(intent);
    	//setIntent(intent);
    }
    
	@SuppressLint("SetJavaScriptEnabled")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web);

		activityClass = AigentsTabActivity.getActivityClass(this);
		
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		webView.getSettings().setBuiltInZoomControls(true);

		//http://stackoverflow.com/questions/3250034/android-webview-intercept-clicks
		//http://stackoverflow.com/questions/2378800/clicking-urls-opens-default-browser
		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url){
				setSite(site = url);
		    	history.push(url);
		    	return super.shouldOverrideUrlLoading(view, url);
		    }		    
			@Override
		    public void onLoadResource(WebView  view, String  url){
		    	super.onLoadResource(view, url);
		    }			
			@Override
			public void onPageFinished(WebView view, String url) {
				find();
				super.onPageFinished(view, url);
			}		
		});
		//webView.canGoBack();
		//webView.canGoForward();
		/*
		webView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {              
			        if (selection == null) {                       
			             selection = clipboard.getText().toString();    
			             webView.postDelayed(onClipBoard, 500);
			        }
			    }
			    return false;
			}		
		});
		*/
		
		webView.setOnLongClickListener(new View.OnLongClickListener() {
		    public boolean onLongClick(View arg0) {
		    	unfind();
		        return false;
		    }
		});
		
        webInput = (EditText) findViewById(R.id.web_text);
        webInput.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                	mAddListener.onClick(null);
                	return true;
                }
                return false;
            }
        });
        ((ImageButton) findViewById(R.id.web_add)).setOnClickListener(mAddListener);        
        ((ImageButton) findViewById(R.id.web_del)).setOnClickListener(mDelListener);        
				
		Intent intent = getIntent();
		site = intent.getStringExtra("site");
		time = intent.getStringExtra("time");
		text = intent.getStringExtra("text");
		
        Log.d(AigentsTabActivity.LOG_TAG,"Opening url "+site);
		//TODO: terminate on wrong url
		
        if (text != null)
        	webInput.setText(text);
        setSite(site);
        history.push(site);
		webView.loadUrl(site);
	}	

	
    OnClickListener mAddListener = new OnClickListener() {
        public void onClick(View v) {
        	text = webInput.getText().toString();
        	if (AL.empty(text))
        		AigentsTabActivity.displayStatus(v.getContext(), getText(R.string.hint_enter_text).toString());
        	else {
        		//TODO:check if new urls added via "There times today, new true, trust true, text "+text+", sources '"+site+"'." are added silently!
        		//TODO:trust checked sites 
        		String request = (Reader.hasVariables(text))
        			//create trusted thing-pattern and site
        			? "My sites "+Writer.toString(site)+"."+
                    	" "+Writer.toString(site)+" trust true."+
                    	" My knows "+Writer.toString(text)+"."+
                    	" "+Writer.toString(text)+" trust true."
                    //create trusted news-item and site
        			: "There "+AL.buildQualifier(
                		new String[]{"times","sources","text"},
                		new String[]{"today",site,text})+" new true, trust true."+
                		" My sites "+Writer.toString(site)+"."+
                		" "+Writer.toString(site)+" trust true.";
        		request(request);
        	}
        }
    };

    OnClickListener mDelListener = new OnClickListener() {
        public void onClick(View v) {
        	text = webInput.getText().toString();
        	if (AL.empty(text))
        		;//TODO: what? AigentsTabActivity.displayStatus(v.getContext(), getText(R.string.hint_enter_text).toString());
        	else {
        		request(AL.buildQualifier(
            			new String[]{"times","sources","text"},
            			new String[]{time,site,text})+" new false, trust false.");
        	}
        }
    };
    
		
	//TODO:
	void request(String request) {
		Intent sendIntent = new Intent(this,activityClass);
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra("req", request);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);		
		AigentsTabActivity.displayStatus(this, request);
	}
		
	String join(String[] chunks, int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(chunks[i]);
		}
		return sb.toString();
	}
	
	//http://stackoverflow.com/questions/18276738/android-implement-findonpage-into-a-webview?lq=1
	private void find() {
		if (!AL.empty(text))
			if (find(text) < 1) {
				String chunks[] = text.split("\\s");
				if (!AL.empty(chunks))
					for (int len = chunks.length - 1; len > 0; len--)
						if (find(join(chunks,len)) > 0)
							return;
			
			}
	}
	
	private void unfind() {
	    try {
	        Method m = WebView.class.getMethod("setFindIsDown", Boolean.TYPE);   
	        m.invoke(webView, false); 
	    } catch (Throwable ignored){}		
	}
	
	
	//http://stackoverflow.com/questions/18276738/android-implement-findonpage-into-a-webview?lq=1
	//http://stackoverflow.com/questions/5294562/android-webview-highlight-a-specific-word-in-a-page-using-javascript
	private int find(String text) {
		int found = 0; 
		try{   
		    found = webView.findAll(text);
		    try {
		        Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);   
		        m.invoke(webView, true); 
		    } catch (Throwable ignored){}
		} catch(Throwable notIgnored){
		    try {
		    	Method m = WebView.class.getMethod("findAllAsync", new Class<?>[]{String.class});
		    	m.invoke(webView, text);  
		    } catch (Throwable ignored){}
		    found = 1;//can't wait //TODO:sort out how to catch all findings
		} 	
		return found;
	}
	
	/*
	//good, by we can't wait till everything is found async
	private void findAsync(String text) {
		try{   
		    Method m = WebView.class.getMethod("findAllAsync", new Class<?>[]{String.class});
		    m.invoke(webView, text);  
		} catch(Throwable notIgnored){
		    webView.findAll(text);
		    try {
		        Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);   
		        m.invoke(webView, true); 
		    } catch (Throwable ignored){}
		} 	
	}
	*/
	
	protected void setSite(String url) {
        if (url != null)
           	//((EditText)findViewById(R.id.web_site)).setText(site);
           	((TextView)findViewById(R.id.web_site)).setText(url);
	}
	
	@Override
	public void onBackPressed () {
		if (history.size() > 1) {
			history.pop();
			setSite(site = history.peek());
			webView.goBack();
		}
		else
			super.onBackPressed();
	}
	
	//http://www.mkyong.com/android/android-webview-example/
	public static void open(Context context, String site, String time, String text) {
    	//startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    	Intent intent = new Intent(context, WebViewer.class);
    	intent.putExtra("site", site);
    	if (time != null)
    		intent.putExtra("time", time);
    	if (text != null)
    		intent.putExtra("text", text);
    	context.startActivity(intent);		
	}

	//http://stackoverflow.com/questions/8503270/tracking-the-selecting-text-in-android-webview
	String selection = null;
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		/*
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {              
	        if (selection == null) {                       
	             selection = clipboard.getText().toString();    
	             webView.postDelayed(onClipBoard, 500);
	        }
	    }
	    */
	    return false;
	 }


	private Runnable onClipBoard=new Runnable() {
	   public void run() {                  
		   /*
	            // if selected text is copied in clipboard toast will show the  
	            // correct text otherwise else part will execute                
	      if (!clipboardManager.getText().toString().equalsIgnoreCase("XXXXXX")) {
	            Toast.makeText(getApplicationContext(),
	                    "selected Text = " + clipboardManager.getText().toString(),
	                    Toast.LENGTH_LONG).show();
	            clipboardManager.setText("XXXXXX");

	      } else {
	           webView.postDelayed(onClipBoard, 1000);
	      }  
	      */   
		   //Toast.makeText(getApplicationContext(),"selected Text = " + selection,Toast.LENGTH_LONG).show();
		   //selection = null;
	   }
	};		
}