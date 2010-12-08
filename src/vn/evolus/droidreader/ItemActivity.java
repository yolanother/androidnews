package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.List;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.content.criteria.LatestItems;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.widget.ActionItem;
import vn.evolus.droidreader.widget.ItemView;
import vn.evolus.droidreader.widget.QuickAction;
import vn.evolus.droidreader.widget.ScrollView;
import vn.evolus.droidreader.widget.ScrollView.OnScreenSelectedListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ItemActivity extends LocalizedActivity implements OnScreenSelectedListener {	
	public static final String ITEM_ID_PARAM = "ItemId";
	public static final String TAG_ID_PARAM = "TagId";
	public static final String CHANNEL_ID_PARAM = "ChannelId";
	
	private TextView title;	
	private ScrollView scrollView;
	
	private ArrayList<Item> items;
	private Item currentItem;
	private int totalItems = 0;
	private int currentItemIndex = 0;
	private boolean loadReadItems = true;
	private int itemId = 0;
	private int channelId = LatestItems.ALL_CHANNELS;
	private int tagId = LatestItems.ALL_TAGS;
	private String article;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.item_view);		
		ImageLoader.initialize(this);
		article = getString(R.string.article);
		
		title = (TextView)findViewById(R.id.title);
		
		ImageButton shareButton = (ImageButton)findViewById(R.id.share);
		shareButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				share();
			}
		});
		
		ImageButton viewOriginalButton = (ImageButton)findViewById(R.id.viewOriginal);
		viewOriginalButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				viewOriginal();
			}
		});
		
		ImageButton toolsButton = (ImageButton)findViewById(R.id.tools);
		toolsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showItemTools(v);
			}
		});
				
		ImageButton nightModeButton = (ImageButton)findViewById(R.id.nightMode);
		nightModeButton.setSelected(Settings.getNightReadingMode());
		nightModeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());
				Settings.saveNightReadingMode(v.isSelected());
				changeReadingMode(v.isSelected());
			}
		});
						
		scrollView = (ScrollView)findViewById(R.id.scrollView);	
		scrollView.setOnItemSelectedListener(this);
							
		if (savedInstanceState != null) {
			itemId = savedInstanceState.getInt(ITEM_ID_PARAM);
		} else {
			itemId = 0;
		}
		if (savedInstanceState != null) {
			tagId = savedInstanceState.getInt(TAG_ID_PARAM);
		} else {
			tagId = LatestItems.ALL_TAGS;
		}
		if (savedInstanceState != null) {
			channelId = savedInstanceState.getInt(CHANNEL_ID_PARAM);
		} else {
			channelId = LatestItems.ALL_CHANNELS;
		}
	}		
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(ITEM_ID_PARAM, currentItem.id);
		outState.putInt(TAG_ID_PARAM, tagId);
		outState.putInt(CHANNEL_ID_PARAM, channelId);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {		
		super.onNewIntent(intent);
		setIntent(intent);			
	}
	
	@Override
	protected void onStart() {
		super.onStart();
				
		itemId = getIntent().getIntExtra(ITEM_ID_PARAM, itemId);
		tagId = getIntent().getIntExtra(TAG_ID_PARAM, tagId);
		channelId = getIntent().getIntExtra(CHANNEL_ID_PARAM, channelId);
		
		currentItem = ContentManager.loadItem(itemId,
				ContentManager.FULL_ITEM_LOADER,
				ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);				
		
		if (channelId != ContentManager.ALL_CHANNELS) {
			loadReadItems = true;			
		} else {
			loadReadItems = Settings.getShowRead();
		}
		items = new ArrayList<Item>();
		List<Item> newerItems = ContentManager.loadItems(
				new LatestItems(
						channelId,
						tagId, 
						!loadReadItems, 
						currentItem,
						LatestItems.NEWER,
						1), 
				ContentManager.FULL_ITEM_LOADER,
				ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);		
		items.addAll(newerItems);
		
		items.add(currentItem);
		
		List<Item> olderItems = ContentManager.loadItems(
				new LatestItems(
						channelId,
						tagId, 
						!loadReadItems, 
						currentItem,
						LatestItems.OLDER,
						1), 
				ContentManager.FULL_ITEM_LOADER,
				ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
		items.addAll(olderItems);
		
		loadItems();
	}
		
	@Override
	protected void onPause() {
		ContentManager.markAllTemporarilyMarkedReadItemsAsRead();
		super.onPause();
	}
	
	protected void showItemTools(View anchor) {
		final QuickAction quickAction = new QuickAction(anchor);
    	
    	ActionItem star = new ActionItem();
    	if (currentItem.starred) {
    		star.setTitle(getResources().getString(R.string.unstar));    	
    		star.setIcon(getResources().getDrawable(R.drawable.ic_star));
    	} else {
	    	star.setTitle(getResources().getString(R.string.star));
	    	star.setIcon(getResources().getDrawable(R.drawable.ic_star_empty));	    	
    	}
    	star.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleStarred();
				quickAction.dismiss();
			}			
		});						
		
		ActionItem keepUnread = new ActionItem();
		if (currentItem.isKeptUnread()) {
			keepUnread.setTitle(getResources().getString(R.string.mark_as_read));
			keepUnread.setIcon(getResources().getDrawable(R.drawable.ic_checkbox));
		} else {
			keepUnread.setTitle(getResources().getString(R.string.keep_unread));
			keepUnread.setIcon(getResources().getDrawable(R.drawable.ic_checkbox_empty));			
		}
		keepUnread.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleKeptUnread();
				quickAction.dismiss();
			}
		});
		
		ActionItem mobilize = new ActionItem();		
		mobilize.setTitle(getResources().getString(R.string.mobilize));
		mobilize.setIcon(getResources().getDrawable(R.drawable.ic_mobilize));		
		mobilize.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mobilizeItem();
				quickAction.dismiss();
			}
		});
		
		quickAction.addActionItem(star);		
		quickAction.addActionItem(keepUnread);
		quickAction.addActionItem(mobilize);
				
		quickAction.setAnimStyle(QuickAction.ANIM_AUTO);		
		quickAction.show();
	}
	
	private void loadItems() {
		if (scrollView.getChildCount() > 0) {
			scrollView.removeAllViews();
		}
		
		int i = 0, currentItemIndex = 0;
		for (Item item : items) {
			ItemView itemView = new ItemView(this);
			itemView.setNightMode(Settings.getNightReadingMode());
			if (currentItem != null && currentItem.equals(item)) {
				currentItemIndex = i;
				itemView.setItem(currentItem);
			}
			scrollView.addView(itemView);
			i++;
		}
		scrollView.showScreen(currentItemIndex);
	}
	
	private void share() {
		if (currentItem == null) return;
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, currentItem.title);		
		String shareText = getResources().getString(R.string.share_text);
		shareText = shareText.replace("{link}", currentItem.link);		
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
		startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share)));      
	}
	
	private void viewOriginal() {
		Intent viewIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(currentItem.link));
		startActivity(viewIntent);		
	}
	
	private void mobilizeItem() {
		int viewIndex = scrollView.getDisplayedChild();
		if (viewIndex >= 0) {
			ItemView itemView = (ItemView)scrollView.getChildAt(viewIndex);
			if (itemView != null) {
				itemView.mobilize();
			}
		}
	}
	
	protected void changeReadingMode(boolean nightMode) {
		int count = scrollView.getChildCount();
		for (int i = 0; i < count; i++) {
			ItemView itemView = (ItemView)scrollView.getChildAt(i);
			itemView.setNightMode(nightMode);
		}
	}
	
	protected void toggleStarred() {
		if (currentItem != null) {
			if (currentItem.starred) {
				ContentManager.unmarkItemAsStarred(currentItem);
				Toast.makeText(this, R.string.item_unstarred, Toast.LENGTH_SHORT).show();
			} else {
				ContentManager.markItemAsStarred(currentItem);
				Toast.makeText(this, R.string.item_starred, Toast.LENGTH_SHORT).show();
			}
		}
	}	
	
	protected void toggleKeptUnread() {
		if (currentItem.isKeptUnread()) {
			ContentManager.unmarkItemAsKeptUnread(currentItem);
			Toast.makeText(this, R.string.item_marked_as_read, Toast.LENGTH_SHORT).show();
		} else {
			ContentManager.markItemAsKeptUnread(currentItem);
			Toast.makeText(this, R.string.item_kept_unread, Toast.LENGTH_SHORT).show();
		}
	}

	private void markItemAsRead(Item item) {
		if (!item.isRead() && !item.isKeptUnread()) {
			ContentManager.saveItemReadState(item, Item.TEMPORARILY_MARKED_AS_READ);
		}
	}
	
	@Override
	public void onSelected(int selectedIndex) {
		currentItem = showItem(selectedIndex);
					
		markItemAsRead(currentItem);
		
		if (selectedIndex > 0) {
			showItem(selectedIndex - 1);
		}		
		if (selectedIndex < items.size() - 1) {
			showItem(selectedIndex + 1);
		}
		loadOlderItem(selectedIndex);
		selectedIndex = loadNewerItem(selectedIndex, currentItem);
				
		totalItems = ContentManager.countItems(
				new LatestItems(
						channelId,
						tagId,
						!loadReadItems, 
						null,
						LatestItems.NONE));
		currentItemIndex = ContentManager.countItems(
				new LatestItems(
						channelId,
						tagId,
						!loadReadItems,
						currentItem,
						LatestItems.NEWER));		
		title.setText(article.replace("{no}", 
				String.valueOf(currentItemIndex + 1)
					.concat("/")
					.concat(String.valueOf(totalItems))));
	}

	private void loadOlderItem(int selectedIndex) {
		if (selectedIndex == (items.size() - 1)) {			
			List<Item> olderItems = ContentManager.loadItems(
					new LatestItems(
							channelId,
							tagId, 
							!loadReadItems, 
							currentItem,
							LatestItems.OLDER,
							1), 
					ContentManager.FULL_ITEM_LOADER,
					ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (olderItems.size() > 0) {				
				Item olderItem = olderItems.get(0);
				items.add(items.size(), olderItem);
				
				ItemView itemView = new ItemView(this);
				itemView.setNightMode(Settings.getNightReadingMode());
				scrollView.addView(itemView);
				itemView.setItem(olderItem);
			}
		}
	}

	private int loadNewerItem(int selectedIndex, Item item) {
		if (selectedIndex == 0) {
			List<Item> newerItems = ContentManager.loadItems(
					new LatestItems(
							channelId,
							tagId, 
							!loadReadItems, 
							item,
							LatestItems.NEWER,
							1), 
					ContentManager.FULL_ITEM_LOADER,
					ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (newerItems.size() > 0) {
				Item newerItem = newerItems.get(0);
				items.add(0, newerItem);
				selectedIndex++;
				
				ItemView itemView = new ItemView(this);
				itemView.setNightMode(Settings.getNightReadingMode());
				scrollView.prependView(itemView);
				try {
					itemView.setItem(newerItem);
				} catch (Throwable t) {
					t.printStackTrace();
				}								
			}
		}
		return selectedIndex;
	}
	
	private Item showItem(int itemIndex) {
		ItemView itemView = (ItemView)scrollView.getChildAt(itemIndex);
		Item item = items.get(itemIndex);
		if (item == null) {
			Log.e("ERROR", "Oops! Item at index " + itemIndex + " is null");
		}
		if (!itemView.hasItem()) {			
			item = ContentManager.loadItem(item.id,
					ContentManager.FULL_ITEM_LOADER,
					ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			itemView.setItem(item);
		} else {
			item = itemView.getItem();
		}
		if (!item.equals(currentItem)) {
			itemView.scrollTo(0, 0);
		}		
		return item;
	}		
}
