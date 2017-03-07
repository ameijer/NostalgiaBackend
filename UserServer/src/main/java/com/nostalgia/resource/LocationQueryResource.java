package com.nostalgia.resource;

import com.nostalgia.*;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Hex;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nostalgia.ImageDownloaderBase64;
import com.nostalgia.LocationRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.VideoRepository;
import com.nostalgia.client.SynchClient;
import com.nostalgia.persistence.model.*;


@Path("/api/v0/location")
public class LocationQueryResource {


	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(LocationQueryResource.class);

	private final LocationRepository locRepo;



	public LocationQueryResource(LocationRepository locRepo) {
		this.locRepo = locRepo;
		//this.sManager = manager;

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/id")
	@Timed
	public KnownLocation newLocation(@QueryParam("locID") String locationId, @Context HttpServletRequest req) throws Exception{

		if(locationId == null){
			throw new BadRequestException("no location specified to add");

		}

		KnownLocation found = locRepo.findOneById(locationId);
		

		if(found == null){
			resp.sendError(404, "Location with id: " + locationId + " not found");
		}

		return found;

	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/discrete/bbox")
	@Timed
	public List<KnownLocation> withinBbox(@QueryParam("bbox") String bbox, @Context HttpServletRequest req) throws Exception{
		JsonArray bboxArray = null;
		try {
		 bboxArray = JsonArray.fromJson(bbox);

		} catch (Exception e){
			logger.error("error getting bbox out of query param", e);
			throw new BadRequestException("no location specified to add");

		}

		List<KnownLocation> found = locRepo.findDiscreteLocationsInBbox(bboxArray);
		

		if(found == null){
			found = new ArrayList<KnownLocation>();
		}

		return found;

	}
	

}
