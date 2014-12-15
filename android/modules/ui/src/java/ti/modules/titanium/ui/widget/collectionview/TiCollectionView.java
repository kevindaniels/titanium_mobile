/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.collectionview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.ParentingProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.view.TiBorderWrapperView;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.appcelerator.titanium.view.TiUINonViewGroupView;
import org.appcelerator.titanium.view.TiUIView;

import com.nhaarman.listviewanimations.ListViewAnimationsBaseAdapter;
import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListItemView;
import com.nhaarman.listviewanimations.itemmanipulation.swipemenu.MenuAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipemenu.SwipeMenuAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipemenu.SwipeMenuCallback;
import com.nhaarman.listviewanimations.util.Insertable;
import com.nhaarman.listviewanimations.util.Removable;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;

import se.emilsjolander.stickylistheaders.OnStickyHeaderChangedListener;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListViewAbstract;
import ti.modules.titanium.ui.SearchBarProxy;
import ti.modules.titanium.ui.UIModule;
import android.annotation.SuppressLint;
import ti.modules.titanium.ui.android.SearchViewProxy;
import ti.modules.titanium.ui.widget.CustomListView;
import ti.modules.titanium.ui.widget.collectionview.CollectionSectionProxy.CollectionItemData;
import ti.modules.titanium.ui.widget.listview.ListViewProxy;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar.OnSearchChangeListener;
import ti.modules.titanium.ui.widget.searchview.TiUISearchView;
import yaochangwei.pulltorefreshlistview.widget.RefreshableListView.OnPullListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

@SuppressLint("NewApi")
public class TiCollectionView extends TiUINonViewGroupView implements OnSearchChangeListener {

	private CustomListView listView;
	private TiBaseAdapter adapter;
	private ArrayList<CollectionSectionProxy> sections;
	private AtomicInteger itemTypeCount;
	private String defaultTemplateBinding;
	private HashMap<String, TiCollectionViewTemplate> templatesByBinding;
	public static int listContentId = 24123;
	public static int isCheck;
	public static int hasChild;
	public static int disclosure;
	public static int accessory = 24124;
	private int headerFooterId;
	public static LayoutInflater inflater;
	private int titleId;
	private int[] marker = new int[2];
	private View headerView;
	private View footerView;
    private TiViewProxy pullView;
    private TiViewProxy searchView;
	private String searchText;
	private boolean caseInsensitive;
	private RelativeLayout searchLayout;
	private static final String TAG = "TiCollectionView";
	private boolean hideKeyboardOnScroll = true;
	private boolean canShowMenus = false;
	
	private SwipeMenuAdapter mSwipeMenuAdapater;
	
	private static final String defaultTemplateKey = UIModule.LIST_ITEM_TEMPLATE_DEFAULT;
	private static final TiCollectionViewTemplate defaultTemplate = new TiDefaultCollectionViewTemplate(defaultTemplateKey);

	
	/* We cache properties that already applied to the recycled list tiem in ViewItem.java
	 * However, since Android randomly selects a cached view to recycle, our cached properties
	 * will not be in sync with the native view's properties when user changes those values via
	 * User Interaction - i.e click. For this reason, we create a list that contains the properties 
	 * that must be reset every time a view is recycled, to ensure synchronization. Currently, only
	 * "value" is in this list to correctly update the value of Ti.UI.Switch.
	 */
	public static List<String> MUST_SET_PROPERTIES = Arrays.asList(TiC.PROPERTY_VALUE, TiC.PROPERTY_AUTO_LINK, TiC.PROPERTY_TEXT, TiC.PROPERTY_HTML);
	
	public static final String MIN_SEARCH_HEIGHT = "50dp";
	public static final int HEADER_FOOTER_WRAP_ID = 12345;
	public static final int HEADER_FOOTER_VIEW_TYPE = 0;
	public static final int HEADER_FOOTER_TITLE_TYPE = 1;
	public static final int BUILT_IN_TEMPLATE_ITEM_TYPE = 2;
	public static final int CUSTOM_TEMPLATE_ITEM_TYPE = 3;
	
	
	
	private ListView getInternalCollectionView() {
        return listView.getWrappedList();
	}

