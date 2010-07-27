package vn.evolus.news.adapter;

import java.lang.ref.WeakReference;

import vn.evolus.news.R;
import vn.evolus.news.model.Item;
import vn.evolus.news.util.ActiveList;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.util.ImageLoaderHandler;
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

public class ItemListViewAdapter extends BaseAdapter {
	private static final int REFRESH_MESSAGE = 1;
	static class ViewHolder {
		ImageView readIndicator;
		ImageView image;
		TextView title;
		TextView date;
	}
	
	private int readIndicatorColor = 0;
	private int unreadIndicatorColor = 0;
	private ActiveList<Item> items = new ActiveList<Item>();
	private Context context;
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == REFRESH_MESSAGE) {
				ItemListViewAdapter.this.notifyDataSetChanged();
			}						
		}
	};
	
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
		Message message = handler.obtainMessage();
		message.what = REFRESH_MESSAGE;
		handler.sendMessage(message);		
	}

	public ItemListViewAdapter(Context context) {
		this.context = context;
		ImageLoader.initialize(context);
		readIndicatorColor = context.getResources().getColor(R.color.readIndicator);
		unreadIndicatorColor = context.getResources().getColor(R.color.unreadIndicator);
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
		if (this.items != null) {
			this.items.removeListener(activeListListener);
		}
		this.items = items;
		this.items.addListener(activeListListener);	
		this.notifyDataSetInvalidated();
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
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
				
		holder.title.setText(item.getTitle());
		if (item.getRead()) {
			holder.title.setTextAppearance(context, R.style.ReadTitleText);
			holder.readIndicator.setBackgroundColor(readIndicatorColor);					
		} else {			
			holder.title.setTextAppearance(context, R.style.UnreadTitleText);
			holder.readIndicator.setBackgroundColor(unreadIndicatorColor);
		}
		if (item.getPubDate() != null) {
			holder.date.setText(DateUtils.getRelativeDateTimeString(context, item.getPubDate().getTime(), 1, DateUtils.DAY_IN_MILLIS, 0));
		}
		
		holder.image.setImageBitmap(null);
		holder.image.setVisibility(View.GONE);
		String imageUrl = item.getImageUrl();
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
	
	private class ItemImageLoaderHandler extends ImageLoaderHandler {
		private WeakReference<ImageView> imageRef;
		private String imageUrl;
		
		public ItemImageLoaderHandler(ImageView imageView, String imageUrl) {
			this.imageRef = new WeakReference<ImageView>(imageView);
			this.imageUrl = imageUrl;
		}
		public void handleMessage(Message msg) {
	        super.handleMessage(msg);
	        if (msg.what == ImageLoader.BITMAP_DOWNLOADED_SUCCESS) {
	        	ImageView imageView = imageRef.get();
	        	if (imageView == null) return;
	        	
		        if (imageUrl.equals((String)imageView.getTag())) {
		        	imageView.setVisibility(View.VISIBLE);
		        	imageView.setImageBitmap(super.getImage());
		        }
	        }
	    }
	}
}
