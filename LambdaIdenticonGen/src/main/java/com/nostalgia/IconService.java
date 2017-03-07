package com.nostalgia;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


import org.apache.commons.codec.binary.Base64;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.nostalgia.IconServiceConfig;
import com.nostalgia.identicon.IdenticonUtil;
import com.nostalgia.persistence.model.icon.IconReply;



public class IconService {

	private final IconServiceConfig conf;

	private static final ObjectMapper mapper = new ObjectMapper();
	IdenticonUtil generator = new IdenticonUtil();
	
	public IconService(IconServiceConfig conf){
		this.conf = conf;
	
	}
	
	
	
	public String getBase64Icon(String key) throws Exception{
		byte[] result = generator.makeIdenticon(key);
		return  new String(Base64.encodeBase64(result, false, false));
	}
	
	
}
