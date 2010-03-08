package vn.evolus.android.news.adapter;

import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Channel;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChannelListViewAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView image;
		TextView title;
	}
	
	private List<Channel> channels;
	private Context context;

	public ChannelListViewAdapter(Context context, List<Channel> channels) {
		this.context = context;
		this.channels = channels;
	}

	public int getCount() {
		return channels.size();
	}

	public Object getItem(int position) {		
		return channels.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel, null);
			
			holder = new ViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.title);						
			holder.image = (ImageView)convertView.findViewById(R.id.image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Channel item = channels.get(position);		
		holder.title.setText(item.getTitle());		
		
		return convertView;
	}		
}
