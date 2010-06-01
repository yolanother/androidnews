package vn.evolus.news.adapter;

import vn.evolus.news.R;
import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ActiveList;
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
		TextView unreadCount;
	}
	
	private ActiveList<Channel> channels;
	private Context context;

	public ChannelListViewAdapter(Context context, ActiveList<Channel> channels) {
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
			holder.unreadCount = (TextView)convertView.findViewById(R.id.unreadCount);
			holder.image = (ImageView)convertView.findViewById(R.id.image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Channel item = channels.get(position);		
		holder.title.setText(item.getTitle());
		int unreadItems = item.countUnreadItems();
		if (unreadItems > 0) {
			holder.unreadCount.setText(String.valueOf(unreadItems));
		} else {
			holder.unreadCount.setText("");
		}
		
		return convertView;
	}		
}
