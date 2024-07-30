package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class UserDataFetcher implements DataFetcher<Map<String, String>> {
    @Override
    public Map<String, String> get(final DataFetchingEnvironment environment) throws Exception {
        String var = null;
        String userId = null;
        try {
            final User apiUser = ((DotGraphQLContext) environment.getContext()).getUser();

            userId = getUserId(environment);

            if(UtilMethods.isNotSet(userId)) {
                return Collections.emptyMap();
            }

            Logger.debug(this, "Fetching user for userId: " + userId + " field: " +
                    var + " by user: " + apiUser.getUserId());

            final User user = APILocator.getUserAPI().loadUserById(userId, apiUser, true);

            final Map<String, String> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("email", user.getEmailAddress());

            return userMap;
        } catch(DotSecurityException e) {
            Logger.warn(this, "No permissions to get value for field: '" + var + "'. " + e.getMessage());
            return Collections.emptyMap();
        } catch(NoSuchUserException e ) {
            Logger.warn(this, "No such user: '" + (userId!=null?userId:"N/D") + "'. " + e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    private String getUserId(final DataFetchingEnvironment environment) {
        String userId = null;
        if(environment.getSource() instanceof  Contentlet) {
            userId = ((Contentlet)environment.getSource()).getStringProperty(environment.getField().getName());
        } else if(environment.getSource() instanceof Map) {
            userId = (String) ((Map<Object, Object>)environment.getSource())
                    .get(environment.getField().getName());
        } else if(environment.getSource() instanceof ContainerRaw && environment.getField().getName().equals("modUser")) {
            userId = ((ContainerRaw)environment.getSource()).getContainer().getModUser();
        } else if(environment.getSource() instanceof ContainerRaw && environment.getField().getName().equals("owner")) {
            userId = ((ContainerRaw)environment.getSource()).getContainer().getOwner();
        }
        return userId;
    }
}
