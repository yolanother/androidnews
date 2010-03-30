package vn.evolus.android.news.adapter;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.adapter.ChannelListViewAdapter.ViewHolder;
import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.rss.Item;
import vn.evolus.android.news.util.ActiveList;
import android.content.Context;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.droidfu.widgets.WebImageView;

public class ItemListViewAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView image;
		TextView title;
		TextView date;
	}
	
	private int itemTitleReadColor = 0;
	private List<View> rowViews = new ArrayList<View>();
	private ActiveList<Item> items = new ActiveList<Item>();
	private Context context;
	Handler handler = new Handler();
	
	private  ActiveList.ActiveListListener<Item> activeListListener = 
		new ActiveList.ActiveListListener<Item>() {
		public void onAdd(Item item) {				
			handler.post(new Runnable() {
				public void run() {
					ItemListViewAdapter.this.notifyDataSetChanged();
				}
			});
		}
		public void onInsert(final int location, final Item item) {			
			handler.post(new Runnable() {
				public void run() {
					ItemListViewAdapter.this.addView(location, item);					
					ItemListViewAdapter.this.notifyDataSetChanged();
				}					
			});
		}
		public void onClear() {
			ItemListViewAdapter.this.rowViews.clear();				
		}
	};

	public ItemListViewAdapter(Context context) {
		this.context = context;
		itemTitleReadColor = context.getResources().getColor(R.color.itemTitleRead);
	}
	public int getCount() {
		return items.size();
	}
	public Object getItem(int position) {		
		return items.get(position);
	}
	public long getItemId(int position) {
		return position;
	}
	public void setItems(ActiveList<Item> items) {
		this.rowViews.clear();
		if (this.items != null) {
			this.items.removeListener(activeListListener);
		}
		this.items = items;		
		this.items.addListener(activeListListener);	
		this.notifyDataSetInvalidated();
	}		
	
	private View addView(int position, Item item) {
		View rowView = View.inflate(context, R.layout.item, null);				
		TextView title = (TextView)rowView.findViewById(R.id.itemTitle);
		title.setText(item.getTitle());
		if (item.getRead()) {
			title.setTextColor(itemTitleReadColor);
		}
		TextView date = (TextView)rowView.findViewById(R.id.itemDate);
		if (item.getPubDate() != null) {
			date.setText(DateUtils.getRelativeDateTimeString(context, item.getPubDate().getTime(), 1, DateUtils.DAY_IN_MILLIS, 0));
		}
		WebImageView itemImage = (WebImageView)rowView.findViewById(R.id.itemImage);
		itemImage.setInAnimation(null);
		itemImage.setNoImageDrawable(R.drawable.no_image);
		if (item.getImageUrl() != null) {
			String scaledImageUrl = "http://feeds.demo.evolus.vn/resizer/?width=60&height=60&url=" + 
				URLEncoder.encode(item.getImageUrl());
			//Log.d("DEBUG", scaledImageUrl);
			itemImage.setImageUrl(scaledImageUrl);
			itemImage.loadImage();
		}
		rowViews.add(position, rowView);
		
		return rowView;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		/*
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel, null);
			
			holder = new ViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.itemTitle);
			holder.date = (TextView)convertView.findViewById(R.id.itemDate);
			holder.image = (ImageView)convertView.findViewById(R.id.itemImage);
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
		*/
		
		View rowView = null;
		Item item = this.items.get(position);
		if (position >= rowViews.size()) {
			rowView = addView(position, item);			
		} else {
			rowView = rowViews.get(position);
			TextView title = (TextView)rowView.findViewById(R.id.itemTitle);			
			if (item.getRead()) {
				title.setTextColor(itemTitleReadColor);
			}
		}
		return rowView;
	}		
}
