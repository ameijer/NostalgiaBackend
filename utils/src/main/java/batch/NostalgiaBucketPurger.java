package batch;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.nostalgia.persistence.model.Video;

public class NostalgiaBucketPurger extends BatchClass implements VideoBatchClass, UserBatchClass, LocationBatchClass{



	@Override
	public String getName() {
		return "nostalgia bucket purger"; 
	}


	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for(JsonDocument orig : input){
			
			if(orig.content().getString("_id") == null || !orig.content().getString("_id").equals(orig.id())){
				orig.content().put("_id", orig.id());
				System.out.println("Fixing _id field on document: " + orig.id());
			}
			
			toSave.add(orig);
		}



		return toSave;
	}

}
