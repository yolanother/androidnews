package vn.evolus.droidreader.model;

import vn.evolus.droidreader.providers.ContentsProvider;
import android.net.Uri;
import android.provider.BaseColumns;

public class Job {
	public long id;
	public String type;
	public String params;
	
	@Override
	public String toString() {
		return new StringBuilder()
			.append("{id: ").append(this.id)
			.append(", type: ").append(this.type)
			.append(", params: ").append(this.params + "}").toString();
	}
	
	public static final class Jobs implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" 
				+ ContentsProvider.AUTHORITY + "/jobs");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.evolus.droidreader.jobs";
		
		public static final String ID = "ID";
		public static final String TYPE = "JOB_TYPE";
		public static final String PARAMS = "PARAMS";
		
		public static final Uri limit(int limit) {
			return Uri.parse("content://" 
					+ ContentsProvider.AUTHORITY + "/jobs/" + limit);
		}
		
		private Jobs() {
		}
	}
}
