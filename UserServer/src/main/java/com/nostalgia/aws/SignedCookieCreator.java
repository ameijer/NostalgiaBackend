package com.nostalgia.aws;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jets3t.service.CloudFrontService;
import org.jets3t.service.security.EncryptionUtil;
import org.jets3t.service.utils.ServiceUtils;

import com.google.common.io.Resources;

public class SignedCookieCreator {

	public final AWSConfig config; 
	
    
	public SignedCookieCreator(AWSConfig config){
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		this.config = config;
	}

	public Map<String, String> generateCookies(String baseUrl, long expirationTime) throws Exception {

		HashMap<String, String> cookies = new HashMap<String, String>();
		
	
		Date expiry = new Date(expirationTime);
		Date minimum = new Date(System.currentTimeMillis() + 3600 * 1000);
		
		if(!expiry.after(minimum)){
			throw new Exception("Invalid date, must expire later in time");
		}
		
		String customPolicy = CloudFrontService.buildPolicyForSignedUrl(baseUrl, expiry, "0.0.0.0/0", null);
		
		
		cookies.put("CloudFront-Policy", ServiceUtils.toBase64(customPolicy.getBytes()));
		byte[] signatureBytes = EncryptionUtil.signWithRsaSha1(getDerPrivateKey(), customPolicy.getBytes("UTF-8"));
		String signature = ServiceUtils.toBase64(signatureBytes).replace('+', '-').replace('=', '_').replace('/', '~');
		
		cookies.put("CloudFront-Signature", signature);

		cookies.put("CloudFront-Key-Pair-Id", config.keyPairId);
		return cookies; 
	}

	// Convert your DER file into a byte array.
	// Signed URLs for a private distribution
	// Note that Java only supports SSL certificates in DER format, 
	// so you will need to convert your PEM-formatted file to DER format. 
	// To do this, you can use openssl:
	// openssl pkcs8 -topk8 -nocrypt -in origin.pem -inform PEM -out new.der 
//	    -outform DER 
	// So the encoder works correctly, you should also add the bouncy castle jar
	// to your project and then add the provider.
	private byte[] getDerPrivateKey() throws FileNotFoundException, IOException {
		URL icon = getClass().getResource("/private.der");
		byte[] defaultEncoded = Resources.toByteArray(icon);
		return defaultEncoded; 
	}




}
