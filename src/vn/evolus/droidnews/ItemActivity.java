package vn.evolus.droidnews;

import java.util.ArrayList;

import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.Item;
import vn.evolus.droidnews.util.ImageLoader;
import vn.evolus.droidnews.widget.ItemView;
import vn.evolus.droidnews.widget.ScrollView;
import vn.evolus.droidnews.widget.ScrollView.OnItemSelectedListener;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class ItemActivity extends Activity implements OnItemSelectedListener {
	private TextView title;
	private TextView subTitle;
	private ScrollView scrollView;
	private ArrayList<Item> items;
	private Item currentItem;		
		
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
					
		long currentItemId = 0;
		if (savedInstanceState != null) {
			currentItemId = savedInstanceState.getLong("ItemId");
		} else {
			currentItemId = getIntent().getLongExtra("ItemId", 0);
		}
		
		long channelId = getIntent().getLongExtra("ChannelId", 0);
		if (channelId != 0) {
			Channel channel = ContentManager.loadChannel(channelId, 
					ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			title.setText(channel.title);
			
			channel.loadFullItems();
			items = channel.getItems();
		} else {			
			items = ContentManager.loadLatestItems(30, 
					ContentManager.FULL_ITEM_LOADER, ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			title.setText(getString(R.string.latest));
		}

		//*
		int index = items.indexOf(new Item(currentItemId));		
		if (index >= 0) {
			currentItem  = items.get(index);
		}
		//*/					
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
			ItemView itemView = new ItemView(this.getApplicationContext());
			if (item.equals(currentItem)) {
				currentItemIndex = i;
				itemView.setItem(item);
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
		title.setText(currentItem.channel.title);
		subTitle.setText(String.valueOf(selectedIndex + 1)
				.concat("/")
				.concat(String.valueOf(items.size())));
		if (!currentItem.read) {			
			currentItem.read = true;
			ContentManager.saveItem(currentItem);
		}
		
		if (selectedIndex > 0) {
			showItem(selectedIndex - 1);
		}
		if (selectedIndex < scrollView.getChildCount() - 1) {
			showItem(selectedIndex + 1);
		}		
	}
	
	private Item showItem(int itemIndex) {
		ItemView itemView = (ItemView)scrollView.getChildAt(itemIndex);		
		Item item = items.get(itemIndex);
		itemView.setItem(item);
		if (!item.equals(currentItem)) {
			itemView.scrollTo(0, 0);
		}		
		return item;
	}
}
