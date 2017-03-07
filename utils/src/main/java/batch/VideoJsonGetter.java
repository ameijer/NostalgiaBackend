package batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.nostalgia.persistence.model.Video;

public class VideoJsonGetter extends BatchClass implements VideoBatchClass{



	@Override
	public String getName() {
		return "VideoJsonGetter"; 
	}


	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for(JsonDocument examining : input){
			JsonObject video = examining.content();
				
				try {
				writeToFileHumanInput(video, examining.id());
				} catch (Exception e){
					System.err.println("error saving json to file");
					e.printStackTrace();
					System.err.print("Exiting");
					System.exit(1);
				}
				
			toSave.add(examining); 
			
		}
		
		
		return toSave;
	}

	final static File parent = new File("videos");
	static {
		parent.mkdirs();
	}

	private void writeToFileHumanInput(JsonObject video, String docid) throws IOException {
		String id = video.getString("_id");
		if(id == null){
			id = docid;
			video.put("_id", id);
		}

		File output = new File(parent, id + ".json");
		
		if(output.exists()){
			System.out.print("Json file " + output.getName() + " already exists. Overwrite? (y/[n])");
			Scanner scan = new Scanner(System.in);
			String ans = scan.nextLine();
			
			if(ans.contains("y")){
				System.out.println("Overwriting file: " + output.getAbsolutePath());
				FileUtils.forceDelete(output);
			} else {
				System.out.println("skipping file: " + output.getAbsolutePath());
				return; 
			}
			
		}
		
		FileWriter file = new FileWriter(output);
		String Json = video.toString();
		
		JSONObject json = new JSONObject(Json); // Convert text to object
		Json = json.toString(4); 
		
		file.write(Json);
		file.flush();
		file.close();
		return; 
	}
}
