package vn.evolus.android.news;

import java.util.ArrayList;
import java.util.List;

import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.widget.ChannelListView;
import vn.evolus.android.news.widget.ChannelView;
import vn.evolus.android.news.widget.ChannelViewEventListener;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.activities.BetterDefaultActivity;

public class AndroidNews extends BetterDefaultActivity {
	private List<Channel> channels;
	private ViewSwitcher switcher;
	private ChannelListView channelListView;
	private ChannelView channelView;
	
	Animation slideLeftIn;
	Animation slideLeftOut;
	Animation slideRightIn;
	Animation slideRightOut;
	
	public AndroidNews() {
		channels = new ArrayList<Channel>();
		channels.add(new Channel("Engadget", "http://feeds.feedburner.com/engadget/full"));
		channels.add(new Channel("Gizmodo", "http://feeds.feedburner.com/gizmodo/4androidnews"));
		channels.add(new Channel("TechCrunch", "http://feeds.feedburner.com/TechCrunch"));
		channels.add(new Channel("Lifehacker", "http://feeds.gawker.com/lifehacker/full"));
		channels.add(new Channel("Download Squad", "http://feeds.feedburner.com/downloadsquad/full"));
						
		channels.add(new Channel("Vietstock", "http://feeds.feedburner.com/androidnews/vietstock/chungkhoan"));
		channels.add(new Channel("CafeF", "http://feeds.feedburner.com/androidnews/cafef/chungkhoan"));				
		channels.add(new Channel("VNExpress - Xã hội", "http://feeds.feedburner.com/androidnews/vnexpress/xahoi"));
		channels.add(new Channel("VNExpress - Kinh doanh", "http://feeds.feedburner.com/androidnews/vnexpress/kinhdoanh"));
		channels.add(new Channel("VNExpress - Vi tính", "http://feeds.feedburner.com/androidnews/vnexpress/vitinh"));
		channels.add(new Channel("VNExpress - Ôtô & Xe máy", "http://feeds.feedburner.com/androidnews/vnexpress/oto-xemay"));
		channels.add(new Channel("Tuổi Trẻ - Xã hội", "http://feeds.feedburner.com/androidnews/tuoitre/chinhtri-xahoi"));
		channels.add(new Channel("Tuổi Trẻ - Kinh tế", "http://feeds.feedburner.com/androidnews/tuoitre/kinhte"));
		
		channels.add(new Channel("Ars Technica", "http://feeds.arstechnica.com/arstechnica/everything"));
		channels.add(new Channel("Android and Me", "http://feeds.feedburner.com/androidandme"));		
		channels.add(new Channel("AndroidGuys", "http://feeds.feedburner.com/androidguyscom"));
		channels.add(new Channel("Android Phone Fans", "http://feeds2.feedburner.com/AndroidPhoneFans"));		
	}	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE | Window.FEATURE_INDETERMINATE_PROGRESS);
        
        switcher = new ViewSwitcher(this);
        setContentView(switcher);
        
        slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        slideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
        
        View mainView = View.inflate(this, R.layout.main, null);
        channelListView = (ChannelListView)mainView.findViewById(R.id.channelListView);
        switcher.addView(mainView);
        
        channelListView.setChannels(channels);
        channelListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);				
				AndroidNews.this.showChannel(channel);
			}
        });
        
        channelView = new ChannelView(this, new ChannelViewEventListener() {			
			public void onExit() {
				AndroidNews.this.goHome();
			}        	
        });        
        switcher.addView(channelView);
    }  
    
    public void goHome() {
    	switcher.setInAnimation(slideRightIn);
		switcher.setOutAnimation(slideRightOut);
    	switcher.showPrevious();
    }
    
    public void showChannel(Channel channel) {
    	channelView.setChannel(channel);
    	switcher.setInAnimation(slideLeftIn);
		switcher.setOutAnimation(slideLeftOut);
    	switcher.showNext();
    	if (channel.getItems().size() == 0) {
    		channelView.refreshChannel();
    	}
    }
}