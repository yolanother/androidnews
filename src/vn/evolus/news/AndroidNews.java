package vn.evolus.news;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import vn.evolus.news.rss.Channel;
import vn.evolus.news.util.ActiveList;
import vn.evolus.news.widget.ChannelListView;
import vn.evolus.news.widget.ChannelView;
import vn.evolus.news.widget.ChannelViewEventListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;

import com.github.droidfu.activities.BetterDefaultActivity;
import com.github.droidfu.concurrent.BetterAsyncTask;
import com.github.droidfu.concurrent.BetterAsyncTaskCallable;

public class AndroidNews extends BetterDefaultActivity {	
	private final int MENU_BACK = 0;
	private final int MENU_REFRESH = 1;
	
	private ActiveList<Channel> channels = null;
	private ViewSwitcher switcher;
	private ChannelListView channelListView;
	private ChannelView channelView;
	
	Animation slideLeftIn;
	Animation slideLeftOut;
	Animation slideRightIn;
	Animation slideRightOut;
	
	public AndroidNews() {
		Log.d("DEBUG", "New instance created!");
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", "onCreate");
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initViews();
        loadData(savedInstanceState);
    }	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);		
		Log.d("DEBUG", "onSaveInstanceState");
		//outState.putSerializable("Channels", (Serializable) channels);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.d("DEBUG", "onPause");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("DEBUG", "onDestroy");
	}
	
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {    	
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	    	if (switcher.getCurrentView() == channelView) {
	    		channelView.goBack();	    		
	    	} else {
	    		confirmExit();
	    	}	        
	    	return false;
	    }
	    return super.onKeyDown(keyCode, event);
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	
    	menu.add(0, MENU_BACK, 0, "Back").setIcon(R.drawable.ic_menu_back);
    	menu.add(0, MENU_REFRESH, 1,  "Refresh").setIcon(R.drawable.ic_menu_refresh);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return (switcher.getCurrentView() == channelView);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == MENU_BACK) {
    		channelView.goBack();
    	} else if (item.getItemId() == MENU_REFRESH){
    		channelView.refresh();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
	@SuppressWarnings({ "unchecked", "unused" })
	private void loadChannels() {
		try {
			final FileInputStream fis = this.openFileInput("channels");			
			final ObjectInputStream ois = new ObjectInputStream(fis);
			channels = (ActiveList<Channel>)ois.readObject();
			fis.close();
			Log.d("DEBUG", "Successful loading channels from disk!");
		} catch (final Exception ex) {
			// initialize channels 
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
	}
	
	@SuppressWarnings("unused")
	private void saveChannels() {
		try {
			FileOutputStream fis = this.openFileOutput("channels", Context.MODE_PRIVATE);
			ObjectOutputStream ois = new ObjectOutputStream(fis);
			ois.writeObject(channels);
			fis.close();
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	    
    private void initViews() {
        switcher = new ViewSwitcher(this);
        setContentView(switcher);
        
        slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        slideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
        
        View mainView = View.inflate(this, R.layout.main, null);
        channelListView = (ChannelListView)mainView.findViewById(R.id.channelListView);
        switcher.addView(mainView);
                
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
    
    @SuppressWarnings("unchecked")
	private void loadData(Bundle savedInstanceState) {   
    	if (savedInstanceState != null && channels == null) {
    		channels = (ActiveList<Channel>)savedInstanceState.getSerializable("Channels");
    	}
    	if (channels != null) {
    		channelListView.setChannels(channels);
    		return;
    	}
    	
    	createDefaultChannels();
    	channelListView.setChannels(channels);
    	
    	BetterAsyncTask<Void, Void, Void> loadingTask = new BetterAsyncTask<Void, Void, Void>(this) {
    		protected void before(Context context) {
    			//progressDialog = ProgressDialog.show(context, "", "Loading data, please wait...", true);
    		}
			protected void after(Context arg0, Void arg1) {				
				//progressDialog.dismiss();				
			}
			protected void handleError(Context arg0, Exception arg1) {
			}    		
    	};
    	loadingTask.setCallable(new BetterAsyncTaskCallable<Void, Void, Void>() {
			public Void call(BetterAsyncTask<Void, Void, Void> arg0)
					throws Exception {
				for (Channel channel : channels) {
					try {
						channel.update();
					} catch (Exception e) {
						Log.e("ERROR", e.getMessage());
					}
				}
				return null;
			}    		
    	});
    	loadingTask.disableDialog();
    	loadingTask.execute();        
    }        
    
    public void goHome() {
    	channelListView.refesh();
    	switcher.setInAnimation(slideRightIn);
		switcher.setOutAnimation(slideRightOut);		
    	switcher.showPrevious();
    }
    
    public void showChannel(Channel channel) {
    	channelView.setChannel(channel);
    	switcher.setInAnimation(slideLeftIn);
		switcher.setOutAnimation(slideLeftOut);
    	switcher.showNext();
    }
    
    private void confirmExit() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Are you sure you want to exit?")
    	       .setCancelable(false)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                AndroidNews.this.finish();
    	           }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
}