/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.util.TiActivityHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.View;

@SuppressLint("InlinedApi")
@Kroll.proxy(propertyAccessors = {
		TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED,
        TiC.PROPERTY_DISPLAY_HOME_AS_UP,
        TiC.PROPERTY_BACKGROUND_COLOR,
        TiC.PROPERTY_BACKGROUND_IMAGE,
        TiC.PROPERTY_BACKGROUND_GRADIENT,
        TiC.PROPERTY_BACKGROUND_OPACITY,
        TiC.PROPERTY_LOGO,
		TiC.PROPERTY_ICON
})

public class ActionBarProxy extends KrollProxy
{
    private static final boolean JELLY_BEAN_MR1_OR_GREATER = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1);
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	private static final int MSG_DISPLAY_HOME_AS_UP = MSG_FIRST_ID + 100;
	private static final int MSG_SET_BACKGROUND_IMAGE = MSG_FIRST_ID + 101;
	private static final int MSG_SET_TITLE = MSG_FIRST_ID + 102;
	private static final int MSG_SHOW = MSG_FIRST_ID + 103;
	private static final int MSG_HIDE = MSG_FIRST_ID + 104;
	private static final int MSG_SET_LOGO = MSG_FIRST_ID + 105;
	private static final int MSG_SET_ICON = MSG_FIRST_ID + 106;
	private static final int MSG_SET_HOME_BUTTON_ENABLED = MSG_FIRST_ID + 107;
	private static final int MSG_SET_NAVIGATION_MODE = MSG_FIRST_ID + 108;
	private static final int MSG_SET_BACKGROUND_COLOR = MSG_FIRST_ID + 109;
	private static final int MSG_SET_BACKGROUND_GRADIENT = MSG_FIRST_ID + 110;
	private static final int MSG_RESET_BACKGROUND = MSG_FIRST_ID + 111;
	private static final int MSG_RESET_ICON = MSG_FIRST_ID + 112;
	private static final int MSG_SET_SUBTITLE = MSG_FIRST_ID + 113;
	private static final int MSG_SET_DISPLAY_SHOW_HOME = MSG_FIRST_ID + 114;
	private static final int MSG_SET_DISPLAY_SHOW_TITLE = MSG_FIRST_ID + 115;
    private static final int MSG_SET_BACKGROUND_OPACITY = MSG_FIRST_ID + 116;

	private static final String SHOW_HOME_AS_UP = "showHomeAsUp";
	private static final String HOME_BUTTON_ENABLED = "homeButtonEnabled";
	private static final String BACKGROUND_IMAGE = "backgroundImage";
	private static final String TITLE = "title";
	private static final String LOGO = "logo";
	private static final String ICON = "icon";
	private static final String NAVIGATION_MODE = "navigationMode";
	private static final String TAG = "ActionBarProxy";

	private ActionBar actionBar;
	private Drawable themeBackgroundDrawable;
	private Drawable themeIconDrawable = null;
	private boolean showTitleEnabled = true;
	private int defaultColor = 0;
	private boolean customBackgroundSet = false;
    private Drawable mActionBarBackgroundDrawable;
    private int backgroundAlpha = 255;
    
    private Drawable.Callback mDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            actionBar.setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
        }
    };
    
    private void setActionBarDrawable(final Drawable drawable) {
        mActionBarBackgroundDrawable = drawable;
        if (mActionBarBackgroundDrawable != null) {
            if (!JELLY_BEAN_MR1_OR_GREATER) {
                mActionBarBackgroundDrawable.setCallback(mDrawableCallback);
            }
            mActionBarBackgroundDrawable.setAlpha(backgroundAlpha);
        }
        actionBar.setBackgroundDrawable(mActionBarBackgroundDrawable);
    }

	public ActionBarProxy(TiBaseActivity activity)
	{
		super();
        actionBar = TiActivityHelper.getActionBar(activity);

//		try {
//		    actionBar = activity.getSupportActionBar();
//		    //trick to actually know if the internal action bar exists
//	        actionBar.isShowing();
//        } catch (NullPointerException e) {
//            //no internal action bar
//            actionBar = null;
//        }
		int resourceId = 0;
		try {
		    TypedValue typedValue = new TypedValue(); 
            int id = TiRHelper.getResource("android.support.v7.appcompat.R$", "attr.colorPrimary");
		    activity.getTheme().resolveAttribute(id, typedValue, true);
		    defaultColor = typedValue.data;
		    if (defaultColor == 0) {
		        //non material
	            resourceId = TiRHelper.getResource("android.support.v7.appcompat.R$", "id.action_context_bar");
	            if (resourceId > 0) {
	                View view = activity.getWindow().getDecorView().findViewById(resourceId);
	                if (view != null) {
	                    themeBackgroundDrawable = view.getBackground();
	                }
	            }
	            themeIconDrawable = getActionBarIcon(activity);
		    }
        } catch (ResourceNotFoundException e) {
        }
	}
	protected static TypedArray obtainStyledAttrsFromThemeAttr(Context context,
            int[] styleAttrs) throws ResourceNotFoundException {
        // Need to get resource id of style pointed to from the theme attr
        TypedValue outValue = new TypedValue();
    	int resourceId = TiRHelper.getResource("android.support.v7.appcompat.R$", "attr.actionBarStyle");
       context.getTheme().resolveAttribute(resourceId, outValue, true);
        final int styleResId =  outValue.resourceId;

        return context.obtainStyledAttributes(styleResId, styleAttrs);
    }
	
	protected Drawable getActionBarBackground(Context context) {
        TypedArray values = null;
        try {
            int resourceId = TiRHelper.getResource("android.support.v7.appcompat.R$", "attr.background");
            int[] attrs = {resourceId};
            values = context.getTheme().obtainStyledAttributes(attrs);
            return values.getDrawable(0);
        } catch (ResourceNotFoundException e) {
            return null;
        } finally {
            if (values != null) {
                values.recycle();
            }
        }
    }

    public static int getActionBarSize(Context context) {
        TypedArray values = null;
        try {
            int resourceId = TiRHelper.getResource("android.support.v7.appcompat.R$", "attr.actionBarSize");
            int[] attrs = {resourceId};
            values = context.getTheme().obtainStyledAttributes(attrs);
            return values.getDimensionPixelSize(0, 0);
        } catch (ResourceNotFoundException e) {
            return 0;
        } finally {
            if (values != null) {
                values.recycle();
            }
        }
    }
	

	protected Drawable getActionBarIcon(Context context) {
        int[] android_styleable_ActionBar = {android.R.attr.icon};

        // Now get the action bar style values...
        TypedArray abStyle = null;
        try {
        	abStyle = obtainStyledAttrsFromThemeAttr(context, android_styleable_ActionBar);
       	int count = abStyle.getIndexCount();
        	if (count > 0) {
	            return abStyle.getDrawable(0);
        	}
        	return context.getApplicationInfo().loadIcon(context.getPackageManager()); 
        } catch (ResourceNotFoundException e) {
			return null;
		} finally {
            if (abStyle != null) abStyle.recycle();
        }
    }

	@Kroll.method @Kroll.setProperty
	private void setDisplayHomeAsUp(boolean showHomeAsUp)
	{
		if(TiApplication.isUIThread()) {
			handlesetDisplayHomeAsUp(showHomeAsUp);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_DISPLAY_HOME_AS_UP, showHomeAsUp);
			message.getData().putBoolean(SHOW_HOME_AS_UP, showHomeAsUp);
			message.sendToTarget();
		}
	}

	@Kroll.method @Kroll.setProperty
	public void setNavigationMode(int navigationMode)
	{
		if (TiApplication.isUIThread()) {
			handlesetNavigationMode(navigationMode);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_NAVIGATION_MODE, navigationMode);
			message.getData().putInt(NAVIGATION_MODE, navigationMode);
			message.sendToTarget();
		}
	}

	public void setBackgroundImage(String url)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundImage(url);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_BACKGROUND_IMAGE, url);
			message.getData().putString(BACKGROUND_IMAGE, url);
			message.sendToTarget();
		}
	}
	
	public void setBackgroundColor(int color)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundColor(color);
		} else {
			getMainHandler().obtainMessage(MSG_SET_BACKGROUND_COLOR, Integer.valueOf(color)).sendToTarget();
		}
	}
	
	public void setBackgroundGradient(KrollDict gradient)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundGradient(gradient);
		} else {
			getMainHandler().obtainMessage(MSG_SET_BACKGROUND_GRADIENT, gradient).sendToTarget();
		}
	}
	
	public void setBackgroundOpacity(float alpha)
    {
        if (TiApplication.isUIThread()) {
            handleSetBackgroundOpacity(alpha);
        } else {
            getMainHandler().obtainMessage(MSG_SET_BACKGROUND_OPACITY, Float.valueOf(alpha)).sendToTarget();
        }
    }

	@Kroll.method @Kroll.setProperty
	public void setTitle(String title)
	{
		if (TiApplication.isUIThread()) {
			handleSetTitle(title);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_TITLE, title);
			message.getData().putString(TITLE, title);
			message.sendToTarget();
		}
	}

	@Kroll.method @Kroll.getProperty
	public String getTitle()
	{
		if (actionBar == null) {
			return null;
		}
		return (String) actionBar.getTitle();
	}

	@Kroll.method @Kroll.setProperty
	public void setSubtitle(String subTitle)
	{
		if (TiApplication.isUIThread()) {
			handleSetSubTitle(subTitle);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_SUBTITLE, subTitle);
			message.getData().putString(TiC.PROPERTY_SUBTITLE, subTitle);
			message.sendToTarget();
		}
	}
	
	@Kroll.method 
	public void setDisplayShowHomeEnabled(boolean show) {
		if (actionBar == null) {
			return;
		}
		
		if (TiApplication.isUIThread()) {
			actionBar.setDisplayShowHomeEnabled(show);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_DISPLAY_SHOW_HOME, show);
			message.sendToTarget();
		}
	}
	
	@Kroll.method
	public void setDisplayShowTitleEnabled(boolean show) {
		if (actionBar == null) {
			return;
		}
		
		if (TiApplication.isUIThread()) {
			actionBar.setDisplayShowTitleEnabled(show);
			showTitleEnabled = show;
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_DISPLAY_SHOW_TITLE, show);
			message.sendToTarget();
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public String getSubtitle()
	{
		if (actionBar == null) {
			return null;
		}
		return (String) actionBar.getSubtitle();
	}
	
    @Kroll.method
    @Kroll.getProperty
    public double getHeight() {
        if (actionBar == null) {
            return 0;
        }
        TiDimension nativeHeight = new TiDimension(actionBar.getHeight(), TiDimension.TYPE_HEIGHT);
        return nativeHeight.getAsDefault();
    }

	@SuppressWarnings("deprecation")
    public int getNavigationMode()
	{
		if (actionBar == null) {
			return 0;
		}
		return (int) actionBar.getNavigationMode();
	}

	@Kroll.method
	public void show()
	{
		if (TiApplication.isUIThread()) {
			handleShow();
		} else {
			getMainHandler().obtainMessage(MSG_SHOW).sendToTarget();
		}
	}

	@Kroll.method
	public void hide()
	{
		if (TiApplication.isUIThread()) {
			handleHide();
		} else {
			getMainHandler().obtainMessage(MSG_HIDE).sendToTarget();
		}
	}

	public void setLogo(String url)
	{
		if (TiApplication.isUIThread()) {
			handleSetLogo(url);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_LOGO, url);
			message.getData().putString(LOGO, url);
			message.sendToTarget();
		}
		
	}

	public void setIcon(Object value)
	{
			if (TiApplication.isUIThread()) {
				handleSetIcon(value);
			} else {
				getMainHandler().obtainMessage(MSG_SET_ICON, value).sendToTarget();
			}		
	}

	private void handleSetIcon(Object value)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}
		Drawable icon = null;
		if (value instanceof String) {
			 icon = getDrawableFromUrl(TiConvert.toString(value));
		}
		else {
			icon = TiUIHelper.getResourceDrawable(TiConvert.toInt(value));
		}
		if (icon != null) {
			actionBar.setIcon(icon);
		} 
	}
	
	private void handleSetTitle(String title)
	{
		if (actionBar != null) {
			actionBar.setTitle(title);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleSetSubTitle(String subTitle)
	{
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setSubtitle(subTitle);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}
	
	private void handleShow()
	{
		if (actionBar != null) {
			actionBar.show();
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleHide()
	{
		if (actionBar != null) {
			actionBar.hide();
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleSetBackgroundImage(String url)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		actionBar.setDisplayShowTitleEnabled(!showTitleEnabled);
        actionBar.setDisplayShowTitleEnabled(showTitleEnabled);
        
        setActionBarDrawable(getDrawableFromUrl(url));
        customBackgroundSet = (mActionBarBackgroundDrawable != null);
	}
	
	private void handleSetBackgroundOpacity(float alpha)
    {
        if (actionBar == null) {
            Log.w(TAG, "ActionBar is not enabled");
            return;
        }
        
        backgroundAlpha = (int) (alpha*255.0f);
        if (mActionBarBackgroundDrawable == null) {
            handleSetBackgroundColor(defaultColor);
        } else {
            mActionBarBackgroundDrawable.setAlpha(backgroundAlpha);
        }
    }
	
	private void handleSetBackgroundColor(int color)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}
		
        actionBar.setDisplayShowTitleEnabled(!showTitleEnabled);
        actionBar.setDisplayShowTitleEnabled(showTitleEnabled);
        setActionBarDrawable(new ColorDrawable(color));
        customBackgroundSet = (mActionBarBackgroundDrawable != null) && color != defaultColor;
	}
	
	private void handleSetBackgroundGradient(KrollDict gradDict)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

        actionBar.setDisplayShowTitleEnabled(!showTitleEnabled);
        actionBar.setDisplayShowTitleEnabled(showTitleEnabled);
        setActionBarDrawable(TiUIHelper.buildGradientDrawable(gradDict));
        customBackgroundSet = (mActionBarBackgroundDrawable != null);
	}
	
	private void activateHomeButton(boolean value)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		// If we have a listener on the home icon item, then enable the home button (we need to do this for ICS and
		// above)
		if (TiApplication.isUIThread()) {
			actionBar.setHomeButtonEnabled(value);
		} else {
			getMainHandler().obtainMessage(MSG_SET_HOME_BUTTON_ENABLED, Boolean.valueOf(value)).sendToTarget();
		}
	}


	private void handlesetDisplayHomeAsUp(boolean showHomeAsUp)
	{
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handlesetNavigationMode(int navigationMode)
	{
	    if (actionBar != null) {
	        actionBar.setNavigationMode(navigationMode);
        } else {
            Log.w(TAG, "ActionBar is not enabled");
        }
	}

	private void handleSetLogo(String url)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		Drawable logo = getDrawableFromUrl(url);
		if (logo != null) {
			actionBar.setLogo(logo);
		}
	}

	private Drawable getDrawableFromUrl(String url)
	{
		TiUrl imageUrl = new TiUrl((String) url);
		TiFileHelper tfh = new TiFileHelper(TiApplication.getInstance());
		return tfh.loadDrawable(imageUrl.resolve(), false);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_DISPLAY_HOME_AS_UP:
				handlesetDisplayHomeAsUp(msg.getData().getBoolean(SHOW_HOME_AS_UP));
				return true;
			case MSG_SET_NAVIGATION_MODE:
				handlesetNavigationMode(msg.getData().getInt(NAVIGATION_MODE));
				return true;
			case MSG_SET_BACKGROUND_COLOR:
				handleSetBackgroundColor((Integer)msg.obj);
				return true;
			case MSG_SET_BACKGROUND_IMAGE:
				handleSetBackgroundImage(msg.getData().getString(BACKGROUND_IMAGE));
				return true;
			case MSG_SET_BACKGROUND_GRADIENT:
				handleSetBackgroundGradient((KrollDict) (msg.obj));
				return true;
			case MSG_SET_TITLE:
				handleSetTitle(msg.getData().getString(TITLE));
				return true;
			case MSG_SET_SUBTITLE:
				handleSetSubTitle(msg.getData().getString(TiC.PROPERTY_SUBTITLE));
				return true;
			case MSG_SET_DISPLAY_SHOW_HOME: {
				boolean show = TiConvert.toBoolean(msg.obj, true);
				if (actionBar != null) {
					actionBar.setDisplayShowHomeEnabled(show);
				}
				return true;
			}
			case MSG_SET_DISPLAY_SHOW_TITLE: {
				boolean show = TiConvert.toBoolean(msg.obj, true);
				if (actionBar != null) {
					actionBar.setDisplayShowTitleEnabled(show);
					showTitleEnabled = show;
				}
				return true;
			}
			case MSG_SHOW:
				handleShow();
				return true;
			case MSG_HIDE:
				handleHide();
				return true;
			case MSG_RESET_BACKGROUND:
				actionBar.setBackgroundDrawable(themeBackgroundDrawable);
				return true;
			case MSG_RESET_ICON:
				actionBar.setIcon(themeIconDrawable);
				return true;
			case MSG_SET_LOGO:
				handleSetLogo(msg.getData().getString(LOGO));
				return true;
			case MSG_SET_ICON:
				handleSetIcon(msg.obj);
				return true;
			case MSG_SET_HOME_BUTTON_ENABLED:
				actionBar.setHomeButtonEnabled((Boolean)msg.obj);
				return true;
		}
		return super.handleMessage(msg);
	}

    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED:
            activateHomeButton(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_DISPLAY_HOME_AS_UP:
            setDisplayHomeAsUp(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_BACKGROUND_IMAGE:
        case TiC.PROPERTY_BAR_IMAGE:
            setBackgroundImage(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_BACKGROUND_COLOR:
        case TiC.PROPERTY_BAR_COLOR:
            setBackgroundColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_BACKGROUND_GRADIENT:
            setBackgroundGradient(TiConvert.toKrollDict(newValue));
            break;
        case TiC.PROPERTY_BACKGROUND_OPACITY:
        case TiC.PROPERTY_BAR_OPACITY:
            setBackgroundOpacity(TiConvert.toFloat(newValue, 1.0f));
            break;
        case TiC.PROPERTY_LOGO:
            setLogo(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_ICON:
        case TiC.PROPERTY_BAR_ICON:
            if (newValue != null) {
                if (newValue instanceof String) {
                    setIcon((String)newValue);
                }
                else if (newValue instanceof Number){
                    setIcon(TiConvert.toInt(newValue));
                }
            }
            else {
                if (TiApplication.isUIThread()) {
                    actionBar.setIcon(themeIconDrawable);
                } else {
                    getMainHandler().obtainMessage(MSG_RESET_ICON).sendToTarget();
                }
            }
        default:
            break;
        }
    }
    
    @Override
    public void setProperties(KrollDict newProps) {
        super.setProperties(newProps);
        if (customBackgroundSet && properties.get(TiC.PROPERTY_BACKGROUND_COLOR) == null && 
                properties.get(TiC.PROPERTY_BACKGROUND_IMAGE) == null &&  
                properties.get(TiC.PROPERTY_BACKGROUND_GRADIENT) == null )
        {
            if (defaultColor != 0) {
                setBackgroundColor(defaultColor);
            } else {
                if (TiApplication.isUIThread()) {
                    actionBar.setBackgroundDrawable(themeBackgroundDrawable);
                } else {
                    getMainHandler().obtainMessage(MSG_RESET_BACKGROUND).sendToTarget();
                }
            }
            
            customBackgroundSet = false;
        }
    }
	
	@Override
	public void onPropertyChanged(String key, Object newValue, Object oldValue) {
        propertySet(key, newValue, oldValue, true);
	}

	@Override
	public String getApiName()
	{
		return "Ti.Android.ActionBar";
	}
}
