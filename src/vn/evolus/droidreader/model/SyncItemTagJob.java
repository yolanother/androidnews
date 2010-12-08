package vn.evolus.droidreader.model;


public class SyncItemTagJob extends Job {
	public static final String JOB_TYPE = "SYNC_ITEM_TAG_JOB";	
	public static final int ACTION_ADD = 1;
	public static final int ACTION_REMOVE = 2;
		
	public int action;
	public String originalItemId;
	public String tag;
			
	public SyncItemTagJob(int action, String originalItemId, String tag) {
		this.action = action;
		this.originalItemId = originalItemId;
		this.tag = tag;
		
		this.type = JOB_TYPE;
		this.params = buildParams();
	}	

	public SyncItemTagJob(Job job) {
		this.id = job.id;
		this.type = job.type;
		this.params = job.params;
		
		parseParams();
	}		

	private void parseParams() {
		String[] values = this.params.split("\\|");		
		this.action = Integer.parseInt(values[0]);
		this.originalItemId = values[1];
		this.tag = values[2];
	}

	private String buildParams() {
		return String.valueOf(action) + "|" + originalItemId + "|" + tag;
	}		
}
