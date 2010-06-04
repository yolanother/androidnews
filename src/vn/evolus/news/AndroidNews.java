package vn.evolus.news;

import java.util.ArrayList;

import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ActiveList;
import vn.evolus.news.util.ImageLoader;
import vn.evolus.news.widget.ChannelListView;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.activities.BetterDefaultActivity;
import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class AndroidNews extends BetterDefaultActivity {		
	private final int MENU_REFRESH = 0;
	private final int MENU_SETTINGS = 1;
	
	private ArrayList<Channel> channels = null;
	private ChannelListView channelListView;
		
	public AndroidNews() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		ImageLoader.initialize(this);
		
        super.onCreate(savedInstanceState);        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        channelListView = (ChannelListView)findViewById(R.id.channelListView);               
        channelListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Channel channel = (Channel)adapterView.getItemAtPosition(position);				
				AndroidNews.this.showChannel(channel);
			}
        });
                
        loadData();
    }	
	    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	    	
    	menu.add(0, MENU_REFRESH, 1,  getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
    	menu.add(0, MENU_SETTINGS, 1,  getString(R.string.settings)).setIcon(android.R.drawable.ic_menu_preferences);
    	return super.onCreateOptionsMenu(menu);
    }        
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_SETTINGS) {
    		//finish();
    	} else if (item.getItemId() == MENU_REFRESH){
    		refresh();
    	}
		return true;
	}
    
	private void loadChannels() {
		channels = Channel.loadAllChannels(getContentResolver());
		if (channels == null || channels.isEmpty()) {
			createDefaultChannels();
		}
	}

	private void createDefaultChannels() {
		channels = new ActiveList<Channel>();
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
		
		channels.add(new Channel("Android and Me", "http://feeds.feedburner.com/androidandme"));		
		channels.add(new Channel("AndroidGuys", "http://feeds.feedburner.com/androidguyscom"));
		channels.add(new Channel("Android Phone Fans", "http://feeds2.feedburner.com/AndroidPhoneFans"));
		channels.add(new Channel("Android Community", "http://feeds2.feedburner.com/AndroidCommunity"));
		channels.add(new Channel("AndroidSpin", "http://feeds.feedburner.com/androidspin"));
		channels.add(new Channel("Android Central", "http://feeds2.feedburner.com/androidcentral"));
		channels.add(new Channel("Google Android Blog", "http://feeds.feedburner.com/androinica"));
		
		channels.add(new Channel("Tinh tế", "http://feeds.feedburner.com/androidnews/tinhte"));		
		channels.add(new Channel("Số Hóa - Điện thoại", "http://feeds.feedburner.com/androidnews/sohoa/dienthoai"));
		channels.add(new Channel("Số Hóa - Máy tính", "http://feeds.feedburner.com/androidnews/sohoa/maytinh"));
		channels.add(new Channel("Số Hóa - Camera", "http://feeds.feedburner.com/androidnews/sohoa/camera"));
		channels.add(new Channel("Số Hóa - Hình ảnh", "http://feeds.feedburner.com/androidnews/sohoa/hinhanh"));
		channels.add(new Channel("Số Hóa - Âm thanh", "http://feeds.feedburner.com/androidnews/sohoa/amthanh"));
		channels.add(new Channel("Số Hóa - Đồ chơi số", "http://feeds.feedburner.com/androidnews/sohoa/dochoiso"));
		
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
		
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống số", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongso"));
		channels.add(new Channel("Tuổi Trẻ - Chính trị - Xã hội", "http://feeds.feedburner.com/androidnews/tuoitre/chinhtri-xahoi"));		
		channels.add(new Channel("Tuổi Trẻ - Kinh tế", "http://feeds.feedburner.com/androidnews/tuoitre/kinhte"));
		channels.add(new Channel("Tuổi Trẻ - Văn hóa - Giải Trí", "http://feeds.feedburner.com/androidnews/tuoitre/vanhoa-giaitri"));
		channels.add(new Channel("Tuổi Trẻ - Thế giới", "http://feeds.feedburner.com/androidnews/tuoitre/thegioi"));
		channels.add(new Channel("Tuổi Trẻ - Giáo dục", "http://feeds.feedburner.com/androidnews/tuoitre/giaoduc"));
		channels.add(new Channel("Tuổi Trẻ - Thể thao", "http://feeds.feedburner.com/androidnews/tuoitre/thethao"));		
		channels.add(new Channel("Tuổi Trẻ - Nhịp sống trẻ", "http://feeds.feedburner.com/androidnews/tuoitre/nhipsongtre"));
		
		channels.add(new Channel("VietNamNet - CNTT - Viễn thông", "http://feeds.feedburner.com/androidnews/vietnamnet/cntt"));
		channels.add(new Channel("VietNamNet - Kinh tế", "http://feeds.feedburner.com/androidnews/vietnamnet/kinhte"));
		channels.add(new Channel("VietNamNet - Chính trị", "http://feeds.feedburner.com/androidnews/vietnamnet/chinhtri"));
		channels.add(new Channel("VietNamNet - Xã hội", "http://feeds.feedburner.com/androidnews/vietnamnet/xahoi"));
		channels.add(new Channel("VietNamNet - Khoa học", "http://feeds.feedburner.com/androidnews/vietnamnet/khoahoc"));
		channels.add(new Channel("VietNamNet - Văn hóa", "http://feeds.feedburner.com/androidnews/vietnamnet/vanhoa"));
		channels.add(new Channel("VietNamNet - Thế giới", "http://feeds.feedburner.com/androidnews/vietnamnet/thegioi"));		
		channels.add(new Channel("VietNamNet - Giáo dục", "http://feeds.feedburner.com/androidnews/vietnamnet/giaoduc"));
				
		channels.add(new Channel("Vietstock", "http://feeds.feedburner.com/androidnews/vietstock/chungkhoan"));
		channels.add(new Channel("CafeF", "http://feeds.feedburner.com/androidnews/cafef/chungkhoan"));
		
		saveChannels();
	}
		
	private void saveChannels() {
		try {
			ContentResolver cr = getContentResolver();
			for (Channel channel : this.channels) {
				channel.save(cr);
			}
			Log.d("DEBUG", "Successful saving channels to disk!");
		} catch (Exception e) {		
			e.printStackTrace();
		}
	}	        
        
	private void loadData() {   
    	loadChannels();    	
    	channelListView.setChannels(channels);    	
    	refresh();
    	
//    	BetterAsyncTask<Void, Void, Void> loadingTask = new BetterAsyncTask<Void, Void, Void>(this) {
//			@Override
//			protected void after(Context arg0, Void arg1) {				
//				refresh();
//			}
//			@Override
//			protected void handleError(Context arg0, Exception arg1) {
//			}    		
//    	};
//    	loadingTask.disableDialog();
//    	loadingTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
//			public Void call(BetterAsyncTask<Void, Void, Void> arg0) throws Exception {
//				for (Channel channel : channels) {
//					try {
//						//channel.load(getContentResolver());
//						channelListView.refresh();
//					} catch (Exception e) {
//						Log.e("ERROR", e.getMessage());
//					}
//				}				
//				return null;
//			}    		
//    	});    	
//    	loadingTask.execute();    	    	
    }
	
	private void refresh() {
		BetterAsyncTask<Void, Void, Void> refreshTask = new BetterAsyncTask<Void, Void, Void>(this) {
			@Override
			protected void after(Context arg0, Void arg1) {
			}
			@Override
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	refreshTask.disableDialog();
    	refreshTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0)
					throws Exception {
				ContentResolver cr = getContentResolver();
				for (Channel channel : channels) {
					try {						
						channel.update(cr);
						channelListView.refresh();
					} catch (Exception e) {
						Log.e("ERROR", e.getMessage());
					}
				}
				return null;
			}    		
    	});
    	refreshTask.execute();
	}        
    
    private void showChannel(Channel channel) {
    	Intent intent = new Intent(this, ChannelActivity.class);
    	intent.putExtra("ChannelId", channel.getId());
    	intent.putExtra("ChannelTitle", channel.getTitle());
    	startActivity(intent);
    }
    
    @Override
    public void finish() {
    	//saveChannels();
    	super.finish();
    }
        
}