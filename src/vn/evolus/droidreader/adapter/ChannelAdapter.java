package vn.evolus.droidreader.adapter;

import java.util.ArrayList;

import vn.evolus.droidreader.R;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.util.ImageLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ChannelAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView image;
		TextView title;
		TextView unreadCount;
	}
	
	private ArrayList<Channel> channels;
	private Context context;

	public ChannelAdapter(Context context, ArrayList<Channel> channels) {
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
		holder.title.setText(item.title);
		int unreadItems = item.countUnreadItems();
		if (unreadItems > 0) {
			holder.unreadCount.setText(String.valueOf(unreadItems));
			holder.unreadCount.setVisibility(View.VISIBLE);
		} else {
			holder.unreadCount.setVisibility(View.GONE);
		}
		
		holder.image.setImageBitmap(null);
		holder.image.setVisibility(View.GONE);
		String imageUrl = item.imageUrl;
		holder.image.setTag(imageUrl);
		if (imageUrl != null) {
			try {
				Bitmap itemImage = ImageLoader.get(imageUrl);			
				if (itemImage != null) {
					holder.image.setVisibility(View.VISIBLE);
					holder.image.setImageBitmap(itemImage);
				} else {					
					ImageLoader.start(imageUrl, new ItemImageLoaderHandler(holder.image, imageUrl));
				}
			} catch (RuntimeException e) {
			}
		}
		
		return convertView;
	}

	public void setChannels(ArrayList<Channel> channels) {
		this.channels = channels;
		notifyDataSetChanged();
	}

	public void refresh() {
		notifyDataSetChanged();
	}		
}
