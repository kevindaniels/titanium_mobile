/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.webview;

import java.lang.ref.WeakReference;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.media.TiVideoActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TiWebViewClient extends WebViewClient
{
	private static final String TAG = "TiWVC";

	private WeakReference<TiUIWebView> webView;
	private TiWebViewBinding binding;
	private String username, password;

	public TiWebViewClient(TiUIWebView tiWebView, WebView webView)
	{
		super();
		this.webView = new WeakReference<TiUIWebView>(tiWebView);
		binding = new TiWebViewBinding(webView);
	}
	
	TiUIWebView getTiView() {
	    if (webView != null) {
	        return webView.get();
	    }
	    return null;
	}
	
	
	public void destroy() {
	    binding.destroy();
	    binding = null;
	    webView = null;
	}

	@Override
	public void onPageFinished(WebView view, String url)
	{
		super.onPageFinished(view, url);
        TiUIWebView webView = getTiView();
        if (webView == null) {
            return;
        }
//		WebViewProxy proxy = (WebViewProxy) webView.getProxy();
        KrollDict data = webView.eventForURL(url);
		webView.changeProxyUrl(data.getString(TiC.PROPERTY_URL));
        webView.onProgressChanged(view, 100);
        if (webView.getProxy().hasListeners(TiC.EVENT_LOAD, false)) {
            webView.getProxy().fireEvent(TiC.EVENT_LOAD, data, false, false);
        }
		webView.setBindingCodeInjected(false);
	}

	public TiWebViewBinding getBinding()
	{
		return binding;
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon)
	{
		super.onPageStarted(view, url, favicon);
		Log.d(TAG, "onPageStarted " + url);
		TiUIWebView webView = getTiView();
		if (webView == null) {
            return;
        }
		webView.onProgressChanged(view, 0);
        if (webView.getProxy().hasListeners("beforeload", false)) {
            webView.getProxy().fireEvent("beforeload", webView.eventForURL(url), false, false);
        }
        
        if (webView.getProxy().hasListeners("startload", false)) {
            webView.getProxy().fireEvent("startload", webView.eventForURL(url), false, false);
        }
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
	{
		super.onReceivedError(view, errorCode, description, failingUrl);
        TiUIWebView webView = getTiView();
        if (webView == null) {
            return;
        }
        KrollDict data = webView.eventForURL(failingUrl);
		data.put("errorCode", errorCode);
		data.putCodeAndMessage(errorCode, description);
		data.put("message", description);
		webView.getProxy().fireEvent(TiC.EVENT_ERROR, data);

	}

	@Override
	public boolean shouldOverrideUrlLoading(final WebView view, String url)
	{
		Log.d(TAG, "url=" + url, Log.DEBUG_MODE);
        TiUIWebView webView = getTiView();
        if (webView == null) {
            return super.shouldOverrideUrlLoading(view, url);
        }
		webView.setIsLocalHTML(false);
		if (webView.getProxy().hasProperty(TiC.PROPERTY_BLACKLISTED_URLS)) {
		    String [] blacklistedSites = TiConvert.toStringArray((Object[])webView.getProxy().getProperty(TiC.PROPERTY_BLACKLISTED_URLS));
		    for(String site : blacklistedSites) {
		        if (url.equalsIgnoreCase(site) || (url.indexOf(site) > -1)) {
		            KrollDict data = new KrollDict();
		            data.put("url", url);
		            data.put("message", "Webview did not load blacklisted url.");
		            webView.getProxy().fireEvent(TiC.PROPERTY_ON_STOP_BLACKISTED_URL, data);
		            return true;
		        }
		    }
		}

		if (URLUtil.isAssetUrl(url) || URLUtil.isContentUrl(url) || URLUtil.isFileUrl(url)) {
			// go through the proxy to ensure we're on the UI thread
			webView.getProxy().setPropertyAndFire(TiC.PROPERTY_URL, url);
			return true;
		} else if(url.startsWith(WebView.SCHEME_TEL)) {
			Log.i(TAG, "Launching dialer for " + url, Log.DEBUG_MODE);
			Intent dialer = Intent.createChooser(new Intent(Intent.ACTION_DIAL, Uri.parse(url)), "Choose Dialer");
			webView.getProxy().getActivity().startActivity(dialer);
			return true;
		} else if (url.startsWith(WebView.SCHEME_MAILTO)) {
			Log.i(TAG, "Launching mailer for " + url, Log.DEBUG_MODE);
			Intent mailer = Intent.createChooser(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)), "Send Message");
			webView.getProxy().getActivity().startActivity(mailer);
			return true;
		} else if (url.startsWith(WebView.SCHEME_GEO)) {
			Log.i(TAG, "Launching app for " + url, Log.DEBUG_MODE);
			/*geo:latitude,longitude
			geo:latitude,longitude?z=zoom
			geo:0,0?q=my+street+address
			geo:0,0?q=business+near+city
			*/
			Intent geoviewer = Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Choose Viewer");
			webView.getProxy().getActivity().startActivity(geoviewer);
			return true;
		} else {
			String extension = MimeTypeMap.getFileExtensionFromUrl(url);
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mimeType != null) {
				return shouldHandleMimeType(mimeType, url);
			}
			return super.shouldOverrideUrlLoading(view, url);
		}
	}

	private boolean shouldHandleMimeType(String mimeType, String url)
	{
		if (mimeType.startsWith("video/")) {
	        TiUIWebView webView = getTiView();
	        if (webView != null) {
    			Intent intent = new Intent();
    			intent.setClass(webView.getProxy().getActivity(), TiVideoActivity.class);
    			intent.putExtra("contentURL", url);
    			intent.putExtra("play", true);
    			webView.getProxy().getActivity().startActivity(intent);
	        }
			
			return true;
		}
		return false;
	}

	@Override
	public void onReceivedHttpAuthRequest(WebView view,
			HttpAuthHandler handler, String host, String realm)
	{
		
		if (this.username != null && this.password != null) {
			handler.proceed(this.username, this.password);
		}
		else {
	        TiUIWebView webView = getTiView();
	        if (webView != null) {
	            webView.getProxy().fireEvent(TiC.EVENT_AUTHENTICATION);
	        }
		}
	}

	public void setBasicAuthentication(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	@Override
	public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
	{
        TiUIWebView webView = getTiView();
		/*
		 * in theory this should be checked to make sure it's not null but if there is some failure 
		 * in the association then usage of webViewProxy should trigger a NPE to make sure the issue 
		 * is not ignored
		 */
        
        boolean ignoreSslError = false;
       if (webView != null) {
           KrollProxy webViewProxy = webView.getProxy();
           
           KrollDict data = webView.eventForURL(view.getUrl());
           data.put(TiC.ERROR_PROPERTY_CODE, error.getPrimaryError());
           webView.getProxy().fireSyncEvent(TiC.EVENT_SSL_ERROR, data);
    
           try {
               ignoreSslError = webViewProxy.getProperties().optBoolean(TiC.PROPERTY_WEBVIEW_IGNORE_SSL_ERROR, false);
    
           } catch(IllegalArgumentException e) {
               Log.e(TAG, TiC.PROPERTY_WEBVIEW_IGNORE_SSL_ERROR + " property does not contain a boolean value, ignoring"); 
           }
       }
	

		if (ignoreSslError == true) {
			Log.w(TAG, "ran into SSL error but ignoring...");
			handler.proceed();

		} else {
			Log.e(TAG, "SSL error occurred: " + error.toString());
		}
	}

	@Override
	public void onLoadResource(WebView view, String url)
	{
		super.onLoadResource(view, url);
        TiUIWebView webView = getTiView();
        if (webView == null) {
            return;
        }
		if (webView.getProxy().hasListeners(TiC.EVENT_WEBVIEW_ON_LOAD_RESOURCE, false)) {
	        webView.getProxy().fireEvent(TiC.EVENT_WEBVIEW_ON_LOAD_RESOURCE, webView.eventForURL(url), false, false);
		}
		
	}

}