	public class TiBaseAdapter extends ListViewAnimationsBaseAdapter implements StickyListHeadersAdapter
	, SectionIndexer, MenuAdapter , Insertable<Object> , Removable<Object>
	{

		Activity context;
		
		public TiBaseAdapter(Activity activity) {
		    super();
			context = activity;
		}
		
		public boolean hasStableIds() {
	        return true;
	    }

		@Override
		public int getCount() {
			int count = 0;
			synchronized (sections) {
			    for (int i = 0; i < sections.size(); i++) {
	                CollectionSectionProxy section = sections.get(i);
	                count += section.getItemCount();
	            }
            }
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		//One type for header/footer title, one for header/footer view, one for built-in template, and one type per custom template.
		@Override
		public int getViewTypeCount() {
			return itemTypeCount.get();
			
		}
		@Override
		public int getItemViewType(int position) {
			Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
			CollectionSectionProxy section = info.first;
			int sectionItemIndex = info.second.second;
			if (section.isHeaderTitle(sectionItemIndex) || section.isFooterTitle(sectionItemIndex))
				return HEADER_FOOTER_TITLE_TYPE;
			if (section.isHeaderView(sectionItemIndex) || section.isFooterView(sectionItemIndex)) {
				return HEADER_FOOTER_VIEW_TYPE;
			}
			return getTemplate(section.getTemplateByIndex(sectionItemIndex)).getType();			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Get section info from index
			Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
			CollectionSectionProxy section = info.first;
			if (section.hidden) {
			    return null; // possible because of WrapperView
			}
			int sectionItemIndex = info.second.second;
			int sectionIndex = info.second.first;
			//check marker
			if (sectionIndex > marker[0] || (sectionIndex == marker[0] && sectionItemIndex >= marker[1])) {
				if (proxy.hasListeners(TiC.EVENT_MARKER, false)) {
	                proxy.fireEvent(TiC.EVENT_MARKER, null, false, false);
				}
				resetMarker();
			}

			View content = convertView;

			//Handles header/footer views and titles.
//			if (section.isHeaderView(sectionItemIndex) || 
//			        section.isFooterView(sectionItemIndex) || 
//			        section.isHeaderTitle(sectionItemIndex) || 
//			        section.isFooterTitle(sectionItemIndex)) {
//				return null;
//			}
			
			if (section.isFooterView(sectionItemIndex)) {
                return section.getOrCreateFooterView(sectionItemIndex);
            } else if (section.isFooterTitle(sectionItemIndex)) {
                //No content to reuse, so we create a new view
                if (content == null) {
                    content = inflater.inflate(headerFooterId, null);
                }
                TextView title = (TextView)content.findViewById(titleId);
                title.setText(section.getHeaderOrFooterTitle(sectionItemIndex));
                return content;
            }
			
			//Handling templates
			CollectionItemData item = section.getCollectionItem(sectionItemIndex);
			KrollDict data = item.getProperties();
			TiCollectionViewTemplate template = getTemplate(item.getTemplate());
			
			TiBaseCollectionViewItem itemContent = null;
			if (content != null) {
				itemContent = (TiBaseCollectionViewItem) content.findViewById(listContentId);
				setBoundsForBaseItem(itemContent);
				boolean reusing = sectionIndex != itemContent.sectionIndex || 
						itemContent.itemIndex >= section.getItemCount() || 
						item != section.getCollectionItem(itemContent.itemIndex);
				section.populateViews(data, itemContent, template, sectionItemIndex, sectionIndex, content, reusing);
			} else {
				content = new TiBaseCollectionViewItemHolder(getContext());
				itemContent = (TiBaseCollectionViewItem) content.findViewById(listContentId);
				setBoundsForBaseItem(itemContent);
//				LayoutParams params = new LayoutParams();
//                params.autoFillsWidth = true;
//                params.width = LayoutParams.MATCH_PARENT;
//				itemContent.setLayoutParams(params);
				CollectionItemProxy itemProxy = template.generateCellProxy(data, proxy);
				itemProxy.setListProxy(getProxy());
				section.generateCellContent(sectionIndex, data, itemProxy, itemContent, template, sectionItemIndex, content);
			}
		    canShowMenus |= itemContent.getCollectionItem().canShowMenus();

			return content;

		}

		private void setBoundsForBaseItem(TiBaseCollectionViewItem item)  {
			TiBaseCollectionViewItemHolder holder;
			ViewParent parent = item.getParent();
			if (parent instanceof TiBaseCollectionViewItemHolder)
			{
				holder = (TiBaseCollectionViewItemHolder) parent;
			}
			else if (parent instanceof TiBorderWrapperView)
			{
				holder = (TiBaseCollectionViewItemHolder) parent.getParent();
			}
			else return;
			//here the parent cant be null as we inflated
			holder.setCollectionView(listView);
//			String minRowHeight = MIN_ROW_HEIGHT;
//			if (proxy != null && proxy.hasProperty(TiC.PROPERTY_MIN_ROW_HEIGHT)) {
//				minRowHeight = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_MIN_ROW_HEIGHT));
//			}
//			item.setMinHeight(TiConvert.toTiDimension(minRowHeight, TiDimension.TYPE_HEIGHT));
//			if (proxy == null) return;
//			if (proxy.hasProperty(TiC.PROPERTY_MAX_ROW_HEIGHT)) {
//				item.setMaxHeight(TiConvert.toTiDimension(proxy.getProperty(TiC.PROPERTY_MAX_ROW_HEIGHT), TiDimension.TYPE_HEIGHT));
//			}
		}
		
		@Override
		public void notifyDataSetChanged()
		{
		    canShowMenus = false;
			// save index and top position
			int index = listView.getFirstVisiblePosition();
			View v = listView.getListChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			super.notifyDataSetChanged();
			// restore
			//
			listView.getWrappedList().setSelectionFromTop(index, top);
		}

        @Override
        public long getHeaderId(int position) {
          //Get section info from index
            Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
            CollectionSectionProxy section = info.first;
            return section.getIndex();
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            //Get section info from index
            Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
            CollectionSectionProxy section = info.first;
            int sectionItemIndex = info.second.second;
            int sectionIndex = info.second.first;

            
            if (section.getHeaderView() != null) {
                return section.getOrCreateHeaderView();
            }
            else if (section.getHeaderTitle() != null) {
              //No content to reuse, so we create a new view
                View content = convertView;
                if (content == null || content.getTag() == null || (Integer)content.getTag() != headerFooterId) {
                    content = inflater.inflate(headerFooterId, null);
                    content.setTag(headerFooterId);
                }
                TextView title = (TextView)content.findViewById(titleId);
                title.setText(section.getHeaderTitle());
                return content;
            }
            if (convertView != null) {
                return convertView;
            }
            //StickyListHeaderView always wants a header
            return new FrameLayout(getContext());
        }

        @Override
        public Object[] getSections() {
            return sections.toArray();
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return getSectionFirstPosition(sectionIndex);
        }

        @Override
        public int getSectionForPosition(int position) {
            Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
            CollectionSectionProxy section = info.first;
            return section.getIndex();
        }
        
        @Override
        public Object remove(int position) {
            Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
            Object result = info.first.deleteItemData(info.second.second);
            notifyDataSetChanged();
            return result;
        }

        @Override
        public void add(int position, Object data) {
            Pair<CollectionSectionProxy, Pair<Integer, Integer>> info = getSectionInfoByEntryIndex(position);
            info.first.insertItemData(info.second.second, data);
            notifyDataSetChanged();
        }

        @Override
        public boolean canShowLeftMenu(int position, final DynamicListItemView view) {
            if (!canShowMenus) return false;
            TiBaseCollectionViewItem viewItem = (TiBaseCollectionViewItem) view.findViewById(TiCollectionView.listContentId);
            if (viewItem != null) {
                TiCollectionItem listItem = viewItem.getCollectionItem();
                return listItem.canShowLeftMenu();
            }
            return false;
        }

        @Override
        public boolean canShowRightMenu(int position, final DynamicListItemView view) {
            if (!canShowMenus) return false;
            TiBaseCollectionViewItem viewItem = (TiBaseCollectionViewItem) view.findViewById(TiCollectionView.listContentId);
            if (viewItem != null) {
                TiCollectionItem listItem = viewItem.getCollectionItem();
                return listItem.canShowRightMenu();
            }
            return false;
        }

        @Override
        public View[] getLeftButtons(int position, final DynamicListItemView view) {
            TiBaseCollectionViewItem viewItem = (TiBaseCollectionViewItem) view.findViewById(TiCollectionView.listContentId);
            if (viewItem != null) {
                TiCollectionItem listItem = viewItem.getCollectionItem();
                return listItem.getLeftButtons();
            }
            return null;
        }

        @Override
        public View[] getRightButtons(int position, final DynamicListItemView view) {
            TiBaseCollectionViewItem viewItem = (TiBaseCollectionViewItem) view.findViewById(TiCollectionView.listContentId);
            if (viewItem != null) {
                TiCollectionItem listItem = viewItem.getCollectionItem();
                return listItem.getRightButtons();
            }
            return null;
        }
	}
	
	private Dictionary<Integer, Integer> listViewItemHeights = new Hashtable<Integer, Integer>();

    public int getScroll() {
        View c = listView.getListChildAt(0); //this is the first visible row
        int scrollY = -c.getTop();
        int first = listView.getFirstVisiblePosition();
        listViewItemHeights.put(first, c.getHeight());
        for (int i = 0; i < first; ++i) {
            if (listViewItemHeights.get(i) != null) // (this is a sanity check)
                scrollY += listViewItemHeights.get(i); //add all heights of the views that are gone
        }
        return scrollY;
    }
    
    public int getViewHeigth(View v) {
        int viewPosition = listView.getPositionForView(v);
        int scrollY = 0;
        for (int i = 0; i < viewPosition; ++i) {
                scrollY += listView.getListChildAt(i).getHeight();
        }
        return scrollY;
    }
	
	private KrollDict dictForScrollEvent() {
		KrollDict eventArgs = new KrollDict();
		KrollDict size = new KrollDict();
		size.put(TiC.PROPERTY_WIDTH, TiCollectionView.this.getNativeView().getWidth());
		size.put(TiC.PROPERTY_HEIGHT, TiCollectionView.this.getNativeView().getHeight());
		eventArgs.put(TiC.PROPERTY_SIZE, size);
		
        int firstVisibleItem = listView.getFirstVisiblePosition();
        int lastVisiblePosition = listView.getLastVisiblePosition();
		eventArgs.put("firstVisibleItem", firstVisibleItem);
        eventArgs.put("visibleItemCount", lastVisiblePosition - firstVisibleItem);
        eventArgs.put("contentOffset", getScroll());
//		View view = listView.getChildAt(0);
//		if (view != null) {
//	        eventArgs.put("contentOffset", view.getTop());
//		}
//		else {
//		    Log.d(TAG, "not normal");
//		}
		
		return eventArgs;
	}
	
	private SwipeMenuCallback mMenuCallback = new SwipeMenuCallback() {
        @Override
        public void onStartSwipe(View view, int position, int direction) {

        }

        @Override
        public void onMenuShown(View view, int position, int direction) {

        }

        @Override
        public void onMenuClosed(View view, int position, int direction) {

        }

        @Override
        public void beforeMenuShow(View view, int position, int direction) {

        }

        @Override
        public void beforeMenuClose(View view, int position, int direction) {

        }

    };

	public TiCollectionView(TiViewProxy proxy, Activity activity) {
		super(proxy);
		
		//initializing variables
		sections = new ArrayList<CollectionSectionProxy>();
		itemTypeCount = new AtomicInteger(CUSTOM_TEMPLATE_ITEM_TYPE);
		templatesByBinding = new HashMap<String, TiCollectionViewTemplate>();
		defaultTemplateBinding = defaultTemplateKey;
		templatesByBinding.put(defaultTemplateKey, defaultTemplate);
		defaultTemplate.setType(BUILT_IN_TEMPLATE_ITEM_TYPE);
		caseInsensitive = true;
		
		//handling marker
		HashMap<String, Integer> preloadMarker = ((CollectionViewProxy)proxy).getPreloadMarker();
		if (preloadMarker != null) {
			setMarker(preloadMarker);
		} else {
			resetMarker();
		}
		
		final KrollProxy fProxy = proxy;
		//initializing listView
		listView = new CustomListView(activity) {
		    
		    @Override
	        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	            
	            super.onLayout(changed, left, top, right, bottom);
	            if (changed && fProxy != null && fProxy.hasListeners(TiC.EVENT_POST_LAYOUT, false)) {
	                fProxy.fireEvent(TiC.EVENT_POST_LAYOUT, null);
	            }
	        }
	        
	        @Override
	        public boolean dispatchTouchEvent(MotionEvent event) {
	            if (touchPassThrough == true)
	                return false;
	            return super.dispatchTouchEvent(event);
	        }
	        
			@Override
		    protected void dispatchDraw(Canvas canvas) {
		        try {
		            super.dispatchDraw(canvas);
		        } catch (IndexOutOfBoundsException e) {
		            // samsung error
		        }
		    }
		};
		listView.setDuplicateParentStateEnabled(true);
		
		adapter = new TiBaseAdapter(activity);
		listView.setOnScrollListener(new OnScrollListener()
		{
			private boolean scrollValid = false;
			private int lastValidfirstItem = 0;
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
                
				view.requestDisallowInterceptTouchEvent(scrollState != ViewPager.SCROLL_STATE_IDLE);		
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
				    if (scrollValid) {
				        scrollValid = false;
	                    if (!fProxy.hasListeners(TiC.EVENT_SCROLLEND, false)) return;
	                    fProxy.fireEvent(TiC.EVENT_SCROLLEND, dictForScrollEvent(), false, false);
				    }
					
				}
				else if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
				    if (hideKeyboardOnScroll && hasFocus()) {
	                    blur();
	                }
					if (scrollValid == false) {
						scrollValid = true;
						if (!fProxy.hasListeners(TiC.EVENT_SCROLLSTART, false)) return;
						fProxy.fireEvent(TiC.EVENT_SCROLLSTART, dictForScrollEvent(), false, false);
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
//				Log.d(TAG, "onScroll : " + firstVisibleItem, Log.DEBUG_MODE);
				boolean fireScroll = scrollValid;
				if (!fireScroll && visibleItemCount > 0) {
					//Items in a list can be selected with a track ball in which case
					//we must check to see if the first visibleItem has changed.
					fireScroll = (lastValidfirstItem != firstVisibleItem);
				}
				if(fireScroll && fProxy.hasListeners(TiC.EVENT_SCROLL, false)) {
					lastValidfirstItem = firstVisibleItem;
					fProxy.fireEvent(TiC.EVENT_SCROLL, dictForScrollEvent(), false, false);
				}
			}
		});
		listView.setOnPullListener( new OnPullListener() {
			private boolean canUpdate = false;
			@Override
			public void onPull(boolean canUpdate) {
				if (canUpdate != this.canUpdate) {
					this.canUpdate = canUpdate;
					if(fProxy.hasListeners(TiC.EVENT_PULL_CHANGED, false)) {
						KrollDict event = dictForScrollEvent();
						event.put("active", canUpdate);
						fProxy.fireEvent(TiC.EVENT_PULL_CHANGED, event, false, false);
					}
				}
				if(fProxy.hasListeners(TiC.EVENT_PULL, false)) {
					KrollDict event = dictForScrollEvent();
					event.put("active", canUpdate);
					fProxy.fireEvent(TiC.EVENT_PULL, event, false, false);
				}
			}
	
			@Override
			public void onPullEnd(boolean canUpdate) {
				if(fProxy.hasListeners(TiC.EVENT_PULL_END, false)) {
					KrollDict event = dictForScrollEvent();
					event.put("active", canUpdate);
					fProxy.fireEvent(TiC.EVENT_PULL_END, event, false, false);
				}
			}
		});
		
