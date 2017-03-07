package utils; 

/**
 * Created by alex on 11/7/15.
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.LocationUpdate;
import com.nostalgia.persistence.model.LoginResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.geojson.GeoJsonObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class KnownLocationCreatorThread extends Thread {
    private static final String TAG = "KnownLocationCreator";
    private final KnownLocation location;
    String server = "REDACTED";
    int port = 10004;
    private KnownLocation added;

    public KnownLocationCreatorThread(KnownLocation toCreate) {
        this.location = toCreate;
    }

    public KnownLocation getAdded(){
        return added;
    }

    public void run() {

        ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println(mapper.writeValueAsString(location));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HttpPost httpPost = null;

        httpPost = new HttpPost(server + ":" + port + "/api/v0/admin/location/new");


        try {
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(location)));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");


        HttpResponse resp = null;
        try {
            resp = new DefaultHttpClient().execute(httpPost);

        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String contents = null;
        try {
            contents = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");

        added = mapper.readValue(contents, KnownLocation.class);

        } catch (Exception e) {
            e.printStackTrace();
           System.err.println(TAG + "Error reading in created location object: " + e);
        }

    }
}



