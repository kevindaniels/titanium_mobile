/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.AnimatableReusableProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.KrollProxyReusableListener;
import org.appcelerator.titanium.view.TiTouchDelegate;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.listview.TiListView.TiBaseAdapter;
import android.annotation.SuppressLint;
import android.os.Message;
import android.view.View;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = {})
@SuppressWarnings({ "unchecked", "rawtypes" })
@SuppressLint("DefaultLocale")
public class ListSectionProxy extends AnimatableReusableProxy {

	private static final String TAG = "ListSectionProxy";
	private ArrayList<ListItemData> listItemData;
	private int mItemCount;
    private int mCurrentItemCount = 0;
	private TiBaseAdapter adapter;
	private ArrayList<Object> itemProperties;
	private ArrayList<Integer> filterIndices;
	private boolean preload;
	private ArrayList<Boolean> hiddenItems;
	boolean hidden = false;
	
	private int sectionIndex;

	private WeakReference<TiListView> listView;

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_SET_ITEMS = MSG_FIRST_ID + 700;
	private static final int MSG_APPEND_ITEMS = MSG_FIRST_ID + 701;
	private static final int MSG_INSERT_ITEMS_AT = MSG_FIRST_ID + 702;
	private static final int MSG_DELETE_ITEMS_AT = MSG_FIRST_ID + 703;
	private static final int MSG_GET_ITEM_AT = MSG_FIRST_ID + 704;
	private static final int MSG_REPLACE_ITEMS_AT = MSG_FIRST_ID + 705;
	private static final int MSG_UPDATE_ITEM_AT = MSG_FIRST_ID + 706;
	private static final int MSG_GET_ITEMS = MSG_FIRST_ID + 707;

	private static HashMap<String, String> toPassProps;

	public class ListItemData {
		private KrollDict properties;
		private String searchableText;
		private String template = null;
		private boolean visible = true;

		public ListItemData(KrollDict properties) {
			setProperties(properties);
		}
		
		private void updateSearchableAndVisible() {
		    if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
                Object props = properties.get(TiC.PROPERTY_PROPERTIES);
                if (props instanceof HashMap) {
                    HashMap<String, Object> propsHash = (HashMap<String, Object>) props;
                    if (propsHash.containsKey(TiC.PROPERTY_SEARCHABLE_TEXT)) {
                        searchableText = TiConvert.toString(propsHash,
                                TiC.PROPERTY_SEARCHABLE_TEXT);
                    }
                    if (propsHash.containsKey(TiC.PROPERTY_VISIBLE)) {
                        visible = TiConvert.toBoolean(propsHash,
                                TiC.PROPERTY_VISIBLE, true);
                    }
                }
            }
		}

		public KrollDict getProperties() {
			return properties;
		}

		public String getSearchableText() {
			return searchableText;
		}
		

		public boolean isVisible() {
			return visible;
		}


		public String getTemplate() {
			return template;
		}

        public void setProperties(KrollDict d) {
            this.properties = d;
            if (properties.containsKey(TiC.PROPERTY_TEMPLATE)) {
                this.template = properties.getString(TiC.PROPERTY_TEMPLATE);
            }
            else {
                this.template = getListView().getDefaultTemplateBinding();
            }
            // set searchableText
            updateSearchableAndVisible();
        }
        