		listView.setOnStickyHeaderChangedListener(new OnStickyHeaderChangedListener() {
            
            @Override
            public void onStickyHeaderChanged(StickyListHeadersListViewAbstract l, View header,
                    int itemPosition, long headerId) {
                //for us headerId is the section index
                int sectionIndex = (int) headerId;
                if (fProxy.hasListeners(TiC.EVENT_HEADER_CHANGE, false)) {
                    KrollDict data = new KrollDict();
                    CollectionSectionProxy section = null;
                    synchronized (sections) {
                        if (sectionIndex >= 0 && sectionIndex < sections.size()) {
                            section = sections.get(sectionIndex);
                        }
                        else {
                            return;
                        }
                    }
                    data.put(TiC.PROPERTY_HEADER_VIEW, section.getCurrentHeaderViewProxy());
                    data.put(TiC.PROPERTY_SECTION, section);
                    data.put(TiC.PROPERTY_SECTION_INDEX, sectionIndex);
                    fProxy.fireEvent(TiC.EVENT_HEADER_CHANGE, data, false, false);
                }
            }
        });

		
		//init inflater
		if (inflater == null) {
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		getInternalCollectionView().setCacheColorHint(Color.TRANSPARENT);
		listView.setEnabled(true);
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;
//		listView.setFocusable(false);
		listView.setFocusable(true);
		listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

		try {
			headerFooterId = TiRHelper.getResource("layout.titanium_ui_list_header_or_footer");
			titleId = TiRHelper.getResource("id.titanium_ui_list_header_or_footer_title");
			isCheck = TiRHelper.getResource("drawable.btn_check_buttonless_on_64");
			hasChild = TiRHelper.getResource("drawable.btn_more_64");
			disclosure = TiRHelper.getResource("drawable.disclosure_64");
		} catch (ResourceNotFoundException e) {
			Log.e(TAG, "XML resources could not be found!!!", Log.DEBUG_MODE);
		}
		
		setNativeView(listView);
	}
	
