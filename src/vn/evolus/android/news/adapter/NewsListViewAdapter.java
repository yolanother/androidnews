package vn.evolus.android.news.adapter;

import java.util.List;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NewsListViewAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView itemImage;
		TextView title;
		TextView date;
	}
	
	private List<Item> items;
	private Context context;

	public NewsListViewAdapter(Context context, List<Item> items) {
		this.context = context;
		this.items = items;
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
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.news_item, null);
			
			holder = new ViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.newsTitle);			
			holder.date = (TextView)convertView.findViewById(R.id.newsDate);						
			holder.itemImage = (ImageView)convertView.findViewById(R.id.newsImage);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Item item = items.get(position);		
		holder.title.setText(item.getTitle());
		if (item.getPubDate() != null) {
			holder.date.setText(DateUtils.getRelativeDateTimeString(context, item.getPubDate().getTime(), 1, DateUtils.DAY_IN_MILLIS, 0));
		}
		
		return convertView;
	}		
}
