package batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.KnownLocation;

public class LocationJsonGetter extends BatchClass implements LocationBatchClass{

	@Override
	public String getName() {
		return "LocationJsonGetter"; 
	}

	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for(JsonDocument examining : input){
			System.out.println("examining: " + examining.id());
			JsonObject video = examining.content();

			try {
				writeLocationToFileHumanInput(video, examining.id());
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

	final static File parent = new File("locations");

	static {
		parent.mkdirs();
	}

	public static KnownLocation docToLocation(JsonObject obj) {
		String objString = obj.toString();

		KnownLocation knownLoc = null;
		try {
			knownLoc = mapper.readValue( objString , KnownLocation.class );
		} catch (Exception e) {
			System.err.println("error converting location to doc");
			System.exit(1);
		}

		return knownLoc; 
	}

	private void writeLocationToFileHumanInput(JsonObject video, String docid) throws IOException {
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
		KnownLocation me = docToLocation(video); 

		String Json = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(me);

		JSONObject json = new JSONObject(Json); // Convert text to object
		Json = json.toString(4); 

		file.write(Json);
		file.flush();
		file.close();
		return; 
	}
}
