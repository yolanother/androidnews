package vn.evolus.news;

import java.util.ArrayList;

import vn.evolus.news.model.Channel;
import vn.evolus.news.model.Item;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.widget.ItemView;
import vn.evolus.news.widget.ScrollView;
import vn.evolus.news.widget.ScrollView.OnItemSelectedListener;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ItemActivity extends Activity implements OnItemSelectedListener {	
	private ScrollView scrollView;
	private ArrayList<Item> items;
	private Item currentItem;	
	private ContentResolver cr;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		cr = getContentResolver();
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_view);				
		scrollView = (ScrollView)findViewById(R.id.scrollView);	
		scrollView.setOnItemSelectedListener(this);		
					
		long currentItemId = 0;
		if (savedInstanceState != null) {
			currentItemId = savedInstanceState.getLong("ItemId");
		} else {
			currentItemId = getIntent().getLongExtra("ItemId", 0);
		}
		
		Channel channel = Channel.load(getIntent().getLongExtra("ChannelId", 0), cr);		
		channel.loadFullItems(cr);
		items = channel.getItems();
		
//		currentItem = Item.load(cr, currentItemId);
//		items = new ActiveList<Item>();
//		items.add(currentItem);

		int index = items.indexOf(new Item(currentItemId));		
		if (index >= 0) {
			currentItem  = items.get(index);
		}
		//*/
		setTitle(channel.getTitle());				
		loadItems();		
	}	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putLong("ItemId", currentItem.getId());
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.share).setIcon(android.R.drawable.ic_menu_share);
		menu.add(R.string.view_original).setIcon(android.R.drawable.ic_menu_view);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		share();
		return true;
	}
	
	public void share() {
		if (currentItem == null) return;
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, currentItem.getTitle());		
		String shareText = getResources().getString(R.string.share_text);
		shareText = shareText.replace("{link}", currentItem.getLink());		
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
		startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share)));      
	}

	@Override
	public void onSelected(int selectedIndex) {
		currentItem = showItem(selectedIndex);
		if (!currentItem.getRead()) {			
			currentItem.setRead(true);
			currentItem.save(cr);
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
