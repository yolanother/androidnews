package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.util.ImageLoader;
import vn.evolus.droidreader.widget.ItemView;
import vn.evolus.droidreader.widget.ScrollView;
import vn.evolus.droidreader.widget.ScrollView.OnScreenSelectedListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class ItemActivity extends LocalizedActivity implements OnScreenSelectedListener {
	private TextView title;
	private ImageButton starButton;
	private ScrollView scrollView;
	private ArrayList<Item> items;
	private int channelId = ContentManager.ALL_CHANNELS;
	private Item currentItem;
	private int totalItems = 0;
	private int currentItemIndex = 0;	
	private boolean loadReadItems = true;
	private boolean loadStarredOnly = false;
	private Set<Item> readItems = new HashSet<Item>();	
	private String article;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.item_view);		
		ImageLoader.initialize(this);
		article = getString(R.string.article);
		
		title = (TextView)findViewById(R.id.title);
		
		starButton = (ImageButton)findViewById(R.id.star);
		starButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());								
				changeCurrentItemAsStarredState(v.isSelected());				
			}			
		});
		
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
		
		ImageButton nightModeButton = (ImageButton)findViewById(R.id.nightMode);
		nightModeButton.setSelected(Settings.getNightReadingMode(this));
		nightModeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setSelected(!v.isSelected());
				Settings.saveNightReadingMode(ItemActivity.this, v.isSelected());
				changeReadingMode(v.isSelected());
			}
		});
						
		scrollView = (ScrollView)findViewById(R.id.scrollView);	
		scrollView.setOnItemSelectedListener(this);
					
		int itemId = 0;
		if (savedInstanceState != null) {
			itemId = savedInstanceState.getInt("ItemId");
		} else {
			itemId = getIntent().getIntExtra("ItemId", 0);
		}
		currentItem = ContentManager.loadItem(itemId,
				ContentManager.FULL_ITEM_LOADER,
				ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
		
		loadStarredOnly = getIntent().getBooleanExtra("StarredOnly", false);
		channelId = getIntent().getIntExtra("ChannelId", ContentManager.ALL_CHANNELS);
		if (channelId != ContentManager.ALL_CHANNELS) {
			loadReadItems = true;			
		} else {
			loadReadItems = Settings.getShowRead(this);
		}
		items = new ArrayList<Item>();
		List<Item> newerItems = ContentManager.loadNewerItems(currentItem,
				channelId,
				1, // load only 1 items
				loadReadItems,
				loadStarredOnly,
				ContentManager.ID_ONLY_ITEM_LOADER, null);
		items.addAll(newerItems);
		items.add(currentItem);
		
		List<Item> olderItems = ContentManager.loadOlderItems(currentItem,
				channelId,
				1, // load only 1 items
				loadReadItems,
				loadStarredOnly, 
				ContentManager.ID_ONLY_ITEM_LOADER, null);
		items.addAll(olderItems);
						
		loadItems();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("ItemId", currentItem.id);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {		
		if (readItems.size() > 0) {
			for (Item item : readItems) {
				ContentManager.markItemAsRead(item);
			}
			readItems.clear();
		}
		super.onPause();
	}
	
	private void loadItems() {
		int i = 0, currentItemIndex = 0;
		for (Item item : items) {
			ItemView itemView = new ItemView(this);
			itemView.setNightMode(Settings.getNightReadingMode(this));
			if (item.equals(currentItem)) {
				currentItemIndex = i;
				itemView.setItem(currentItem);
				readItems.add(currentItem);
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
	
	protected void changeReadingMode(boolean nightMode) {
		int count = scrollView.getChildCount();
		for (int i = 0; i < count; i++) {
			ItemView itemView = (ItemView)scrollView.getChildAt(i);
			itemView.setNightMode(nightMode);
		}
	}
	
	protected void changeCurrentItemAsStarredState(boolean starred) {
		if (currentItem != null) {
			if (starred) {
				ContentManager.markItemAsStarred(currentItem);
			} else {
				ContentManager.unmarkItemAsStarred(currentItem);
			}
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
				
		totalItems = ContentManager.countItems(channelId, loadReadItems, loadStarredOnly);
		currentItemIndex = ContentManager.countNewerItems(currentItem, channelId, 
				loadReadItems, loadStarredOnly);		
		title.setText(article.replace("{no}", 
				String.valueOf(currentItemIndex + 1)
					.concat("/")
					.concat(String.valueOf(totalItems))));		
		starButton.setSelected(currentItem.starred);
	}

	private void markItemAsRead(Item item) {
		if (!item.read && !readItems.contains(item)) {
			readItems.add(item);
		}
	}

	private void loadOlderItem(int selectedIndex) {
		if (selectedIndex == (items.size() - 1)) {			
			List<Item> olderItems = ContentManager.loadOlderItems(currentItem,
						channelId,
						1, 
						loadReadItems,
						loadStarredOnly,
						ContentManager.FULL_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (olderItems.size() > 0) {				
				Item olderItem = olderItems.get(0);
				items.add(items.size(), olderItem);
				
				ItemView itemView = new ItemView(this);
				itemView.setNightMode(Settings.getNightReadingMode(this));
				scrollView.addView(itemView);
				itemView.setItem(olderItem);
			}
		}
	}

	private int loadNewerItem(int selectedIndex, Item item) {
		if (selectedIndex == 0) {
			List<Item> newerItems = ContentManager.loadNewerItems(item,
						channelId,
						1,
						loadReadItems,
						loadStarredOnly,
						ContentManager.FULL_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (newerItems.size() > 0) {
				Item newerItem = newerItems.get(0);
				items.add(0, newerItem);
				selectedIndex++;
				
				ItemView itemView = new ItemView(this);
				itemView.setNightMode(Settings.getNightReadingMode(this));
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
