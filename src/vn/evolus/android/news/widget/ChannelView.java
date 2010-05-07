package vn.evolus.android.news.widget;

import java.util.Observable;
import java.util.Observer;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.rss.Item;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class ChannelView extends LinearLayout implements Observer {
	private Channel channel;
	Animation slideLeftIn;
	Animation slideLeftOut;
	Animation slideRightIn;
	Animation slideRightOut;
	
	TextView channelName;
	ViewSwitcher switcher;
	ItemListView itemListView;
	ItemView itemView;
	ViewSwitcher refreshOrProgress;	
	
	private Handler handler;
	private ChannelViewEventListener listener;
	
	public ChannelView(Context context, ChannelViewEventListener listener) {
		super(context);		
		this.handler = new Handler();
		this.listener = listener;		
		LayoutInflater.from(context).inflate(R.layout.channel_view, this, true);
		
		Button back = (Button)findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ChannelView.this.goBack();
			}			
		});
		
		refreshOrProgress = (ViewSwitcher)findViewById(R.id.refreshOrProgress);
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
	
	private void setBusy() {
		refreshOrProgress.setDisplayedChild(1);
	}
	
	private void setIdle() {
		refreshOrProgress.setDisplayedChild(0);
	}	
	
	public void setChannel(Channel channel) {
		if (this.channel != channel) {
			if (this.channel != null) {
				this.channel.deleteObserver(this);
			}
			
			this.channel = channel;
			this.channel.addObserver(this);
			channelName.setText(channel.getTitle());							
			itemListView.setItems(channel.getItems());			
			if (channel.isUpdating()) {
				this.setBusy();
			} else {
				this.setIdle();
			}
			if (channel.isEmpty()) {
	    		refreshChannel();
	    	}			
		}
		showChannel();
	}
	
	public void goBack() {
		if (switcher.getCurrentView() == itemView) {			
			showChannel();
			itemListView.refresh();
		} else {
			listener.onExit();
		}
	}
	
	public void refresh() {
		refreshChannel();
	}
	
	private void refreshChannel() {
		this.setBusy();
		
		BetterAsyncTask<Channel, Void, Channel> task = new BetterAsyncTask<Channel, Void, Channel>(getContext()) {			
			protected void before(Context context) {
			}
			protected void after(Context context, Channel channel) {				
				ChannelView.this.onChannelUpdated(channel);				
			}			
			protected void handleError(Context context, Exception e) {
				Toast.makeText(context, "Cannot load the feed: " + e.getMessage(), 5).show();
				ChannelView.this.setIdle();
			}			
		};
		task.setCallable(new BetterAsyncTaskCallable<Channel, Void, Channel>() {
			public Channel call(BetterAsyncTask<Channel, Void, Channel> task) throws Exception {
				ChannelView.this.channel.update();
				return ChannelView.this.channel;
			}    			
		});
		task.disableDialog();
		task.execute();		       
	}
	
	private void onChannelUpdated(Channel channel) {
		this.channel = channel;
		channelName.setText(channel.getTitle());
		this.setIdle();
	}

	private void showItem(Item item) {
		itemView.setItem(item);
		switcher.setInAnimation(slideLeftIn);
		switcher.setOutAnimation(slideLeftOut);
		switcher.showNext();
	}
	
	private void showChannel() {
		if (switcher.getCurrentView() != itemListView) {
			switcher.setInAnimation(slideRightIn);
			switcher.setOutAnimation(slideRightOut);
			switcher.showPrevious();
		}		
	}

	@Override
	public void update(Observable observable, Object data) {
		if (observable instanceof Channel) {
			Boolean updated = !(Boolean)data;
			if (updated) {
				this.handler.post(new Runnable() {
					public void run() {
						ChannelView.this.setIdle();			
					}					
				});			
			}
		}
	}		
}
