package vn.evolus.droidreader;

import java.util.ArrayList;
import java.util.List;

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
	private TextView subTitle;
	private ScrollView scrollView;
	private ArrayList<Item> items;
	private long channelId = ContentManager.ALL_CHANNELS;
	private Item currentItem;
	private int totalItems = 0;
	private int currentItemIndex = 0;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.item_view);
		
		ImageLoader.initialize(this);		
		
		title = (TextView)findViewById(R.id.title);
		subTitle = (TextView)findViewById(R.id.subTitle);
		
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
						
		scrollView = (ScrollView)findViewById(R.id.scrollView);	
		scrollView.setOnItemSelectedListener(this);		
					
		long itemId = 0;
		if (savedInstanceState != null) {
			itemId = savedInstanceState.getLong("ItemId");
		} else {
			itemId = getIntent().getLongExtra("ItemId", 0);
		}
		currentItem = ContentManager.loadItem(itemId,
				ContentManager.FULL_ITEM_LOADER,
				ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
		
		channelId = getIntent().getLongExtra("ChannelId", ContentManager.ALL_CHANNELS);			
		items = new ArrayList<Item>();
		List<Item> newerItems = ContentManager.loadNewerItems(currentItem,
				channelId,
				1, // load only 1 items
				ContentManager.ID_ONLY_ITEM_LOADER, null);
		items.addAll(newerItems);
		items.add(currentItem);
		
		List<Item> olderItems = ContentManager.loadOlderItems(currentItem,
				channelId,
				1, // load only 1 items
				ContentManager.ID_ONLY_ITEM_LOADER, null);
		items.addAll(olderItems);
						
		loadItems();
	}	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putLong("ItemId", currentItem.id);
		super.onSaveInstanceState(outState);
	}
	
	private void loadItems() {
		int i = 0, currentItemIndex = 0;
		for (Item item : items) {
			ItemView itemView = new ItemView(this);
			if (item.equals(currentItem)) {
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

	@Override
	public void onSelected(int selectedIndex) {		
		currentItem = showItem(selectedIndex);
				
		if (!currentItem.read) {			
			currentItem.read = true;
			ContentManager.saveItem(currentItem);
		}
		if (selectedIndex > 0) {
			showItem(selectedIndex - 1);
		}		
		if (selectedIndex < items.size() - 1) {
			showItem(selectedIndex + 1);
		}
		loadOlderItem(selectedIndex);
		selectedIndex = loadNewerItem(selectedIndex, currentItem);		
		
		title.setText(currentItem.channel.title);
		totalItems = ContentManager.countItems(channelId);
		currentItemIndex = ContentManager.countNewerItems(currentItem, channelId);		
		subTitle.setText(String.valueOf(currentItemIndex + 1)
				.concat("/")
				.concat(String.valueOf(totalItems)));
//		Log.d("DEBUG", "countItems in " + (System.currentTimeMillis() - start));
	}

	private void loadOlderItem(int selectedIndex) {
		if (selectedIndex == (items.size() - 1)) {			
			List<Item> olderItems = ContentManager.loadOlderItems(currentItem,
						channelId,
						1, 
						ContentManager.FULL_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (olderItems.size() > 0) {				
				Item olderItem = olderItems.get(0);
				items.add(items.size(), olderItem);
				
				ItemView itemView = new ItemView(this);								
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
						ContentManager.FULL_ITEM_LOADER, 
						ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			if (newerItems.size() > 0) {
				Item newerItem = newerItems.get(0);
				items.add(0, newerItem);
				selectedIndex++;
				
				ItemView itemView = new ItemView(this);
				scrollView.prependView(itemView);
				itemView.setItem(newerItem);
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
