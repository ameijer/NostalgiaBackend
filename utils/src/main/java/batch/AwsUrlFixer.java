package batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

public class AwsUrlFixer extends BatchClass implements VideoBatchClass{



	@Override
	public String getName() {
		return "Aws Url Fixer - deletes /data"; 
	}


	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for(JsonDocument orig : input){

			JsonObject video = orig.content();


			String linkToVideo = video.getString("url");

			if(linkToVideo == null){
				linkToVideo = video.getString("mpd");
			}
			
			video.removeKey("mpd");
			video.removeKey("url");

			//if video has /data in paths
			if(linkToVideo != null && linkToVideo.contains("cloudfront.net") && linkToVideo.contains("data/")){

				linkToVideo = linkToVideo.replace("data/", "");
				//fix url
				video.put("url", linkToVideo);
			}
			
			String thumbNails = video.get("thumbNails").toString();
			
			if(thumbNails != null){
				ArrayList<String> fixed = new ArrayList<String>();
				JsonArray thumbs = JsonArray.fromJson(thumbNails);
				
				Iterator<Object> iter = thumbs.iterator();
				
				while(iter.hasNext()){
					Object thumbLink = iter.next();
					
					String asString = thumbLink.toString();
					
					if(asString.contains("cloudfront.net") && asString.contains("data/")){
						asString = asString.replace("data/", "");
						iter.remove();
						fixed.add(asString);
						
					}
				}
				
				for(String str : fixed){
					thumbs.add(str);
				}
				video.put("thumbNails", thumbs);
				
			}
			

			toSave.add(orig);
		}



		return toSave;
	}

}
