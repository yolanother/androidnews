package vn.evolus.droidreader.content;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.evolus.droidreader.ConnectivityReceiver;
import vn.evolus.droidreader.Constants;
import vn.evolus.droidreader.GoogleReaderFactory;
import vn.evolus.droidreader.Settings;
import vn.evolus.droidreader.model.Channel;
import vn.evolus.droidreader.model.Item;
import vn.evolus.droidreader.model.Job;
import vn.evolus.droidreader.model.JobExecutor;
import vn.evolus.droidreader.model.SyncItemTagJobExecutor;
import vn.evolus.droidreader.model.Tag;
import android.util.Log;

import com.google.reader.GoogleReader;
import com.google.reader.Subscription;
import com.google.reader.atom.AtomFeed;
import com.google.reader.atom.Entry;
import com.google.reader.atom.AtomHandler.OnNewEntryCallback;

public class SynchronizationManager {
	private static SynchronizationManager instance;	
	private static final String TAG = "SynchronizationManager";	
		
	private Object synRoot = new Object();
	private boolean synchronizing = false;
	private Map<String, Channel> channelMap;
	private Map<String, JobExecutor> jobExecutors;
	
	static {
		instance = new SynchronizationManager();
	}
	
	public SynchronizationManager() {
		Log.d(TAG, "SynchronizationManager() is called.");
		jobExecutors = new HashMap<String, JobExecutor>();
		registerJobExecutor(new SyncItemTagJobExecutor(GoogleReaderFactory.getGoogleReader()));
	}
	
	public static SynchronizationManager getInstance() {
		return instance;
	}
	
	public boolean isSynchronizing() {
		synchronized (synRoot) {
			return synchronizing;
		}
	}
	
