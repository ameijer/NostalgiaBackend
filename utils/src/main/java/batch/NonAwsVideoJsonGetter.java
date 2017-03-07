package batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.nostalgia.persistence.model.Video;

public class NonAwsVideoJsonGetter extends BatchClass implements VideoBatchClass{



	@Override
	public String getName() {
		return "Non-AwsVideoJsonGetter"; 
	}


	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for(JsonDocument examining : input){
			JsonObject video = examining.content();


			String linkToVideo = video.getString("url");

			if(linkToVideo == null){
				linkToVideo = video.getString("mpd");
			}
			
			 
			if( video.containsKey("mpd") || video.containsKey("url")){
				//write video object to metadata folder
				if(linkToVideo == null){
					linkToVideo = "null";
				}
			}

			if(!linkToVideo.contains("cloudfront.net")){
				//if video is not on amazon
				linkToVideo = null;
				//scrub thumbs
				video.removeKey("thumbNail");
				video.removeKey("thumbNails");
				video.removeKey("mpd");
				
				video.put("url", linkToVideo);
				
				String nullThumbs = null;
				video.put("thumbNails", nullThumbs);
				
				try {
				writeToFile(video, examining.id());
				} catch (Exception e){
					System.err.println("error saving json to file");
					e.printStackTrace();
					System.err.print("Exiting");
					System.exit(1);
				}
				
			} else {
				//else, just add video to output, so that it is saved
				toSave.add(examining);
			}

			
		}
		return toSave;
	}

	final static File parent = new File("outputJson");
	static {
		parent.mkdirs();
	}

	private void writeToFile(JsonObject video, String docid) throws IOException {
		String id = video.getString("_id");
		if(id == null){
			id = docid;
			video.put("_id", id);
		}

		File output = new File(parent, id + ".json");
		
		
		FileWriter file = new FileWriter(output);
		String Json = video.toString();
		
		JSONObject json = new JSONObject(Json); // Convert text to object
		Json = json.toString(4); 
		
		file.write(Json);
		file.flush();
		file.close();
	}
}
