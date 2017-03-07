package oauth;


import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Wither;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@Wither
public class AccessToken implements Serializable, Principal {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3569465691710328516L;

	@JsonProperty("access_token_id")
	@NotNull
	private UUID access_token_id;

	@JsonProperty("user_id")
	@NotNull
	private String user_id;

	public AccessToken(){
		super();
	}
	
	public AccessToken(UUID access_token_id, String user_id, long last_access_utc) {
		super();
		this.access_token_id = access_token_id;
		this.user_id = user_id;
		this.last_access_utc = last_access_utc;
	}

	
	public UUID getAccess_token_id() {
		return access_token_id;
	}

	public void setAccess_token_id(UUID access_token_id) {
		this.access_token_id = access_token_id;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public long getLast_access_utc() {
		return last_access_utc;
	}

	public AccessToken setLast_access_utc(long last_access_utc) {
		this.last_access_utc = last_access_utc;
		return this; 
	}

	@JsonProperty("last_access_utc")
	@NotNull
	private long last_access_utc;

	@Override
	public String getName() {

		return access_token_id.toString(); 
	}
}
