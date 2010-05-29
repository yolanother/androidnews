package vn.evolus.news.widget;

import vn.evolus.android.news.R;
import vn.evolus.news.adapter.ItemListViewAdapter;
import vn.evolus.news.rss.Item;
import vn.evolus.news.util.ActiveList;
import android.content.Context;
import android.widget.ListView;

public class ItemListView extends ListView {
	private ItemListViewAdapter adapter;
	public ItemListView(Context context) {
		super(context);		
		adapter = new ItemListViewAdapter(context);
		this.setAdapter(adapter);
		
		this.setBackgroundColor(getResources().getColor(R.color.itemBackground));
		this.setCacheColorHint(getResources().getColor(R.color.itemBackground));
		this.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
	}
	
	public void setItems(ActiveList<Item> items) {
		this.setSelection(-1);
		adapter.setItems(items);
	}

	public void refresh() {
		adapter.notifyDataSetChanged();
	}	
}
