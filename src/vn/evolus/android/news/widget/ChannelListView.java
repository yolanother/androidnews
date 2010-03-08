package vn.evolus.android.news.widget;

import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.adapter.ChannelListViewAdapter;
import vn.evolus.android.news.rss.Channel;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class ChannelListView extends ListView {				
	
	public ChannelListView(Context context) {
		super(context);		
		
		this.setBackgroundColor(getResources().getColor(R.color.newsItemBackground));
		this.setCacheColorHint(getResources().getColor(R.color.newsItemBackground));
		this.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
	}
	
	public ChannelListView(Context context, AttributeSet attrs) {
		super(context, attrs);			
	}
	
	public ChannelListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);			
	}
	
	public void setChannels(List<Channel> channels) {		                  
        this.setAdapter(new ChannelListViewAdapter(getContext(), channels));
	}		
}
