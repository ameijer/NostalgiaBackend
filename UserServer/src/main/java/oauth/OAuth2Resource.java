package oauth;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.nostalgia.PasswordRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.persistence.model.User;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.soap.AddressingFeature.Responses;

@Slf4j
@Path("/oauth2/token")
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2Resource {
	private ImmutableList<String> allowedGrantTypes;
	private AccessTokenRepository accessTokenDAO;
	private UserRepository userDAO;
	private PasswordRepository passRepo;
	private static final Logger logger = LoggerFactory.getLogger(OAuth2Resource.class.getName());
	
	public OAuth2Resource(ImmutableList<String> allowedGrantTypes, AccessTokenRepository accessTokenDAO, UserRepository userDAO, PasswordRepository passRepo) {
		this.allowedGrantTypes = allowedGrantTypes;
		this.accessTokenDAO = accessTokenDAO;
		this.passRepo = passRepo;
		this.userDAO = userDAO;

		logger.info("Constructed OAuth2Resource with grant types: " + allowedGrantTypes.toString());
	}

	

}
