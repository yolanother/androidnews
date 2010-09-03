package vn.evolus.droidnews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.evolus.droidnews.adapter.SuggestedChannelsAdapter;
import vn.evolus.droidnews.content.ContentManager;
import vn.evolus.droidnews.model.Channel;
import vn.evolus.droidnews.model.ChannelGroup;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnChildClickListener;

import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class SubscriptionActivity extends Activity {
	private TextView channelUrl;
	private ExpandableListView listView;
	private SuggestedChannelsAdapter adapter;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		listView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, 
					int groupPosition, int childPosition, long id) {
				Channel channel = (Channel)adapter.getChild(groupPosition, childPosition);
				if (channel.id == 0) {
					startAddChannel(channel.url);
				} else {
					removeChannel(channel);
				}
				return true;
			}			
		});
	}

	protected void removeChannel(Channel channel) {
		ContentManager.deleteChannel(channel);
		adapter.notifyDataSetChanged();
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
		
		group = new ChannelGroup();
		group.setTitle("VnExpress");
		channels = new ArrayList<Channel>();
		channels.add(new Channel("VnExpress - Xã hội", "http://feeds.feedburner.com/androidnews/vnexpress/xahoi"));
		channels.add(new Channel("VnExpress - Đời sống", "http://feeds.feedburner.com/androidnews/vnexpress/doisong"));
		channels.add(new Channel("VnExpress - Kinh doanh", "http://feeds.feedburner.com/androidnews/vnexpress/kinhdoanh"));
		channels.add(new Channel("VnExpress - Vi tính", "http://feeds.feedburner.com/androidnews/vnexpress/vitinh"));
		channels.add(new Channel("VnExpress - Ôtô & Xe máy", "http://feeds.feedburner.com/androidnews/vnexpress/oto-xemay"));
		channels.add(new Channel("VnExpress - Thế giới", "http://feeds.feedburner.com/androidnews/vnexpress/thegioi"));
		channels.add(new Channel("VnExpress - Thể thao", "http://feeds.feedburner.com/androidnews/vnexpress/thethao"));
		channels.add(new Channel("VnExpress - Văn hóa", "http://feeds.feedburner.com/androidnews/vnexpress/vanhoa"));
		channels.add(new Channel("VnExpress - Pháp luật", "http://feeds.feedburner.com/androidnews/vnexpress/phapluat"));
		channels.add(new Channel("VnExpress - Khoa học", "http://feeds.feedburner.com/androidnews/vnexpress/khoahoc"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		group = new ChannelGroup();
		group.setTitle("VietnamNet");
		channels = new ArrayList<Channel>();
		channels.add(new Channel("VietNamNet - CNTT - Viễn thông", "http://feeds.feedburner.com/androidnews/vietnamnet/cntt"));
		channels.add(new Channel("VietNamNet - Kinh tế", "http://feeds.feedburner.com/androidnews/vietnamnet/kinhte"));
		channels.add(new Channel("VietNamNet - Chính trị", "http://feeds.feedburner.com/androidnews/vietnamnet/chinhtri"));
		channels.add(new Channel("VietNamNet - Xã hội", "http://feeds.feedburner.com/androidnews/vietnamnet/xahoi"));
		channels.add(new Channel("VietNamNet - Khoa học", "http://feeds.feedburner.com/androidnews/vietnamnet/khoahoc"));
		channels.add(new Channel("VietNamNet - Văn hóa", "http://feeds.feedburner.com/androidnews/vietnamnet/vanhoa"));
		channels.add(new Channel("VietNamNet - Thế giới", "http://feeds.feedburner.com/androidnews/vietnamnet/thegioi"));		
		channels.add(new Channel("VietNamNet - Giáo dục", "http://feeds.feedburner.com/androidnews/vietnamnet/giaoduc"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		group = new ChannelGroup();
		group.setTitle("Tuổi Trẻ");
		channels = new ArrayList<Channel>();
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống số", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongso"));
		channels.add(new Channel("Tuổi Trẻ - Chính trị - Xã hội", "http://feeds.feedburner.com/androidnews/tuoitre/chinhtri-xahoi"));		
		channels.add(new Channel("Tuổi Trẻ - Kinh tế", "http://feeds.feedburner.com/androidnews/tuoitre/kinhte"));
		channels.add(new Channel("Tuổi Trẻ - Văn hóa - Giải Trí", "http://feeds.feedburner.com/androidnews/tuoitre/vanhoa-giaitri"));
		channels.add(new Channel("Tuổi Trẻ - Thế giới", "http://feeds.feedburner.com/androidnews/tuoitre/thegioi"));
		channels.add(new Channel("Tuổi Trẻ - Giáo dục", "http://feeds.feedburner.com/androidnews/tuoitre/giaoduc"));
		channels.add(new Channel("Tuổi Trẻ - Thể thao", "http://feeds.feedburner.com/androidnews/tuoitre/thethao"));		
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống trẻ", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongtre"));
		group.setChannels(channels);
		channelGroups.add(group);
		
		checkSubscritions(channelGroups, ContentManager.loadAllChannels(ContentManager.LIGHTWEIGHT_CHANNEL_LOADER));
		
		adapter = new SuggestedChannelsAdapter(this, channelGroups);
		listView.setAdapter(adapter);
	}
	
	private void checkSubscritions(List<ChannelGroup> channelGroups,
			ArrayList<Channel> subscribedChannels) {
		Map<String, Long> channelMap = new HashMap<String, Long>(); 
		for (Channel channel : subscribedChannels) {
			channelMap.put(channel.url, channel.id);
		}
		for (ChannelGroup channelGroup : channelGroups) {
			for (Channel channel : channelGroup.getChannels()) {
				if (channelMap.containsKey(channel.url)) {
					channel.id = channelMap.get(channel.url);
				}
			}
		}
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
					adapter.updateChannel(channel);
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
