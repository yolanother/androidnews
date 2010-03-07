package vn.evolus.android.news.widget;

import vn.evolus.android.news.R;
import vn.evolus.android.news.adapter.NewsListViewAdapter;
import vn.evolus.android.news.rss.Channel;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChannelView extends LinearLayout {

	public ChannelView(Context context) {
		super(context);
		
		LayoutInflater.from(context).inflate(R.layout.channel_view, this, true);
	}			
	
	public void setChannel(Channel channel) {
		TextView channelName = (TextView)findViewById(R.id.channelName);
        channelName.setText(channel.getTitle());        
        ListView channelItems = (ListView)findViewById(R.id.newsList);
        channelItems.setAdapter(new NewsListViewAdapter(getContext(), channel.getItems()));
	}
}
