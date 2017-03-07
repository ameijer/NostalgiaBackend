package oauth;

import java.security.Principal;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.nostalgia.UserRepository;
import com.nostalgia.persistence.model.User;

import io.dropwizard.auth.Auth;

@Path("/test/auth/")
@Produces(MediaType.TEXT_PLAIN)
public class AuthResource {

	private final UserRepository userRepo; 
	
	public AuthResource(UserRepository userRepo){
		this.userRepo = userRepo;
	}
	
	
    @RolesAllowed({"ADMIN"})
    @GET
    @Path("admin")
    public String show(@Auth AccessToken principal) {
    	User matching = userRepo.findOneByOAuthToken(principal.getAccess_token_id().toString());
        return "Hello. User '" + matching.getUsername() + "' has admin privileges";
    }

    @PermitAll
    @POST
    @Path("profile")
    public String showForEveryUserPost(@Auth AccessToken principal, String body) {
    	User matching = userRepo.findOneByOAuthToken(principal.getAccess_token_id().toString());
        return "Hello. User '" + matching.getUsername() +  "' posted wit user privileges. Post body sent: " + body;
    }
    
    @PermitAll
    @GET
    @Path("params/profile")
    public String showForEveryUserGet(@Auth AccessToken principal, @QueryParam("testval") String testVal) {
    	User matching = userRepo.findOneByOAuthToken(principal.getAccess_token_id().toString());
        return "Hello. User '" + matching.getUsername() +  "' has did a get with privileges, sent query testVal: " + testVal;
    }
    
    @PermitAll
    @GET
    @Path("profile")
    public String showForEveryUserGetEmpty(@Auth AccessToken principal) {
    	User matching = userRepo.findOneByOAuthToken(principal.getAccess_token_id().toString());
        return "Hello. User '" + matching.getUsername() +  "' has did a get with privileges, empty get.";
    }

    @GET
    @Path("implicit-permitall")
    public String implicitPermitAllAuthorization(@Auth AccessToken principal) {
    	User matching = userRepo.findOneByOAuthToken(principal.getAccess_token_id().toString());
        return "Hello. User '" + matching.getUsername() + "' has user privileges";
    }

    @GET
    @Path("noauth")
    public String hello() {
        return "hello";
    }

    @DenyAll
    @GET
    @Path("denied")
    public String denied() {
        return "denied";
    }
}