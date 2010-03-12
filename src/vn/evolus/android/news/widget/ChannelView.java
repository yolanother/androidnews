package vn.evolus.android.news.widget;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class ChannelView extends LinearLayout {	
	private Channel channel;	
	Animation slideLeftIn;
	Animation slideLeftOut;
	Animation slideRightIn;
	Animation slideRightOut;
	
	TextView channelName;
	ViewSwitcher switcher;
	ItemListView itemListView;
	ItemView itemView;
	
	private ChannelViewEventListener listener;
	
	public ChannelView(Context context, ChannelViewEventListener listener) {
		super(context);
		
		this.listener = listener;
		
		LayoutInflater.from(context).inflate(R.layout.channel_view, this, true);
		
		Button back = (Button)findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ChannelView.this.goBack();
			}			
		});
		
		ImageButton refresh = (ImageButton)findViewById(R.id.refresh);
		refresh.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ChannelView.this.refreshChannel();
			}			
		});
		
		switcher = (ViewSwitcher)findViewById(R.id.switcher);        
		channelName = (TextView)findViewById(R.id.channelName);              
                                
        itemListView = new ItemListView(context);
        switcher.addView(itemListView);
        itemView = new ItemView(context, this);
        switcher.addView(itemView);
                
        slideLeftIn = AnimationUtils.loadAnimation(context, R.anim.slide_left_in);
        slideLeftOut = AnimationUtils.loadAnimation(context, R.anim.slide_left_out);
        slideRightIn = AnimationUtils.loadAnimation(context, R.anim.slide_right_in);
        slideRightOut = AnimationUtils.loadAnimation(context, R.anim.slide_right_out);
                                        
        itemListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Item item = (Item)adapterView.getItemAtPosition(position);				
				ChannelView.this.showItem(item);
			}
        });               
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	    	Log.d("DEBUG", "Back key pressed");
	    	goBack();
	        return false;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public void refreshChannel() {		
		BetterAsyncTask<Channel, Void, Channel> task = new BetterAsyncTask<Channel, Void, Channel>(getContext()) {			
			protected void after(Context context, Channel channel) {				
				ChannelView.this.setChannel(channel);
			}			
			protected void handleError(Context context, Exception e) {
				
			}			
		};		
		task.setCallable(new BetterAsyncTaskCallable<Channel, Void, Channel>() {
			public Channel call(BetterAsyncTask<Channel, Void, Channel> task) throws Exception {
				return Channel.create(ChannelView.this.channel);				
			}    			
		});		
		task.disableDialog();
		task.execute();		       
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
		channelName.setText(channel.getTitle());
		itemListView.setItems(channel.getItems());
		showChannel();
	}
	
	public void goBack() {
		if (switcher.getCurrentView() == itemView) {
			showChannel();
		} else {
			listener.onExit();
		}
	}
	
	public void showItem(Item item) {
		itemView.setItem(item);
		switcher.setInAnimation(slideLeftIn);
		switcher.setOutAnimation(slideLeftOut);
		switcher.showNext();
	}
	
	public void showChannel() {
		if (switcher.getCurrentView() != itemListView) {
			switcher.setInAnimation(slideRightIn);
			switcher.setOutAnimation(slideRightOut);
			switcher.showPrevious();
		}
	}		
}