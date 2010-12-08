package vn.evolus.droidreader.adapter;

import java.util.ArrayList;
import java.util.List;

import vn.evolus.droidreader.R;
import vn.evolus.droidreader.model.Subscription;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchResultAdapter extends BaseAdapter {
	static class ViewHolder {
		ImageView image;
		TextView title;		
	}
	
	private List<Subscription> subscriptions;
	private Context context;

	public SearchResultAdapter(Context context) {
		this.context = context;
		subscriptions = new ArrayList<Subscription>();
	}

	public int getCount() {
		return subscriptions.size();
	}

	public Object getItem(int position) {		
		return subscriptions.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		Subscription subscription = (Subscription)getItem(position);
		CheckedTextView title;
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.channel_subscription, null);
			title = (CheckedTextView)convertView.findViewById(R.id.title);
			convertView.setTag(title);
		} else {
			title = (CheckedTextView)convertView.getTag();
		}
        title.setText(subscription.title);
        title.setChecked(subscription.subscribed);
        
        return convertView;
	}

	public void setResults(List<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
		notifyDataSetChanged();
	}

	public List<Subscription> getResults() {		
		return this.subscriptions;
	}
}