	@Override
	protected void handleTouchEvent(MotionEvent event) {
	    super.handleTouchEvent(event);
	    if (event.getAction() == MotionEvent.ACTION_UP) {
	        final int x = (int) event.getX();
            final int y = (int) event.getY();
            int motionPosition = listView.getWrappedList().pointToPosition(x, y);
            if (motionPosition == -1) {
                listView.performClick();
            }
        }
    }
	
	public String getSearchText() {
		return searchText;
	}
	
	public boolean getCaseInsensitive() {
		return caseInsensitive;
	}

	private void resetMarker() 
	{
		marker[0] = Integer.MAX_VALUE;
		marker[1] = Integer.MAX_VALUE;
	}

	public void setHeaderTitle(String title) {
		TextView textView = (TextView) headerView.findViewById(titleId);
		if (textView == null) return;
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}
	
	public void setFooterTitle(String title) {
		TextView textView = (TextView) footerView.findViewById(titleId);
        if (textView == null) return;
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}

	private TiUIView layoutHeaderOrFooter(TiViewProxy viewProxy)
	{
		//We are always going to create a new view here. So detach outer view here and recreate
		View outerView = (viewProxy.peekView() == null) ? null : viewProxy.peekView().getOuterView();
		if (outerView != null) {
			ViewParent vParent = outerView.getParent();
			if ( vParent instanceof ViewGroup ) {
				((ViewGroup)vParent).removeView(outerView);
			}
		}
		TiUIView tiView = viewProxy.forceCreateView();
		View nativeView = tiView.getOuterView();
		TiCompositeLayout.LayoutParams params = tiView.getLayoutParams();

		int width = AbsListView.LayoutParams.WRAP_CONTENT;
		int height = AbsListView.LayoutParams.WRAP_CONTENT;
		if (params.sizeOrFillHeightEnabled) {
			if (params.autoFillsHeight) {
				height = AbsListView.LayoutParams.MATCH_PARENT;
			}
		} else if (params.optionHeight != null) {
			height = params.optionHeight.getAsPixels(listView);
		}
		if (params.sizeOrFillWidthEnabled) {
			if (params.autoFillsWidth) {
				width = AbsListView.LayoutParams.MATCH_PARENT;
			}
		} else if (params.optionWidth != null) {
			width = params.optionWidth.getAsPixels(listView);
		}
		AbsListView.LayoutParams p = new AbsListView.LayoutParams(width, height);
		nativeView.setLayoutParams(p);
		return tiView;
	}

