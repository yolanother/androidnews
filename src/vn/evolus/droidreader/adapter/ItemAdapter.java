package vn.evolus.droidreader.adapter;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import vn.evolus.droidreader.R;
import vn.evolus.droidreader.content.ContentManager;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.util.ActiveList;
import vn.evolus.droidreader.util.ImageLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemAdapter extends BaseAdapter {
	private static DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy',' HH:mm a");
	
	private static final int REFRESH_MESSAGE = 1;	
	static class ViewHolder {
		ImageView readIndicator;
		ImageView starred;
		ImageView image;
		TextView title;
		TextView date;
	}
	StringBuilder sb = new StringBuilder();
	private int lastRequestPosition = -1;
	private String fromChannel;
	private int readIndicatorColor = 0;
	private int unreadIndicatorColor = 0;
	private ActiveList<Item> items = new ActiveList<Item>();
	private Context context;
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == REFRESH_MESSAGE) {
				ItemAdapter.this.notifyDataSetChanged();
			}						
		}
	};
	private OnItemRequestListener itemRequestListener;
	
	private  ActiveList.ActiveListListener<Item> activeListListener = 
		new ActiveList.ActiveListListener<Item>() {
		public void onAdd(Item item) {				
			refresh();
		}
		public void onInsert(final int location, final Item item) {			
			refresh();
		}
		public void onClear() {
			refresh();		
		}
	};
	
	private void refresh() {
		handler.sendEmptyMessage(REFRESH_MESSAGE);		
	}

	public ItemAdapter(Context context) {
		this.context = context;
		ImageLoader.initialize(context);
		readIndicatorColor = context.getResources().getColor(R.color.readIndicator);
		unreadIndicatorColor = context.getResources().getColor(R.color.unreadIndicator);
		fromChannel = context.getString(R.string.from_channel);
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
	public synchronized void setItems(ActiveList<Item> items) {
		lastRequestPosition = -1;
		if (this.items != null) {
			this.items.removeListener(activeListListener);
		}
		this.items = items;
		this.items.addListener(activeListListener);	
		this.notifyDataSetInvalidated();
	}
	public synchronized void addItems(List<Item> items) {		
		if (this.items != null) {
			this.items.addAll(items);
		} else {
			this.items = new ActiveList<Item>();
			this.items.addAll(items);
			this.items.addListener(activeListListener);
		}
		this.notifyDataSetChanged();
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {		
		ViewHolder holder;
		Item item = items.get(position);
		
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.item, null);
			
			holder = new ViewHolder();
			holder.readIndicator = (ImageView)convertView.findViewById(R.id.readIndicator);
			holder.title = (TextView)convertView.findViewById(R.id.itemTitle);
			holder.date = (TextView)convertView.findViewById(R.id.itemDate);
			holder.image = (ImageView)convertView.findViewById(R.id.itemImage);
			holder.starred = (ImageView)convertView.findViewById(R.id.starred);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
				
		holder.title.setText(item.title);
		if (item.isRead()) {
			holder.title.setTextAppearance(context, R.style.ReadTitleText);
			holder.readIndicator.setBackgroundColor(readIndicatorColor);					
		} else {			
			holder.title.setTextAppearance(context, R.style.UnreadTitleText);
			holder.readIndicator.setBackgroundColor(unreadIndicatorColor);
		}
		holder.starred.setSelected(item.starred);
		if (item.pubDate != null) {
			sb.delete(0, sb.length());
			try {
				sb.append(
					DateUtils.formatSameDayTime(item.pubDate.getTime(), 
							System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT));
			} catch (Throwable e) {
				sb.append(dateFormat.format(item.pubDate));
			}
			if (item.channel != null) {
				sb.append(" ");
				sb.append(fromChannel.replace("{channel}", item.channel.title));
			}
			holder.date.setText(sb.toString());
		}
				
		String imageUrl = item.imageUrl;
		holder.image.setVisibility(View.GONE);
		if (imageUrl != null && imageUrl.length() > 0) {
			if (!imageUrl.equals((String)holder.image.getTag())) {
				holder.image.setTag(imageUrl);
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
			} else {
				holder.image.setVisibility(View.VISIBLE);
			}
		}
		
		// request more items if we reach the to 2/3 items		
		int requestPosition = (2 * getCount() / 3);
		if (position == requestPosition 
				&& lastRequestPosition != requestPosition 
				&& itemRequestListener != null) {
			lastRequestPosition = requestPosition;
			itemRequestListener.onRequest((Item)getItem(getCount() - 1));
		}
		
		return convertView;
	}
	
	public void setItemRequestListener(OnItemRequestListener listener) {
		this.itemRequestListener = listener;
	}
	
	public interface OnItemRequestListener {
		void onRequest(Item lastItem);
	}

	public void refreshReadState() {
		for (Item item : this.items) {
			if (ContentManager.isItemRead(item.id)) {
				item.read = Item.READ;
			}
		}
		this.notifyDataSetChanged();
	}
}
