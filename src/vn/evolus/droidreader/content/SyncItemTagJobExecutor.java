package vn.evolus.droidreader.content;

import vn.evolus.droidreader.model.Job;
import vn.evolus.droidreader.model.SyncItemTagJob;

import com.google.reader.GoogleReader;

public class SyncItemTagJobExecutor implements JobExecutor {
	private GoogleReader reader;	
	public SyncItemTagJobExecutor(GoogleReader reader) {
		this.reader = reader;
	}
	
	@Override
	public String getExecutableJobType() {
		return SyncItemTagJob.JOB_TYPE;
	}
	@Override
	public void execute(Job job) throws Exception {
		SyncItemTagJob syncJob = new SyncItemTagJob(job);		
		if (syncJob.originalItemId == null || syncJob.tag == null) return;
		
		if (syncJob.action == SyncItemTagJob.ACTION_ADD) {			
			reader.addTagToItem(syncJob.originalItemId, syncJob.tag);
		} else {			
			reader.removeTagFromItem(syncJob.originalItemId, syncJob.tag);
		}
	}
}
