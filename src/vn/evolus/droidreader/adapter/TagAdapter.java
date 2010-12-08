package vn.evolus.droidreader.adapter;

import java.util.ArrayList;

import vn.evolus.droidreader.R;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TagAdapter extends BaseAdapter {
	public static class TagItem {
		public int id;
		public String icon;
		public String title;
		public int unreadCount;		
	}
	
	static class ViewHolder {
		ImageView icon;
		TextView title;
		TextView unreadCount;
	}
	
	private ArrayList<TagItem> items;
	private Context context;

	public TagAdapter(Context context, ArrayList<TagItem> items) {
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
			convertView = View.inflate(context, R.layout.tag, null);
			
			holder = new ViewHolder();
			holder.icon = (ImageView)convertView.findViewById(R.id.icon);
			holder.title = (TextView)convertView.findViewById(R.id.title);
			holder.unreadCount = (TextView)convertView.findViewById(R.id.unreadCount);			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		TagItem item = items.get(position);
		if (item.icon != null) {
			holder.icon.setImageResource(Integer.parseInt(item.icon));
		}
		holder.title.setText(item.title);
		int unreadItems = item.unreadCount;
		
		if (unreadItems > 0) {
			holder.unreadCount.setText(String.valueOf(unreadItems));
			holder.unreadCount.setVisibility(View.VISIBLE);
		} else {
			holder.unreadCount.setVisibility(View.GONE);
		}
		
		return convertView;
	}

	public void setItems(ArrayList<TagItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}		
}