	public void setSeparatorStyle(int separatorHeight) {
		Drawable drawable = listView.getDivider();
		listView.setDivider(drawable);
		listView.setDividerHeight(separatorHeight);
	}

	@Override
	public void registerForTouch()
	{
		registerForTouch(listView);
	}
	
	public void setMarker(HashMap<String, Integer> markerItem) 
	{
		marker[0] = markerItem.get(TiC.PROPERTY_SECTION_INDEX);
		marker[1] = markerItem.get(TiC.PROPERTY_ITEM_INDEX);
		
	}

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_TEMPLATES:
            processTemplates((HashMap)newValue);
            if (changedProperty && adapter != null) {
                adapter.notifyDataSetChanged();
            }
            break;
        case TiC.PROPERTY_SEARCH_TEXT:
            this.searchText = TiConvert.toString(newValue);
            if (changedProperty) {
                reFilter(this.searchText);
            }
            break;
        case TiC.PROPERTY_SEARCH_VIEW:
            setSearchView(newValue, true);
            break;
        case TiC.PROPERTY_SEARCH_VIEW_EXTERNAL:
            setSearchView(newValue, false);
            break;
        case TiC.PROPERTY_SCROLL_HIDES_KEYBOARD:
            this.hideKeyboardOnScroll = TiConvert.toBoolean(newValue, true);
            break;
        case TiC.PROPERTY_STICKY_HEADERS:
            listView.setAreHeadersSticky(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_CASE_INSENSITIVE_SEARCH:
            this.caseInsensitive = TiConvert.toBoolean(newValue, true);
            if (changedProperty) {
                reFilter(this.searchText);
            }
            break;
        case TiC.PROPERTY_SEPARATOR_COLOR:
            setSeparatorColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED:
            getInternalCollectionView().setFooterDividersEnabled(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_HEADER_DIVIDERS_ENABLED:
            getInternalCollectionView().setHeaderDividersEnabled(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR:
            listView.setVerticalScrollBarEnabled(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE:
            defaultTemplateBinding = TiConvert.toString(newValue);
            if (changedProperty) {
                refreshItems();
            }
            break;
        case TiC.PROPERTY_SECTIONS:
            if (changedProperty) {
                processSectionsAndNotify((Object[])newValue);
            } else {
                //if user didn't append/modify/delete sections before this is called, we process sections
                //as usual. Otherwise, we process the preloadSections, which should also contain the section(s)
                //from this dictionary as well as other sections that user append/insert/deleted prior to this.
                ListViewProxy listProxy = (ListViewProxy) proxy;
                if (!listProxy.getPreload()) {
                    processSections((Object[])newValue);
                } else {
                    processSections(listProxy.getPreloadSections().toArray());
                }
            }
            break;
        case TiC.PROPERTY_SEPARATOR_STYLE:
            setSeparatorStyle(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_OVER_SCROLL_MODE:
//            if (Build.VERSION.SDK_INT >= 9) {
                listView.setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
//            }
            break;
        case TiC.PROPERTY_HEADER_VIEW:
            setHeaderOrFooterView(newValue, true);
            break;
        case TiC.PROPERTY_HEADER_TITLE:
            if (headerView == null || headerView.getId() != HEADER_FOOTER_WRAP_ID) {
                if (headerView == null) {
                    headerView = inflater.inflate(headerFooterId, null);
                }
                setHeaderTitle(TiConvert.toString(newValue));
            }
            break;
        case TiC.PROPERTY_FOOTER_VIEW:
            setHeaderOrFooterView(newValue, false);
            break;
        case TiC.PROPERTY_FOOTER_TITLE:
            if (footerView == null || footerView.getId() != HEADER_FOOTER_WRAP_ID) {
                if (footerView == null) {
                    footerView = inflater.inflate(headerFooterId, null);
                }
                setFooterTitle(TiConvert.toString(newValue));
            }
            break;
        case TiC.PROPERTY_SCROLLING_ENABLED:
            listView.setScrollingEnabled(newValue);
            break;
        case TiC.PROPERTY_PULL_VIEW:
            listView.setHeaderPullView(setPullView(newValue));
            break;
        
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
    
    @Override
    public void processProperties(KrollDict d) {
        
        super.processProperties(d);

        ListViewProxy listProxy = (ListViewProxy) proxy;
        listProxy.clearPreloadSections();
        listProxy.setPreload(false);

        //Check to see if headerView and footerView are specified. If not, we hide the views
        if (headerView == null) {
            headerView = inflater.inflate(headerFooterId, null);
            headerView.findViewById(titleId).setVisibility(View.GONE);
        }
        
        if (footerView == null) {
            footerView = inflater.inflate(headerFooterId, null);
            footerView.findViewById(titleId).setVisibility(View.GONE);
        }

        //Have to add header and footer before setting adapter
        listView.addHeaderView(headerView, null, false);
        listView.addFooterView(footerView, null, false);
        
        mSwipeMenuAdapater = new SwipeMenuAdapter(adapter, getProxy().getActivity(), mMenuCallback);
        StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(mSwipeMenuAdapater);
        stickyListHeadersAdapterDecorator.setListViewWrapper(new StickyListHeadersListViewWrapper(listView));
        listView.setAdapter(stickyListHeadersAdapterDecorator);
        
    }

	private void layoutSearchView(TiViewProxy searchView) {
	    if (searchLayout != null) {
            searchLayout.removeAllViews();
        }
		TiUIView search = searchView.getOrCreateView();
		RelativeLayout layout = new RelativeLayout(proxy.getActivity());
		layout.setGravity(Gravity.NO_GRAVITY);
		layout.setPadding(0, 0, 0, 0);
		addSearchLayout(layout, searchView, search);
		setNativeView(layout);	
	}
	
	private void addSearchLayout(RelativeLayout layout, TiViewProxy searchView, TiUIView search) {
		RelativeLayout.LayoutParams p = createBasicSearchLayout();
		p.addRule(RelativeLayout.ALIGN_PARENT_TOP);

		TiDimension rawHeight;
		if (searchView.hasProperty(TiC.PROPERTY_HEIGHT)) {
			rawHeight = TiConvert.toTiDimension(searchView.getProperty(TiC.PROPERTY_HEIGHT), 0);
		} else {
			rawHeight = TiConvert.toTiDimension(MIN_SEARCH_HEIGHT, 0);
		}
		p.height = rawHeight.getAsPixels(layout);

		View nativeView = search.getNativeView();
		layout.addView(nativeView, p);

		p = createBasicSearchLayout();
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		p.addRule(RelativeLayout.BELOW, nativeView.getId());
		ViewParent parentView = getNativeView().getParent();
		if (parentView instanceof ViewGroup) {
			//get the previous layout params so we can reset with new layout
			ViewGroup.LayoutParams lp = getNativeView().getLayoutParams();
//			ViewGroup parentView = (ViewGroup) parentView;
			//remove view from parent
			((ViewGroup) parentView).removeView(getNativeView());
			//add new layout
			layout.addView(getNativeView(), p);
			((ViewGroup) parentView).addView(layout, lp);
			
		} else {
			layout.addView(getNativeView(), p);
		}
		this.searchLayout = layout;
	}

	private RelativeLayout.LayoutParams createBasicSearchLayout() {
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		return p;
	}
	private void setHeaderOrFooterView (Object viewObj, boolean isHeader) {
		View view = layoutHeaderOrFooterView(viewObj, proxy);
		if (view != null) {
			if (isHeader) {
				headerView = view;
			} else {
				footerView = view;
			}
		}
	}
	
	private void setSearchView (Object viewObj, boolean addInHeader) {
        if (searchView != null) {
            searchView.releaseViews(true);
        }
        if (isSearchViewValid(viewObj)) {
            searchView = (TiViewProxy) viewObj;
            TiUIView search = searchView.getOrCreateView();
            setSearchListener(searchView, search);
            if (addInHeader) layoutSearchView(searchView);
        } else {
            Log.e(TAG, "Searchview type is invalid");
        }
    }
	
	private View setPullView (Object viewObj) {
		if (pullView != null) {
			pullView.releaseViews(true);
			pullView.setParent(null);
		}
		pullView = (TiViewProxy) viewObj;
		return layoutHeaderOrFooterView(viewObj, proxy);
	}
	
	public void showPullView(boolean animated) {
		if (pullView != null) {
			listView.showHeaderPullView(animated);
		}
	}
	
	public void closePullView(boolean animated) {
		if (pullView != null) {
			listView.closeHeaderPullView(animated);
		}
	}
    
    public void closeSwipeMenu(boolean animated) {
        if (mSwipeMenuAdapater != null) {
            if (animated) {
                mSwipeMenuAdapater.closeMenusAnimated();
            }
            else {
                mSwipeMenuAdapater.closeMenus();
            }
        }
    }


	private void reFilter(String searchText) {
        synchronized (sections) {
			for (int i = 0; i < sections.size(); ++i) {
				CollectionSectionProxy section = sections.get(i);
				section.applyFilter(searchText);
			}
		}
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private boolean isSearchViewValid(Object proxy) {
		if (proxy instanceof SearchBarProxy || proxy instanceof SearchViewProxy) {
			return true;
		} else {
			return false;
		}
	}

	private void setSearchListener(TiViewProxy searchView, TiUIView search) 
	{
		if (searchView instanceof SearchBarProxy) {
			((TiUISearchBar)search).setOnSearchChangeListener(this);
		} else if (searchView instanceof SearchViewProxy) {
			((TiUISearchView)search).setOnSearchChangeListener(this);
		}
	}

	private void setSeparatorColor(int color) {
		int dividerHeight = listView.getDividerHeight();
		listView.setDivider(new ColorDrawable(color));
		listView.setDividerHeight(dividerHeight);
	}

	private void refreshItems() {
	    synchronized (sections) {
	        for (int i = 0; i < sections.size(); i++) {
	            CollectionSectionProxy section = sections.get(i);
	            section.refreshItems();
	        }
        }
	}
	
	
	public TiCollectionViewTemplate getTemplate(String template)
	{
		if (template == null) template = defaultTemplateBinding;
		if (templatesByBinding.containsKey(template))
		{
			return templatesByBinding.get(template);
		}
		return templatesByBinding.get(UIModule.LIST_ITEM_TEMPLATE_DEFAULT);
	}

	protected void processTemplates(HashMap<String,Object> templates) {
		templatesByBinding = new HashMap<String, TiCollectionViewTemplate>();
		templatesByBinding.put(defaultTemplateKey, defaultTemplate);
		if(templates != null) {
			for (String key : templates.keySet()) {
				HashMap templateDict = (HashMap)templates.get(key);
				if (templateDict != null) {
					//Here we bind each template with a key so we can use it to look up later
					KrollDict properties = new KrollDict((HashMap)templates.get(key));
					TiCollectionViewTemplate template = new TiCollectionViewTemplate(key, properties);
					template.setType(getItemType());
					templatesByBinding.put(key, template);
				}
				else {
					Log.e(TAG, "null template definition: " + key);
				}
			}
		}
	}
	
	public View layoutHeaderOrFooterView (Object object, TiViewProxy parent) {
		TiViewProxy viewProxy = null;
		if (object instanceof TiViewProxy) {
			viewProxy = (TiViewProxy)object;
		}
		else if(object instanceof HashMap) {
			viewProxy = (TiViewProxy) proxy.createProxyFromTemplate((HashMap) object, parent, true);
		}
		if (viewProxy == null) return null;
		viewProxy.setParentForBubbling(this.proxy);
		TiUIView tiView = viewProxy.peekView();
		if (tiView != null) {
			ParentingProxy parentProxy = viewProxy.getParent();
			//Remove parent view if possible
			if (parentProxy != null && parentProxy instanceof TiViewProxy) {
				TiUIView parentView = ((TiViewProxy) parentProxy).peekView();
				if (parentView != null) {
					parentView.remove(tiView);
				}
			}
		} else {
			tiView = viewProxy.forceCreateView();
		}
		
		View outerView = null;
		ViewGroup parentView = null;
		if (tiView != null) {
		    outerView = tiView.getOuterView();
	        parentView = (ViewGroup) outerView.getParent();
		}
		if (parentView != null && parentView.getId() == HEADER_FOOTER_WRAP_ID) {
			return parentView;
		} else {
			//add a wrapper so layout params such as height, width takes in effect.
			TiCompositeLayout wrapper = new TiCompositeLayout(viewProxy.getActivity(), LayoutArrangement.DEFAULT, null);
			AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,  AbsListView.LayoutParams.WRAP_CONTENT);
			wrapper.setLayoutParams(params);
			if (outerView != null) {
	            wrapper.addView(outerView, tiView.getLayoutParams());
			}
			wrapper.setId(HEADER_FOOTER_WRAP_ID);
			return wrapper;
		}
	}

	protected void processSections(Object[] sections) {
		synchronized (this.sections) {
		    this.sections.clear();
	        for (int i = 0; i < sections.length; i++) {
	            processSection(sections[i], -1);
	        }
        }
	}
	
	protected void processSectionsAndNotify(Object[] sections) {
	    (new ProcessSectionsTask()).execute(sections);
//		processSections(sections);
//		if (adapter != null) {
//			adapter.notifyDataSetChanged();
//		}
	}
	
private class ProcessSectionsTask extends AsyncTask<Object[], Void, Void> {
        
        @Override
        protected Void doInBackground(Object[]... params) {
            processSections(params[0]);
            CollectionViewProxy listProxy = (CollectionViewProxy) proxy;
            listProxy.clearPreloadSections();
            listProxy.setPreload(false);
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

    }
	
	protected void processSection(Object sec, int index) {
		if (sec instanceof CollectionSectionProxy) {
			CollectionSectionProxy section = (CollectionSectionProxy) sec;
            section.setCollectionView(this);
            section.setAdapter(adapter);
			synchronized (sections) {
			    if (this.sections.contains(section)) {
	                return;
	            }
	            if (index == -1 || index >= sections.size()) {
	                section.setIndex(this.sections.size());
	                this.sections.add(section);
	            } else {
	                section.setIndex(index);
	                this.sections.add(index, section);
	            }
            }
			
			//Attempts to set type for existing templates.
//			section.setTemplateType();
			//Process preload data if any
			section.processPreloadData();
			//Apply filter if necessary
			if (searchText != null) {
				section.applyFilter(searchText);
			}
		}
		else if(sec instanceof HashMap) {
			CollectionSectionProxy section = (CollectionSectionProxy) KrollProxy.createProxy(CollectionSectionProxy.class, null, new Object[]{sec}, null);
			processSection(section, index);
		}
	}
	
	public KrollDict getItem(int sectionIndex, int itemIndex) {
		if (sectionIndex < 0 || sectionIndex >= sections.size()) {
			Log.e(TAG, "getItem Invalid section index");
			return null;
		}
		synchronized (sections) {
	        return sections.get(sectionIndex).getItemAt(itemIndex);
        }
	}
	
	public CollectionSectionProxy getSectionAt(int sectionIndex) {
        synchronized (sections) {
    		if (sectionIndex < 0 || sectionIndex >= sections.size()) {
    			Log.e(TAG, "getItem Invalid section index");
    			return null;
    		}
    		
    		return sections.get(sectionIndex);
        }
	}
	
	protected Pair<CollectionSectionProxy, Pair<Integer, Integer>> getSectionInfoByEntryIndex(int index) {
		if (index < 0) {
			return null;
		}
        synchronized (sections) {
    		for (int i = 0; i < sections.size(); i++) {
    			CollectionSectionProxy section = sections.get(i);
    			int sectionItemCount = section.getItemCount();
    			if (index <= sectionItemCount - 1) {
    				return new Pair<CollectionSectionProxy, Pair<Integer, Integer>>(section, new Pair<Integer, Integer>(i, index));
    			} else {
    				index -= sectionItemCount;
    			}
    		}
        }

		return null;
	}
	
	protected int getSectionFirstPosition(int sectionIndex) {
        int result = 0;
        synchronized (sections) {
            for (int i = 0; i < sectionIndex; i++) {
                CollectionSectionProxy section = sections.get(i);
                int sectionItemCount = section.getItemCount();
                result += sections.get(i).getItemCount();
            }
        }

        return result;
    }
    
	
	public int getItemType() {
		return itemTypeCount.getAndIncrement();
	}
	
	public TiCollectionViewTemplate getTemplateByBinding(String binding) {
		return templatesByBinding.get(binding);
	}
	
	public String getDefaultTemplateBinding() {
		return defaultTemplateBinding;
	}
	
	public int getSectionCount() {
        synchronized (sections) {
            return sections.size();
        }
	}
	
	public void appendSection(Object section) {
		if (section instanceof Object[]) {
			Object[] secs = (Object[]) section;
			for (int i = 0; i < secs.length; i++) {
				processSection(secs[i], -1);
			}
		} else {
			processSection(section, -1);
		}
		adapter.notifyDataSetChanged();
	}
	
	public void deleteSectionAt(int index) {
        synchronized (sections) {
    		if (index >= 0 && index < sections.size()) {
    			sections.remove(index);
    			adapter.notifyDataSetChanged();
    		} else {
    			Log.e(TAG, "Invalid index to delete section");
    		}
        }
	}
	
	public void insertSectionAt(int index, Object section) {
        synchronized (sections) {
    		if (index > sections.size()) {
    			Log.e(TAG, "Invalid index to insert/replace section");
    			return;
    		}
        }
		if (section instanceof Object[]) {
			Object[] secs = (Object[]) section;
			for (int i = 0; i < secs.length; i++) {
				processSection(secs[i], index);
				index++;
			}
		} else {
			processSection(section, index);
		}
		adapter.notifyDataSetChanged();
	}
	
	public void replaceSectionAt(int index, Object section) {
		deleteSectionAt(index);
		insertSectionAt(index, section);
	}
	
	public int findItemPosition(int sectionIndex, int sectionItemIndex) {
		int position = 0;
        synchronized (sections) {
    		for (int i = 0; i < sections.size(); i++) {
    			CollectionSectionProxy section = sections.get(i);
    			if (i == sectionIndex) {
    				if (sectionItemIndex >= section.getContentCount()) {
    					Log.e(TAG, "Invalid item index");
    					return -1;
    				}
    				position += sectionItemIndex;
    				if (section.hasHeader()) {
    					position += 1;			
    				}
    				break;
    			} else {
    				position += section.getItemCount();
    			}
    		}
        }
		return position;
	}
	
	public int getHeaderViewCount() {
	    return listView.getHeaderViewsCount();
	}

	private int getCount() {
		if (adapter != null) {
			return adapter.getCount();
		}
		return 0;
	}
	
	public static void ensureVisible(CustomListView listView, int pos)
	{
	    if (listView == null)
	    {
	        return;
	    }

	    if(pos < 0 || pos >= listView.getCount())
	    {
	        return;
	    }

	    int first = listView.getFirstVisiblePosition();
	    int last = listView.getLastVisiblePosition();

	    if (pos < first)
	    {
	        listView.setSelection(pos);
	        return;
	    }

	    if (pos >= last)
	    {
	        listView.setSelection(1 + pos - (last - first));
	        return;
	    }
	}
	
	protected void scrollToItem(int sectionIndex, int sectionItemIndex, boolean animated) {
		final int position = findItemPosition(sectionIndex, sectionItemIndex);
		if (position > -1) {
			if (animated)
				listView.smoothScrollToPosition(position + 1);
			else
				ensureVisible(listView, position + 1);
		}
	}

	public void scrollToTop(final int y, boolean animated)
	{
		if (animated) {
			listView.smoothScrollToPosition(0);
		}
		else {
			listView.setSelection(0); 
		}
	}

	public void scrollToBottom(final int y, boolean animated)
	{
		//strangely if i put getCount()-1 it doesnt go to the full bottom but make sure the -1 is shown …
		if (animated) {
			listView.smoothScrollToPosition(getCount()-1);
		}
		else {
			listView.setSelection(getCount()-1);
		}
	}

	
	public void release() {
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).releaseViews();
		}
		
		templatesByBinding.clear();
		sections.clear();
		
//		if (wrapper != null) {
//			wrapper = null;
//		}

		if (listView != null) {
			listView.setAdapter(null);
			listView = null;
		}
		if (headerView != null) {
			headerView = null;
		}
		if (footerView != null) {
			footerView = null;
		}
		
		if (pullView != null) {
			pullView = null;
		}

		super.release();
	}

	@Override
	public void filterBy(String text)
	{
		this.searchText = text;
		reFilter(text);
	}
	
	public CollectionSectionProxy[] getSections()
	{
		return sections.toArray(new CollectionSectionProxy[sections.size()]);
	}
	
	public KrollProxy getChildByBindId(int sectionIndex, int itemIndex, String bindId) {
	    
	    View content = getCellAt(sectionIndex, itemIndex);
        if (content != null) {
            TiBaseCollectionViewItem listItem = (TiBaseCollectionViewItem) content.findViewById(TiCollectionView.listContentId);
            if (listItem != null) {
                if (listItem.getItemIndex() == itemIndex) {
                    return listItem.getViewProxyFromBinding(bindId);
                }
			}
		}
		return null;
	}
	
	public View getCellAt(int sectionIndex, int itemIndex) {
        int position = findItemPosition(sectionIndex, itemIndex);
        int childCount = listView.getListChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = listView.getListChildAt(i);
            TiBaseCollectionViewItem itemContent = (TiBaseCollectionViewItem) child.findViewById(listContentId);
            if (itemContent != null) {
                //first visible item of ours
                int firstposition = findItemPosition(itemContent.getSectionIndex(), itemContent.getItemIndex());
                position -= firstposition;
                break;
            }
            else {
                position++;
            }
        }
        if (position > -1) {
            View content = listView.getListChildAt(position);
            return content;
            
        }
        return null;
    }
	
    public void insert(final int position, final Object item) {
        listView.insert(position, item);
    }

    public void insert(final int position, final Object... items) {
        listView.insert(position, items);
    }

    public <T> void remove( final int position) {
        listView.remove(position - listView.getHeaderViewsCount());
    }

    public <T> void remove( final int position, final int count) {
        listView.remove(position - listView.getHeaderViewsCount(), count);
    }
}