	public int startSynchronizing() {
		synchronized (synRoot) {
			if (synchronizing) {
				Log.d(TAG, "Synchronizing... return now.");
				return 0;
			}
			synchronizing = true;
		}
		int totalNewItems = 0;
		if (ConnectivityReceiver.hasGoodEnoughNetworkConnection()) {
			Log.i(TAG, "Start synchronization at " + new Date());
			try {
				long timestamp = System.currentTimeMillis();
				syncTags();
				syncSubscriptions();
				syncItemsOfBuiltInTags(timestamp);				
				totalNewItems = syncFeeds(timestamp);
				executeJobs();				
				// clean up database
				if (totalNewItems > 0) {
					ContentManager.cleanUp(Settings.getKeepMaxItems());
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			Log.i(TAG, "Stop synchronization at " + new Date());
		}
		
		synchronized (synRoot) {
			synchronizing = false;
		}
		
		return totalNewItems;
	}
	
	public void stopSynchronizing() {
		synchronized (synRoot) {
			synchronizing = false;			
		}
	}

	protected int syncFeeds(long timestamp) {
		int totalNewItems = 0;
		int maxItemsPerChannel = Settings.getMaxItemsPerChannel();		
		List<Channel> channels = ContentManager.loadAllChannels(ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
		for (Channel channel : channels) {
			synchronized (synRoot) {
				if (!synchronizing) break;
			}

			try {
				int newItems = channel.update(maxItemsPerChannel, timestamp);
				totalNewItems += newItems;
				channel.getItems().clear();
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// clean up memory
		channels.clear();
		channels = null;
				
		return totalNewItems;
	}
	
	private void syncItemsOfBuiltInTags(long timestamp) {		
		try {			
			GoogleReader reader = GoogleReaderFactory.getGoogleReader();
			syncItemsOfTag(reader, GoogleReader.ITEM_STATE_STARRED,
					Constants.MAX_ITEMS_PER_BUILT_IN_TAG, timestamp);
			syncItemsOfTag(reader, GoogleReader.ITEM_STATE_SHARED,
					Constants.MAX_ITEMS_PER_BUILT_IN_TAG, timestamp);
			//syncItemsOfTag(reader, GoogleReader.ITEM_STATE_READING, MAX_ITEMS_PER_SYNC, timestamp);
		} catch (Throwable e) {
			e.printStackTrace();
		}		
	}

	private int syncItemsOfTag(GoogleReader reader, final String tag, int maxItems, 
			final long timestamp) 
		throws Exception {
		int totalNewItems = 0;
		String continution = null;
		while (true) {
			int numberOfFetchedItems = 0;
			AtomFeed feed = reader.fetchEntriesOfTag(tag,
					Constants.MAX_ITEMS_PER_FETCH,
					continution,
					new OnNewEntryCallback() {
						public void onNewEntry(Entry entry) {
							Item item = Item.fromEntry(entry);
							if (ContentManager.existItem(entry.getLink())) {
								ContentManager.saveItemTags(item);
							} else {
								String feedUrl = entry.getFeedUrl();								
								if (channelMap.containsKey(feedUrl)) {
									item.channel = channelMap.get(feedUrl);
									item.kept = true;
									item.updateTime = timestamp;
									ContentManager.saveItem(item);									
								}
							}
						}
					});
			continution = feed.getContinution();
			numberOfFetchedItems += feed.getEntries().size();
			if (numberOfFetchedItems >= maxItems || 
					feed.getEntries().size() < Constants.MAX_ITEMS_PER_FETCH) {
				break;
			}
		}	
		return totalNewItems;
	}
	
	private void executeJobs() {
		while (synchronizing) {
			List<Job> jobs = ContentManager.loadJobs(Constants.JOB_SYNC_BATCH_SIZE);		
			for (Job job : jobs) {
				synchronized (synRoot) {
					if (!synchronizing) break;
				}
				
				try {
					JobExecutor jobExecutor = lookupJobExecutor(job);					
					if (jobExecutor != null) {
						jobExecutor.execute(job);
					}				
					ContentManager.deleteJob(job.id);
				} catch (Throwable t) {
					t.printStackTrace();
				}						
				
				try {
					Thread.yield();
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (jobs.size() < Constants.JOB_SYNC_BATCH_SIZE) {
				return;
			}
		}
	}
	
	private void syncSubscriptions() {
		channelMap = new HashMap<String, Channel>();
		try {
			GoogleReader reader = GoogleReaderFactory.getGoogleReader();
			List<Subscription> subscriptions = reader.getSubscriptions();
			List<Channel> currentChannels = ContentManager.loadAllChannels(ContentManager.LIGHTWEIGHT_CHANNEL_LOADER);
			for (Subscription subscription : subscriptions) {
				Channel channel = ContentManager.loadChannel(subscription.getUrl(), 
							ContentManager.FULL_CHANNEL_LOADER);
				if (channel == null) {
					channel = new Channel(subscription.getUrl());
				} else {
					removeExistingChannel(channel, currentChannels);
				}
				channel.title = subscription.getTitle();				
				ContentManager.saveChannel(channel);
				
				channelMap.put(channel.url, channel);
			}
			// delete remain channels
			for (Channel channel : currentChannels) {
				ContentManager.deleteChannel(channel);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private void removeExistingChannel(Channel channel, List<Channel> currentChannels) {
		for (Channel currentChannel : currentChannels) {
			if (currentChannel.url.equals(channel.url)) {
				currentChannels.remove(currentChannel);
				return;
			}
		}
	}
	
	private Map<String, Tag> syncTags() {
		Map<String, Tag> tagMap = new HashMap<String, Tag>();
		try {
			GoogleReader reader = GoogleReaderFactory.getGoogleReader();
			List<String> readerTags = reader.getTags();
			List<Tag> currentTags = ContentManager.loadAllTags();
			for (String readerTag : readerTags) {
				Tag tag = new Tag(readerTag);
				// only sync GOOGLE STATE and user LABEL tags
				if (tag.type == Tag.OTHER) continue;
				if (!currentTags.contains(tag)) {
					ContentManager.saveTag(tag);
				} else {
					currentTags.remove(tag);
				}
			}
			// delete old tags
			for (Tag tag : currentTags) {
				ContentManager.deleteTag(tag.id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tagMap;
	}
	
	private void registerJobExecutor(JobExecutor jobExecutor) {
		if (!jobExecutors.containsKey(jobExecutor.getExecutableJobType())) {
			jobExecutors.put(jobExecutor.getExecutableJobType(), jobExecutor);
		}
	}
	
	private JobExecutor lookupJobExecutor(Job job) {
		if (jobExecutors.containsKey(job.type)) {
			return jobExecutors.get(job.type);
		}
		return null;
	}
}
