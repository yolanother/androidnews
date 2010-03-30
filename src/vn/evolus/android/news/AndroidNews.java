package vn.evolus.android.news;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.widget.ChannelListView;
import vn.evolus.android.news.widget.ChannelView;
import vn.evolus.android.news.widget.ChannelViewEventListener;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
	private List<Channel> channels;
	private ViewSwitcher switcher;
	private ChannelListView channelListView;
	private ChannelView channelView;
	private ProgressDialog progressDialog;
	
	Animation slideLeftIn;
	Animation slideLeftOut;
	Animation slideRightIn;
	Animation slideRightOut;
	
	public AndroidNews() {
	}

	@SuppressWarnings("unchecked")
	private void loadChannels() {
		try {
			FileInputStream fis = this.openFileInput("channels");			
			ObjectInputStream ois = new ObjectInputStream(fis);
			channels = (ArrayList<Channel>)ois.readObject();
			fis.close();
			Log.d("DEBUG", "Successful loading channels from disk!");
		} catch (Exception ex) {
			// initialize channels 
			createDefaultChannels();
		}
	}

	private void createDefaultChannels() {
		channels = new ArrayList<Channel>();
		channels.add(new Channel("Engadget", "http://feeds.feedburner.com/androidnews/engadget"));
		channels.add(new Channel("Gizmodo", "http://feeds.feedburner.com/androidnews/gizmodo"));
		channels.add(new Channel("TechCrunch", "http://feeds.feedburner.com/TechCrunch"));
		channels.add(new Channel("Lifehacker", "http://feeds.feedburner.com/androidnews/lifehacker"));
		channels.add(new Channel("Download Squad", "http://feeds.feedburner.com/androidnews/downloadsquad"));
		channels.add(new Channel("GigaOM", "http://feeds.feedburner.com/ommalik"));
		channels.add(new Channel("Ars Technica", "http://feeds.arstechnica.com/arstechnica/everything"));
		channels.add(new Channel("Boy Genius Report", "http://feeds.feedburner.com/TheBoyGeniusReport"));
		channels.add(new Channel("ReadWriteWeb", "http://feeds.feedburner.com/readwriteweb"));
		
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
		
		channels.add(new Channel("Android and Me", "http://feeds.feedburner.com/androidandme"));		
		channels.add(new Channel("AndroidGuys", "http://feeds.feedburner.com/androidguyscom"));
		channels.add(new Channel("Android Phone Fans", "http://feeds2.feedburner.com/AndroidPhoneFans"));
		channels.add(new Channel("Android Community", "http://feeds2.feedburner.com/AndroidCommunity"));
		channels.add(new Channel("AndroidSpin", "http://feeds.feedburner.com/androidspin"));
		channels.add(new Channel("Android Central", "http://feeds2.feedburner.com/androidcentral"));
		channels.add(new Channel("Google Android Blog", "http://feeds.feedburner.com/androinica"));
	}
	
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        initViews();
        loadData();
    }

    private void initViews() {    	
        switcher = new ViewSwitcher(AndroidNews.this);
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
    
    private void loadData() {    
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
    
    public void finish() {
    	//saveChannels();
    	if (progressDialog != null) {
    		progressDialog.dismiss();
    	}
    	super.finish();
    }
    
    public void exit() {
    	//progressDialog = ProgressDialog.show(this, "", "Saving data, please wait...", true);
    	new Thread(new Runnable() {
			public void run() {				
				finish();				
			}    		
    	}).start();
    }
    
    public void goHome() {
    	switcher.setInAnimation(slideRightIn);
		switcher.setOutAnimation(slideRightOut);		
    	switcher.showPrevious();
    	channelListView.refesh();
    }
    
    public void showChannel(Channel channel) {
    	channelView.setChannel(channel);
    	switcher.setInAnimation(slideLeftIn);
		switcher.setOutAnimation(slideLeftOut);
    	switcher.showNext();    	
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
    
    private void confirmExit() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Are you sure you want to exit?")
    	       .setCancelable(false)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                AndroidNews.this.exit();
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