        public void setProperty(String binding, String key, Object value) {
            if (properties.containsKey(binding)) {
                ((HashMap)properties.get(binding)).put(key, value);
            }
        }
	}
    
	public ListSectionProxy() {
		// initialize variables
		if (toPassProps == null) {
			toPassProps = new HashMap<String, String>();
			toPassProps.put(TiC.PROPERTY_ACCESSORY_TYPE,
					TiC.PROPERTY_ACCESSORY_TYPE);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR,
					TiC.PROPERTY_BACKGROUND_SELECTED_COLOR);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE,
					TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_GRADIENT,
					TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT);
			toPassProps.put(TiC.PROPERTY_ROW_HEIGHT, TiC.PROPERTY_HEIGHT);
			toPassProps.put(TiC.PROPERTY_MIN_ROW_HEIGHT, TiC.PROPERTY_MIN_HEIGHT);
			toPassProps.put(TiC.PROPERTY_MAX_ROW_HEIGHT, TiC.PROPERTY_MAX_HEIGHT);
		}
		listItemData = new ArrayList<ListItemData>();
		filterIndices = new ArrayList<Integer>();
		hiddenItems = new ArrayList<Boolean>();
		mItemCount = 0;
		preload = false;
	}

	public void setAdapter(TiBaseAdapter a) {
		adapter = a;
	}
	
	public boolean hasHeader() {
        return getHoldedProxy(TiC.PROPERTY_HEADER_VIEW) != null;
	}
	public boolean hasFooter() {
        return getHoldedProxy(TiC.PROPERTY_FOOTER_VIEW) != null;
    }
	
	private static final ArrayList<String> KEY_SEQUENCE;
    static{
      ArrayList<String> tmp = new ArrayList<String>();
      tmp.add(TiC.PROPERTY_HEADER_TITLE);
      tmp.add(TiC.PROPERTY_FOOTER_TITLE);
      tmp.add(TiC.PROPERTY_HEADER_VIEW);
      tmp.add(TiC.PROPERTY_FOOTER_VIEW);
      KEY_SEQUENCE = tmp;
    }
    @Override
    protected ArrayList<String> keySequence() {
        return KEY_SEQUENCE;
    }
	
	@Override
	public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
	    switch (key) {
        case TiC.PROPERTY_HEADER_VIEW:
        case TiC.PROPERTY_FOOTER_VIEW:
            addProxyToHold(newValue, key);
            break;
        case TiC.PROPERTY_HEADER_TITLE:
            addProxyToHold(TiListView.headerViewDict(TiConvert.toString(newValue)), TiC.PROPERTY_HEADER_VIEW);
        case TiC.PROPERTY_FOOTER_TITLE:
            addProxyToHold(TiListView.footerViewDict(TiConvert.toString(newValue)), TiC.PROPERTY_FOOTER_VIEW);
            break;
        case TiC.PROPERTY_ITEMS:
            handleSetItems(newValue);
            break;
        case TiC.PROPERTY_VISIBLE:
            setVisible(TiConvert.toBoolean(newValue, true));
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	public void notifyDataChange() {
		if (adapter == null) return;
        updateCurrentItemCount();
		getMainHandler().post(new Runnable() {
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {

		case MSG_SET_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			handleSetItems(result.getArg());
			result.setResult(null);
			return true;
		}

		case MSG_GET_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			result.setResult(itemProperties.toArray());
			return true;
		}

		case MSG_APPEND_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			handleAppendItems(result.getArg());
			result.setResult(null);
			return true;
		}

		case MSG_INSERT_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			handleInsertItemsAt(index, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}

		case MSG_DELETE_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			int count = data.getInt(TiC.PROPERTY_COUNT);
			handleDeleteItemsAt(index, count);
			result.setResult(null);
			return true;
		}

		case MSG_REPLACE_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			int count = data.getInt(TiC.PROPERTY_COUNT);
			handleReplaceItemsAt(index, count, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}

		case MSG_GET_ITEM_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict item = handleGetItemAt(TiConvert.toInt(result.getArg()));
			result.setResult(item);
			return true;
		}

		case MSG_UPDATE_ITEM_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			handleUpdateItemAt(index, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}
		default: {
			return super.handleMessage(msg);
		}

		}
	}
	
	@Kroll.method
    public KrollProxy getBinding(final int itemIndex, final String bindId) {
        if (listView != null) {
            return listView.get().getChildByBindId(this.sectionIndex, itemIndex, bindId);
        }
        return null;
    }

	@Kroll.method
	public KrollDict getItemAt(int index) {
		if (TiApplication.isUIThread()) {
			return handleGetItemAt(index);
		} else {
			return (KrollDict) TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_GET_ITEM_AT), index);
		}
	}

	private KrollDict handleGetItemAt(int index) {
		if (itemProperties != null && index >= 0
				&& index < itemProperties.size()) {
			return new KrollDict((HashMap) itemProperties.get(index));
		}
		return null;
	}

	private int getRealPosition(int position) {
		int hElements = getHiddenCountUpTo(position);
		int diff = 0;
		for (int i = 0; i < hElements; i++) {
			diff++;
			if (hiddenItems.get(position + diff))
				i--;
		}
		return (position + diff);
	}
	
	private int getInverseRealPosition(int position) {
		int hElements = getHiddenCountUpTo(position);
		int diff = 0;
		for (int i = 0; i < hElements; i++) {
			diff++;
			if (hiddenItems.get(position + diff))
				i--;
		}
		return (position - diff);
	}


	private int getHiddenCountUpTo(int location) {
		int count = 0;
		for (int i = 0; i < location; i++) {
			if (hiddenItems.get(i))
				count++;
		}
		return count;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleSetItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_SET_ITEMS), data);
		}
	}

	@Kroll.method
	@Kroll.getProperty
	public Object[] getItems() {
		if (itemProperties == null) {
			return new Object[0];
		} else if (TiApplication.isUIThread()) {
			return itemProperties.toArray();
		} else {
			return (Object[]) TiMessenger
					.sendBlockingMainMessage(getMainHandler().obtainMessage(
							MSG_GET_ITEMS));
		}
	}

	@Kroll.method
	public void appendItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleAppendItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_APPEND_ITEMS), data);
		}
	}

	public boolean isIndexValid(int index) {
		return (index >= 0) ? true : false;
	}

	@Kroll.method
	public void insertItemsAt(int index, Object data) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleInsertItemsAt(index, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.PROPERTY_DATA, data);
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_INSERT_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void deleteItemsAt(int index, int count) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleDeleteItemsAt(index, count);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_DELETE_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void replaceItemsAt(int index, int count, Object data) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleReplaceItemsAt(index, count, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			d.put(TiC.PROPERTY_DATA, data);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_REPLACE_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void updateItemAt(int index, Object data) {
		if (!isIndexValid(index) || !(data instanceof HashMap)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleUpdateItemAt(index, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_DATA, data );
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_UPDATE_ITEM_AT), d);
		}
	}
	
	public void updateItemAt(int index, String binding, String key, Object value) {
	    if (index < 0 || index >= mItemCount) {
	        return;
	    }
	    if (itemProperties != null) {
	        HashMap itemProp = (HashMap) itemProperties.get(index);
	        if (!itemProp.containsKey(binding)) {
	            itemProp.put(binding, new HashMap<String, Object>());
	        }
	        ((HashMap)itemProp.get(binding)).put(key, value);
        }
	    ListItemData itemD = getItemDataAt(index);
	    itemD.setProperty(binding, key, value);
    }
	
	@Kroll.method
	public void hide() {
        setVisible(false);
	}
	
	@Kroll.method
	public void show() {
		setVisible(true);
	}
	
	@Kroll.method
	@Kroll.setProperty
	public void setVisible(boolean value) {
		if (hidden == !value) return;
        hidden = !value;
		notifyDataChange();
	}
	
	@Kroll.method
	@Kroll.getProperty
	public boolean getVisible() {
		return !hidden;
	}

	
	public void processPreloadData() {
		if (itemProperties != null && preload) {
			handleSetItems(itemProperties.toArray());
			preload = false;
		}
	}

	public void refreshItems() {
		handleSetItems(itemProperties.toArray());
	}

	private void processData(Object[] items, int offset) {
		if (listItemData == null) {
			return;
		}

		// Second pass we would merge properties
		for (int i = 0; i < items.length; i++) {
			Object itemData = items[i];
			if (itemData instanceof HashMap) {
				KrollDict d = new KrollDict((HashMap) itemData);
				ListItemData itemD = new ListItemData(d);
				listItemData.add(i + offset, itemD);
				hiddenItems.add(i + offset, !itemD.isVisible());
			}
		}
		updateCurrentItemCount();
		// Notify adapter that data has changed.
		if (preload == false) {
	        adapter.notifyDataSetChanged();
		}
	}

	private void handleSetItems(Object data) {

		if (data instanceof Object[]) {
			Object[] items = (Object[]) data;
			itemProperties = new ArrayList<Object>(Arrays.asList(items));
			listItemData.clear();
			hiddenItems.clear();
			// only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			mItemCount = items.length;
			processData(items, 0);

		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}

	private void handleAppendItems(Object data) {
		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;
			if (itemProperties == null) {
				itemProperties = new ArrayList<Object>(Arrays.asList(views));
			} else {
				for (Object view : views) {
					itemProperties.add(view);
				}
			}
			// only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			// we must update the itemCount before notify data change. If we
			// don't, it will crash
			int count = mItemCount;
			mItemCount += views.length;

			processData(views, count);

		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}

	private void handleInsertItemsAt(int index, Object data) {
        TiListView listView = getListView();
        if (listView != null) {
            int position = listView.findItemPosition(sectionIndex, index) - listView.getHeaderViewCount();
            if (data instanceof Object[]) {
                listView.insert(position, (Object[])data);
            }
            else {
                listView.insert(position, data);
            }
        }
        else {
            if (data instanceof Object[]) {
                Object[] views = (Object[]) data;

                if (itemProperties == null) {
                    itemProperties = new ArrayList<Object>(Arrays.asList(views));
                } else {
                    if (index < 0 || index > itemProperties.size()) {
                        Log.e(TAG, "Invalid index to handleInsertItem",
                                Log.DEBUG_MODE);
                        return;
                    }
                    int counter = index;
                    for (Object view : views) {
                        itemProperties.add(counter, view);
                        counter++;
                    }
                }
                // only process items when listview's properties is processed.
                preload = true;
                
            } else {
                Log.e(TAG, "Invalid argument type to insertItemsAt",
                        Log.DEBUG_MODE);
            }
        }
	}
	
	private void handleUpdateItemAt(int itemIndex, Object data) {
	    if (itemProperties == null) {
	        return;
	    }
	    int nonRealItemIndex = itemIndex;
//	    if (hasHeader()) {
//	        nonRealItemIndex += 1;
//	    }
	    
	    TiListView listView = getListView();
        
	    KrollDict currentItem = KrollDict.merge((HashMap)itemProperties.get(itemIndex), (HashMap)(data));
	    if (currentItem == null) return;
	    itemProperties.set(itemIndex, currentItem);
	    // only process items when listview's properties is processed.
        if (listView == null) {
            preload = true;
            return;
        }
        View content = listView.getCellAt(this.sectionIndex, itemIndex);
        KrollDict d = new KrollDict(currentItem);
        ListItemData itemD = getItemDataAt(itemIndex);
        itemD.setProperties(d);
//        listItemData.set(index, itemD);
        hiddenItems.set(itemIndex, !itemD.isVisible());
        
        if (content != null) {
            TiBaseListViewItem listItem = (TiBaseListViewItem) content.findViewById(TiListView.listContentId);
            if (listItem != null) {
                if (listItem.getItemIndex() == itemIndex) {
                    TiListViewTemplate template = getListView().getTemplate(itemD.getTemplate());
                    populateViews(itemD, listItem, template, nonRealItemIndex, this.sectionIndex, content, false);
                }
                else {
                    Log.d(TAG, "wrong item index", Log.DEBUG_MODE);
                }
                return;
            }
        }
        notifyDataChange();
    }

	private boolean deleteItemsData(int index, int count) {
		boolean delete = false;
		
		while (count > 0) {
			if (index < itemProperties.size()) {
				itemProperties.remove(index);
				mItemCount--;
				delete = true;
			}
			if (index < listItemData.size()) {
				listItemData.remove(index);
			}
			if (index < hiddenItems.size()) {
				hiddenItems.remove(index);
			}
			count--;
		}
		updateCurrentItemCount();
		return delete;
	}
	
	public Object deleteItemData(int index) {
        if (0 <= index && index < itemProperties.size()) {
            hiddenItems.remove(index);
            listItemData.remove(index);
            mItemCount --;
            updateCurrentItemCount();
            return itemProperties.remove(index);
        }
        return null;
    }
	
	public void insertItemData(int index, Object data) {
	    if (itemProperties == null) {
            itemProperties = new ArrayList<Object>();
            itemProperties.add(data);
        } else {
            if (index < 0 || index > itemProperties.size()) {
                Log.e(TAG, "Invalid index to handleInsertItem",
                        Log.DEBUG_MODE);
                return;
            }
            itemProperties.add(data);
        }
        // only process items when listview's properties is processed.
        if (getListView() == null) {
            preload = true;
            return;
        }

        mItemCount += 1;
        if (listItemData != null && data instanceof HashMap) {
            KrollDict d = new KrollDict((HashMap) data);
            ListItemData itemD = new ListItemData(d);
            listItemData.add(index, itemD);
            hiddenItems.add(index, !itemD.isVisible());
        }
        updateCurrentItemCount();
    }

	private void handleDeleteItemsAt(int index, int count) {
	    TiListView listView = getListView();
	    if (listView != null) {
	        int position = listView.findItemPosition(sectionIndex, index);
	        listView.remove(position, count);
	    }
	    else {
	        deleteItemsData(index, count);
	        notifyDataChange();
	    }
		
	}

	private void handleReplaceItemsAt(int index, int count, Object data) {
		if (count == 0) {
			handleInsertItemsAt(index, data);
		} else if (deleteItemsData(index, count)) {
			handleInsertItemsAt(index, data);
		}
	}

//	private void handleUpdateItemAt(int index, Object data) {
//		handleReplaceItemsAt(index, 1, data);
//		setProperty(TiC.PROPERTY_ITEMS, itemProperties.toArray());
//	}

	/**
	 * This method creates a new cell and fill it with content. getView() calls
	 * this method when a view needs to be created.
	 * 
	 * @param sectionIndex
	 *            Entry's index relative to its section
	 * @return
	 */
	public void generateCellContent(int sectionIndex, final ListItemData item, 
			ListItemProxy itemProxy, TiBaseListViewItem itemContent, TiListViewTemplate template,
			int itemPosition, View item_layout) {
		// Create corresponding TiUIView for item proxy
		TiListItem listItem = new TiListItem(itemProxy, itemContent, item_layout);
		itemProxy.setView(listItem);
		itemContent.setView(listItem);
		itemProxy.realizeViews();

		if (template != null) {
			populateViews(item, itemContent, template, itemPosition,
					sectionIndex, item_layout, false);
		}
	}
	
	public int getUserItemIndexFromSectionPosition(final int position) {
	    int result = position;
//	    if (hasHeader()) {
//	        result -= 1;
//        }
	    return getRealPosition(result);
	}

	public void populateViews(final ListItemData item, TiBaseListViewItem cellContent, TiListViewTemplate template, int itemIndex, int sectionIndex,
			View item_layout, boolean reusing) {
		TiListItem listItem = (TiListItem)cellContent.getView();
		// Handling root item, since that is not in the views map.
		if (listItem == null) {
			return;
		}
		listItem.setReusing(reusing);
		int realItemIndex = getUserItemIndexFromSectionPosition(itemIndex);
		cellContent.setCurrentItem(sectionIndex, realItemIndex, this);
		
		KrollDict data = template.prepareDataDict(item.getProperties());
		ListItemProxy itemProxy = (ListItemProxy) cellContent.getView().getProxy();
		itemProxy.setCurrentItem(sectionIndex, itemIndex, this, item);
		itemProxy.setActivity(this.getActivity());

		KrollDict listItemProperties;
//		String itemId = null;

		if (data.containsKey(TiC.PROPERTY_PROPERTIES)) {
			listItemProperties = new KrollDict(
					(HashMap) data.get(TiC.PROPERTY_PROPERTIES));
		} else {
			listItemProperties = new KrollDict();
		}
		ProxyListItem rootItem = itemProxy.getListItem();
		
//		if (!reusing) {
	        KrollDict listViewProperties = getListView().getProxy().getProperties();
		    for (Map.Entry<String, String> entry : toPassProps.entrySet()) {
	            String inProp = entry.getKey();
	            String outProp = entry.getValue();
	            if (!listItemProperties.containsKey(outProp) && !rootItem.containsKey(outProp) && listViewProperties.containsKey(inProp)) {
	                listItemProperties.put(outProp, listViewProperties.get(inProp));
	            }
	        }
//		}
		

//		// find out if we need to update itemId
//		if (listItemProperties.containsKey(TiC.PROPERTY_ITEM_ID)) {
//			itemId = TiConvert.toString(listItemProperties
//					.get(TiC.PROPERTY_ITEM_ID));
//		}

		// update extra event data for list item
		itemProxy.setEventOverrideDelegate(itemProxy);

		HashMap<String, ProxyListItem> views = itemProxy.getBindings();
		// Loop through all our views and apply default properties
		for (String binding : views.keySet()) {
			ProxyListItem viewItem = views.get(binding);
			KrollProxy proxy  = viewItem.getProxy();
			if (proxy instanceof TiViewProxy) {
			    ((TiViewProxy) proxy).getOrCreateView();
			}
			KrollProxyListener modelListener = (KrollProxyListener) proxy.getModelListener();
			if (!(modelListener instanceof KrollProxyReusableListener)) {
                continue;
			}
			if (modelListener instanceof TiUIView) {
	            ((TiUIView)modelListener).setTouchDelegate((TiTouchDelegate) listItem);
            }
			// update extra event data for views
			proxy.setEventOverrideDelegate(itemProxy);
			// if binding is contain in data given to us, process that data,
			// otherwise
			// apply default properties.
			if (reusing) {
			    ((KrollProxyReusableListener) modelListener).setReusing(true);
			}
			KrollDict diffProperties = viewItem
                    .generateDiffProperties((HashMap) data.get(binding));
			
			if (diffProperties != null && !diffProperties.isEmpty()) {
			    if (reusing) {
	                modelListener.processApplyProperties(diffProperties);
			    } else {
	                modelListener.processProperties(diffProperties);
			    }
            }
            proxy.setSetPropertyListener(itemProxy);
            
			if (reusing) {
			    ((KrollProxyReusableListener) modelListener).setReusing(false);
			}
		}
		
		for (KrollProxy theProxy : itemProxy.getNonBindedProxies()) {
		    KrollProxyListener modelListener = (KrollProxyListener) theProxy.getModelListener();
		    if (modelListener instanceof KrollProxyReusableListener) {
		        if (modelListener instanceof TiUIView) {
	                ((TiUIView)modelListener).setTouchDelegate((TiTouchDelegate) listItem);
	            }
		        theProxy.setEventOverrideDelegate(itemProxy);
            }
		}

	    listItemProperties = itemProxy.getListItem()
                .generateDiffProperties(listItemProperties);

		if (!listItemProperties.isEmpty()) {
		    if (reusing) {
		        listItem.processApplyProperties(listItemProperties);
            } else {
                listItem.processProperties(listItemProperties);
            }
		}
        listItem.setReusing(false);
	}

	public String getTemplateByIndex(int index) {
//        if (hasHeader()) {
//			index -= 1;
//		}

		if (isFilterOn()) {
			return getItemDataAt(filterIndices.get(index)).getTemplate();
		} else {
			return getItemDataAt(index).getTemplate();
		}
	}

	public int getContentCount() {
		int totalCount = 0;
		if (hidden) return totalCount;
		if (isFilterOn()) {
			totalCount = filterIndices.size();
		} else {
			totalCount = mItemCount;
		}
		return totalCount - getHiddenCount();
	}
	
	private void updateCurrentItemCount() {
	    int totalCount = 0;
        if (!hidden) {
            if (isFilterOn()) {
                totalCount = filterIndices.size();
            } else {
                totalCount = mItemCount;
            }
        }
        else if (!hideHeaderOrFooter() && hasHeader()) {
            totalCount += 1;
        }

        if (!hideHeaderOrFooter()) {
//          if (hasHeader()) {
//              totalCount += 1;
//          }
            if (hasFooter()) {
                totalCount += 1;
            }
        }
        totalCount -= getHiddenCount();
        mCurrentItemCount = totalCount;
	}
	/**
	 * @return number of entries within section
	 */
	public int getItemCount() {
		return mCurrentItemCount;
	}

	private int getHiddenCount() {
		int count = 0;
		if (hidden || hiddenItems == null) return count;
		for (int i = 0; i < hiddenItems.size(); i++)
			if (hiddenItems.get(i) == true)
				count++;
		return count;
	}

	private boolean hideHeaderOrFooter() {
		TiListView listview = getListView();
		return (listview.getSearchText() != null && filterIndices.isEmpty());
	}

	public boolean isHeaderView(int pos) {
		return (hasHeader() && pos == 0);
	}

	public boolean isFooterView(int pos) {
		return (hasFooter() && pos == getItemCount() - 1);
	}

	public void setListView(TiListView l) {
		listView = new WeakReference<TiListView>(l);
	}

	public TiListView getListView() {
		if (listView != null) {
			return listView.get();
		}
		return null;
	}

	public ListItemData getItemDataAt(int position)
	{
		return listItemData.get(getRealPosition(position));
	}

//	public KrollDict getListItemData(int position) {
//		if (headerTitle != null || headerView != null) {
//			position -= 1;
//		}
//
//		if (isFilterOn()) {
//			return getItemDataAt(filterIndices.get(position))
//					.getProperties();
//		} else if (position >= 0 && position < getItemCount()) {
//			return getItemDataAt(position).getProperties();
//		}
//		return null;
//	}

	public ListItemData getListItem(int position) {
//        if (hasHeader()) {
//			position -= 1;
//		}

		if (isFilterOn()) {
			return getItemDataAt(filterIndices.get(position));
		} else if (position >= 0 && position < getItemCount()) {
			return getItemDataAt(position);
		}
		return null;
	}

	public boolean isFilterOn() {
	    String searchText = getListView().getSearchText();
	    return (searchText != null && searchText.length() > 0);
	}

	public void applyFilter(String searchText) {
		// Clear previous result
		filterIndices.clear();
		hidden = TiConvert.toBoolean(TiC.PROPERTY_VISIBLE, false);
		if (isFilterOn()) {
		    boolean caseInsensitive = getListView().getCaseInsensitive();
	        // Add new results
	        for (int i = 0; i < listItemData.size(); ++i) {
	            ListItemData data = listItemData.get(i);
	            String searchableText = data.getSearchableText();
	            if (searchableText == null) continue;
	            // Handle case sensitivity
	            if (caseInsensitive) {
	                searchText = searchText.toLowerCase();
	                searchableText = searchableText.toLowerCase();
	            }
	            // String comparison
	            if (data.isVisible() && searchableText != null && searchableText.contains(searchText)) {
	                filterIndices.add(getInverseRealPosition(i));
	            }
	        }
	        hidden = hidden || filterIndices.size() == 0;
		}
        updateCurrentItemCount();
	}

	public void release() {
		if (listItemData != null) {
			listItemData.clear();
			listItemData = null;
		}
		
		if (hiddenItems != null) {
			hiddenItems.clear();
			hiddenItems = null;
		}

		if (itemProperties != null) {
			itemProperties.clear();
			itemProperties = null;
		}
		mCurrentItemCount = 0;
		super.release();
	}

	@Override
	public String getApiName() {
		return "Ti.UI.ListSection";
	}

    public void setIndex(int index) {
        this.sectionIndex = index;
        
    }
    
    public int getIndex() {
        return this.sectionIndex;
    }

}
