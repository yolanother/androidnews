package vn.evolus.news.widget;

import vn.evolus.android.news.R;
import vn.evolus.news.adapter.ChannelListViewAdapter;
import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ActiveList;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class ChannelListView extends ListView {				
	ChannelListViewAdapter adapter = null;
	public ChannelListView(Context context) {
		super(context);		
		
		this.setBackgroundColor(getResources().getColor(R.color.itemBackground));
		this.setCacheColorHint(getResources().getColor(R.color.itemBackground));
		this.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
	}
	
	public ChannelListView(Context context, AttributeSet attrs) {
		super(context, attrs);			
	}
	
	public ChannelListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);			
	}
	
	public void setChannels(ActiveList<Channel> channels) {		   
		adapter = new ChannelListViewAdapter(getContext(), channels);
        this.setAdapter(adapter);
	}	
	
	public void refesh() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
}
