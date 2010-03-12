package vn.evolus.android.news.adapter;

import java.net.URLEncoder;
import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.droidfu.widgets.WebImageView;

public class ItemListViewAdapter extends BaseAdapter {
	static class ViewHolder {
		WebImageView itemImage;
		TextView title;
		TextView date;
	}
	
	private View[] rowViews;
	private List<Item> items;
	private Context context;

	public ItemListViewAdapter(Context context, List<Item> items) {
		this.context = context;
		this.items = items;
		rowViews = new View[items.size()];
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

	public View getView(int position, View convertView, ViewGroup parent) {		
				
		if (rowViews[position] == null) {
			View rowView = View.inflate(context, R.layout.item, null);
			Item item = items.get(position);
			
			TextView title = (TextView)rowView.findViewById(R.id.itemTitle);
			title.setText(item.getTitle());
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
			} else {
			}
			rowViews[position] = rowView;
		}
		return rowViews[position];
		/*
		ViewHolder holder;
		if (convertView == null) {				
			convertView = View.inflate(context, R.layout.item, null);
			
			Log.d("DEBUG", "Create new convertView for position " + position);
			
			holder = new ViewHolder();
			holder.title = (TextView)rowView.findViewById(R.id.itemTitle);			
			holder.date = (TextView)rowView.findViewById(R.id.itemDate);						
			holder.itemImage = (WebImageView)rowView.findViewById(R.id.itemImage);
			holder.itemImage.setInAnimation(null);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Item item = items.get(position);		
		holder.title.setText(item.getTitle());
		if (item.getPubDate() != null) {
			holder.date.setText(DateUtils.getRelativeDateTimeString(context, item.getPubDate().getTime(), 1, DateUtils.DAY_IN_MILLIS, 0));
		}
		if (item.getImageUrl() != null) {
			String scaledImageUrl = "http://feeds.demo.evolus.vn/resizer/?width=60&height=60&url=" + 
				URLEncoder.encode(item.getImageUrl());
			holder.itemImage.setImageUrl(scaledImageUrl);
			holder.itemImage.loadImage();
		}		
		return convertView;
		*/
	}		
}
