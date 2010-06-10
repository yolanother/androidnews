package vn.evolus.news.adapter;

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
		ImageView image;
		TextView title;
		TextView date;
	}
	
	private int itemTitleReadColor = 0;
	private int itemTitleColor = 0;
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
		itemTitleReadColor = context.getResources().getColor(R.color.itemTitleRead);
		itemTitleColor = context.getResources().getColor(R.color.itemTitle);
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
			holder.title = (TextView)convertView.findViewById(R.id.itemTitle);
			holder.date = (TextView)convertView.findViewById(R.id.itemDate);
			holder.image = (ImageView)convertView.findViewById(R.id.itemImage);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
				
		holder.title.setText(item.getTitle());
		if (item.getRead()) {
			holder.title.setTextColor(itemTitleReadColor);
		} else {
			holder.title.setTextColor(itemTitleColor);
		}
		if (item.getPubDate() != null) {
			holder.date.setText(DateUtils.getRelativeDateTimeString(context, item.getPubDate().getTime(), 1, DateUtils.DAY_IN_MILLIS, 0));
		}
		holder.image.setImageResource(R.drawable.no_image);
		String imageUrl = item.getImageUrl();
		holder.image.setTag(imageUrl);
		if (imageUrl != null) {
			try {
				Bitmap itemImage = ImageLoader.get(imageUrl);			
				if (itemImage != null) {
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
		private ImageView imageView;
		private String imageUrl;
		
		public ItemImageLoaderHandler(ImageView imageView, String imageUrl) {
			this.imageView = imageView;
			this.imageUrl = imageUrl;
		}
		public void handleMessage(Message msg) {
	        super.handleMessage(msg);
	        if (msg.what == ImageLoader.BITMAP_DOWNLOADED_SUCCESS) {
		        if (imageUrl.equals((String)imageView.getTag())) {
		        	imageView.setImageBitmap(super.getImage());
		        }
	        }
	    }
	}
}
