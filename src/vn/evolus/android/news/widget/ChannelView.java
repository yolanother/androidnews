package vn.evolus.android.news.widget;

import vn.evolus.android.news.R;
import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.rss.Item;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
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
	
	public void refreshChannel() {		
        new LoadChannelTask(this).execute(this.channel);
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
	
	private class LoadChannelTask extends AsyncTask<Channel, Void, Channel> {
    	private ChannelView channelView;
    	private ProgressDialog progressDialog;
    	public LoadChannelTask(ChannelView channelView) {
    		this.channelView = channelView;    		
    	}    	
    	protected void onPreExecute() {
    		super.onPreExecute();
    		progressDialog = ProgressDialog.show(channelView.getContext(), "", "Refreshing. Please wait...", true);
    	}
		protected Channel doInBackground(Channel... channels) {			
			return Channel.create(channels[0]);			
		}    	    		
		protected void onPostExecute(Channel channel) {	
			channelView.setChannel(channel);
	        progressDialog.dismiss();
		}
    }	
}
