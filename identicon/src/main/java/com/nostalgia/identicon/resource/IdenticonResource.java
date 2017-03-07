package com.nostalgia.identicon.resource;


import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.nostalgia.identicon.IdenticonRenderer;
import com.nostalgia.identicon.IdenticonUtil;
import com.nostalgia.identicon.NineBlockIdenticonRenderer2;
import com.nostalgia.identicon.cache.IdenticonCache;
import com.nostalgia.identicon.config.IconGeneratorConfig;
import com.nostalgia.persistence.model.icon.IconReply;

@Path("/api/v0/icongen")
public class IdenticonResource {


	IdenticonUtil generator; 

	private static final Logger logger = LoggerFactory.getLogger(IdenticonResource.class);

	public IdenticonResource(IconGeneratorConfig conf) {
		generator = new IdenticonUtil(); 
	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/newicon")
	@Timed
	public IconReply generateIdenticon(String seed, @QueryParam("key") String key, @Context HttpServletRequest req) throws Exception{

		IconReply reply = new IconReply();

		if(key == null){
			throw new ForbiddenException();
		}

		byte[] encodedImg = generator.makeIdenticon(seed);

		reply.setEncodedImage(encodedImg);
		return reply;


	}
	

	
	

	


}
