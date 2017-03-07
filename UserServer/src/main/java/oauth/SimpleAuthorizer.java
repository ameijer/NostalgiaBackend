package oauth;

import com.nostalgia.persistence.model.User;

import io.dropwizard.auth.Authorizer;

public class SimpleAuthorizer implements Authorizer<AccessToken> {

    @Override
    public boolean authorize(AccessToken tok, String role) {
 
        return true; 
    }
}