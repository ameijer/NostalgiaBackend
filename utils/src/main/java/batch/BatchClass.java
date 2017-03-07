package batch;

import java.util.Collection;
import java.util.Set;

import com.couchbase.client.java.document.JsonDocument;

public abstract class BatchClass {
	public abstract String getName();
	public abstract Set<JsonDocument> execute(Collection<JsonDocument> input);
}
