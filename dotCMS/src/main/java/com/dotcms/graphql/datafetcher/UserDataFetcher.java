package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class UserDataFetcher implements DataFetcher<Map<String, String>> {
    @Override
    public Map<String, String> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();

            final User user = (User) (var.equals("owner") ? contentlet.get(var+"User") : contentlet.get(var));

            if(user==null) {
                return Collections.emptyMap();
            }

            final Map<String, String> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("email", user.getEmailAddress());

            return userMap;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}