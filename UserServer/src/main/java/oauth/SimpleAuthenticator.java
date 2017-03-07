package oauth;

import com.google.common.base.Optional;
import com.nostalgia.UserRepository;
import com.nostalgia.persistence.model.User;
import com.nostalgia.resource.PasswordResource;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimpleAuthenticator implements Authenticator<String, AccessToken> {
	public static final int ACCESS_TOKEN_EXPIRE_TIME_MIN = 3000;
	
	private AccessTokenRepository accessTokenDAO;
	private static final Logger logger = LoggerFactory.getLogger(SimpleAuthenticator.class);
	
	
	public SimpleAuthenticator(AccessTokenRepository accessTokenDAO) {
		super();
		this.accessTokenDAO = accessTokenDAO;
	
	}



	@Override
	public Optional<AccessToken> authenticate(String accessTokenId) throws AuthenticationException {
		// Check input, must be a valid UUID
		logger.info("accesstokenid being considered: " + accessTokenId); 
		UUID accessTokenUUID;
		try {
			accessTokenUUID = UUID.fromString(accessTokenId);
		} catch (IllegalArgumentException e) {
			return Optional.absent();
		}

		// Get the access token from the database
		Optional<AccessToken> accessToken = accessTokenDAO.findAccessTokenById(accessTokenUUID);
		if (accessToken == null || !accessToken.isPresent()) {
			return Optional.absent();
		}

		// Check if the last access time is not too far in the past (the access token is expired)
		Period period = new Period(accessToken.get().getLast_access_utc(), System.currentTimeMillis());
		if (period.getMinutes() > ACCESS_TOKEN_EXPIRE_TIME_MIN) {
			return Optional.absent();
		}

		// Update the access time for the token
		accessTokenDAO.setLastAccessTime(accessTokenUUID, System.currentTimeMillis());
		
		return accessToken; 
	}
}
