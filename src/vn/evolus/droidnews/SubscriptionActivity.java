package vn.evolus.droidnews;

import java.util.ArrayList;
import java.util.List;

import vn.evolus.droidnews.adapter.SuggestedChannelsAdapter;
import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.ChannelGroup;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class SubscriptionActivity extends Activity {
	private TextView channelUrl;
	private ExpandableListView listView;
	private SuggestedChannelsAdapter adapter;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		
		setContentView(R.layout.subscription);
		
		channelUrl = (TextView)findViewById(R.id.channelUrl);
		Button addButton = (Button)findViewById(R.id.add);
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				addChannel();
			}			
		});
		listView = (ExpandableListView)findViewById(R.id.channels);
		loadSuggestedChannels();
	}

	protected void addChannel() {
		String url = channelUrl.getText().toString();
		startAddChannel(url);
	}
	
	private void loadSuggestedChannels() {
		List<ChannelGroup> channelGroups = new ArrayList<ChannelGroup>();
		ChannelGroup group = new ChannelGroup();
		group.setTitle("Technology");
		List<Channel> channels = new ArrayList<Channel>();
		channels.add(new Channel("Engadget", "http://feeds.feedburner.com/androidnews/engadget"));		
		channels.add(new Channel("Gizmodo", "http://feeds.feedburner.com/androidnews/gizmodo"));
		channels.add(new Channel("TechCrunch", "http://feeds.feedburner.com/TechCrunch"));
		channels.add(new Channel("Lifehacker", "http://feeds.feedburner.com/androidnews/lifehacker"));
		channels.add(new Channel("Download Squad", "http://feeds.feedburner.com/androidnews/downloadsquad"));
		channels.add(new Channel("GigaOM", "http://feeds.feedburner.com/ommalik"));
		channels.add(new Channel("Ars Technica", "http://feeds.arstechnica.com/arstechnica/everything"));
		channels.add(new Channel("Boy Genius Report", "http://feeds.feedburner.com/TheBoyGeniusReport"));
		channels.add(new Channel("ReadWriteWeb", "http://feeds.feedburner.com/readwriteweb"));
		channels.add(new Channel("The Design Blog", "http://feeds.feedburner.com/thedesignblog/ntLw"));
		channels.add(new Channel("Smashing Magazine", "http://feeds.feedburner.com/androidnews/smashingmagazine"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		group = new ChannelGroup();
		group.setTitle("Android");		
		channels = new ArrayList<Channel>();
		channels.add(new Channel("Android and Me", "http://feeds.feedburner.com/androidandme"));		
		channels.add(new Channel("AndroidGuys", "http://feeds.feedburner.com/androidguyscom"));
		channels.add(new Channel("Android Phone Fans", "http://feeds2.feedburner.com/AndroidPhoneFans"));
		channels.add(new Channel("Android Community", "http://feeds2.feedburner.com/AndroidCommunity"));
		channels.add(new Channel("AndroidSpin", "http://feeds.feedburner.com/androidspin"));
		channels.add(new Channel("Android Central", "http://feeds2.feedburner.com/androidcentral"));
		channels.add(new Channel("Google Android Blog", "http://feeds.feedburner.com/androinica"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		group = new ChannelGroup();
		group.setTitle("Số Hóa");
		channels = new ArrayList<Channel>();
		channels.add(new Channel("Số Hóa - Điện thoại", "http://feeds.feedburner.com/androidnews/sohoa/dienthoai"));
		channels.add(new Channel("Số Hóa - Máy tính", "http://feeds.feedburner.com/androidnews/sohoa/maytinh"));
		channels.add(new Channel("Số Hóa - Camera", "http://feeds.feedburner.com/androidnews/sohoa/camera"));
		channels.add(new Channel("Số Hóa - Hình ảnh", "http://feeds.feedburner.com/androidnews/sohoa/hinhanh"));
		channels.add(new Channel("Số Hóa - Âm thanh", "http://feeds.feedburner.com/androidnews/sohoa/amthanh"));
		channels.add(new Channel("Số Hóa - Đồ chơi số", "http://feeds.feedburner.com/androidnews/sohoa/dochoiso"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		adapter = new SuggestedChannelsAdapter(this, channelGroups);
		listView.setAdapter(adapter);
	}
	
	private void startAddChannel(String url) {    	
    	final int maxItemsPerChannel = Settings.getMaxItemsPerChannel(this);
    	final Channel channel = new Channel(url);
    	if (ContentManager.existChannel(channel)) {
    		String message = getString(R.string.channel_already_exists).replace("{url}", channel.url);
			Toast.makeText(this, message, 1000).show();
    		return;
    	}
    	ContentManager.saveChannel(channel);
    	
    	final ProgressDialog progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage(getString(R.string.adding_channel));
    	BetterAsyncTask<Void, Void, Void> addChannelTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context context, Void arg1) {
				progressDialog.dismiss();
				if (channel.getItems().size() > 0) {
					ContentManager.saveChannel(channel);
					String message = context.getString(R.string.add_channel_successfully).replace("{channel}", channel.title);
					Toast.makeText(context, message, 1000).show();
					finish();
				} else {
					ContentManager.deleteChannel(channel);
					String message = context.getString(R.string.add_channel_failed).replace("{url}", channel.url);
					Toast.makeText(context, message, 1000).show();					
				}				
			}
			@Override
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	addChannelTask.disableDialog();
    	addChannelTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0) throws Exception {
				channel.update(maxItemsPerChannel);
				return null;
			}    		
    	});
    	progressDialog.show();
    	addChannelTask.execute();    	
    }
}
