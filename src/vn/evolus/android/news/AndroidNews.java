package vn.evolus.android.news;

import vn.evolus.android.news.adapter.NewsListViewAdapter;
import vn.evolus.android.news.rss.Channel;
import vn.evolus.android.news.widget.ChannelView;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class AndroidNews extends Activity {	
	private ProgressDialog progressDialog;
	private ViewSwitcher switcher;
	
	public AndroidNews() {		
	}	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        
        switcher = new ViewSwitcher(this);
        setContentView(switcher);
        
        ChannelView channelView = new ChannelView(this);
        View detailView = View.inflate(this, R.layout.detail_view, null);
        
        switcher.addView(channelView);
        switcher.addView(detailView);
        
        progressDialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
        new LoadChannelTask(channelView, progressDialog).execute("http://feeds.feedburner.com/engadget/full");               
    }
    
    private class LoadChannelTask extends AsyncTask<String, Void, Channel> {
    	private ChannelView channelView;
    	private ProgressDialog progressDialog;
    	public LoadChannelTask(ChannelView channelView, ProgressDialog progressDialog) {
    		this.channelView = channelView;
    		this.progressDialog = progressDialog;
    	}
		@Override
		protected Channel doInBackground(String... url) {
			return Channel.create(url[0]);			
		}    	    
		
		@Override
		protected void onPostExecute(Channel channel) {	
			channelView.setChannel(channel);
	        progressDialog.dismiss();
		}
    }
}