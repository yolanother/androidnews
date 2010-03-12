package vn.evolus.android.news.widget;

import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.adapter.ItemListViewAdapter;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.widget.ListView;

public class ItemListView extends ListView {
	
	public ItemListView(Context context) {
		super(context);
				
		this.setBackgroundColor(getResources().getColor(R.color.newsItemBackground));
		this.setCacheColorHint(getResources().getColor(R.color.newsItemBackground));
		this.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
	}		
	
	public void setItems(List<Item> items) {		                  
        this.setAdapter(new ItemListViewAdapter(getContext(), items));
	}		
}
