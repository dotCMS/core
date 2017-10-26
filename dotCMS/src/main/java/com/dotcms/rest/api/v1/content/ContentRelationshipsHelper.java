package com.dotcms.rest.api.v1.content;

import com.dotcms.rest.RESTParams;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.viewtools.content.util.ContentUtils;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides the utility methods that interact with content relationships in dotCMS. These methods
 * are used by the Content Relationships REST end-point.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 11, 2017
 */
public class ContentRelationshipsHelper implements Serializable {

    private static final long serialVersionUID = -2520775127453216350L;

    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    private static final boolean RESPECT_FE_ROLES = Boolean.TRUE;

    /**
     * Private constructor
     */
    private ContentRelationshipsHelper() {

    }

    /**
     * Provides a singleton instance of the {@link ContentRelationshipsHelper}
     */
    private static class SingletonHolder {
        private static final ContentRelationshipsHelper INSTANCE = new ContentRelationshipsHelper();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return A single instance of this class.
     */
    public static ContentRelationshipsHelper getInstance() {
        return ContentRelationshipsHelper.SingletonHolder.INSTANCE;
    }

    /**
     * Returns a list of contents and its relationships according to a given set of parameters.
     *
     * @param request   The {@link HttpServletRequest} object.
     * @param user      The {@link User} performing this action.
     * @param paramsMap A Map of parameters that will define the search criteria.
     * @return The list of associated contents.
     */
    public List<Contentlet> getRelatedContent(final HttpServletRequest request, final User user,
                                              final Map<String, String> paramsMap) {
        final String query = paramsMap.get(RESTParams.QUERY.getValue());
        final String id = paramsMap.get(RESTParams.ID.getValue());
        String orderBy = paramsMap.get(RESTParams.ORDERBY.getValue());
        final String limitStr = paramsMap.get(RESTParams.LIMIT.getValue());
        final String offsetStr = paramsMap.get(RESTParams.OFFSET.getValue());
        final String inode = paramsMap.get(RESTParams.INODE.getValue());
        orderBy = UtilMethods.isSet(orderBy) ? orderBy : "modDate desc";
        long language = this.languageAPI.getDefaultLanguage().getId();

        if (null != paramsMap.get(RESTParams.LANGUAGE.getValue())) {
            try {
                language = Long.parseLong(paramsMap.get(RESTParams.LANGUAGE.getValue()));
            } catch (Exception e) {
                Logger.warn(this.getClass(), "The specified language ID is invalid. Using the " +
                        "default language ID.");
            }
        }
        // Limit and Offset Parameters Handling, if not passed, using default
        int limit = 10;
        int offset = 0;
        try {
            if (UtilMethods.isSet(limitStr)) {
                limit = Integer.parseInt(limitStr);
            }
        } catch (NumberFormatException e) {
            // Could not parse limit value. Use default value
        }
        try {
            if (UtilMethods.isSet(offsetStr)) {
                offset = Integer.parseInt(offsetStr);
            }
        } catch (NumberFormatException e) {
            // Could not parse offset value. Use default value
        }
        final boolean live = (null == paramsMap.get(RESTParams.LIVE.getValue()) || !"false"
                .equals(paramsMap.get(RESTParams.LIVE.getValue())));
        // Fetching the content using a query if passed or an id
        List<Contentlet> contentlets = new ArrayList<>();
        boolean idPassed = false;
        boolean inodePassed = false;
        boolean queryPassed = false;
        try {
            if (idPassed = UtilMethods.isSet(id)) {
                contentlets.add(this.contentletAPI.findContentletByIdentifier(id, live, language,
                        user, RESPECT_FE_ROLES));
            } else if (inodePassed = UtilMethods.isSet(inode)) {
                contentlets.add(this.contentletAPI.find(inode, user, RESPECT_FE_ROLES));
            } else if (queryPassed = UtilMethods.isSet(query)) {
                final String tmDate = (String) request.getSession().getAttribute("tm_date");
                contentlets = ContentUtils.pull(query, offset, limit, orderBy, user, tmDate);
            }
        } catch (Exception e) {
            if (idPassed) {
                Logger.warn(this, "Can't find Content with Identifier: " + id);
            } else if (queryPassed) {
                Logger.warn(this, "Can't find Content with Inode: " + inode);
            } else if (inodePassed) {
                Logger.warn(this, "Error searching Content : " + e.getMessage());
            }
        }
        return contentlets;
    }

    /**
     * Converts the specified list of {@link Contentlet} objects to JSON format.
     *
     * @param contentlets The list of {@link Contentlet} objects.
     * @return The contents as JSON.
     * @throws JSONException    An error occurred when generating the JSON data.
     * @throws DotDataException An error occurred when accessing the data source.
     */
    public String contentsAsJson(final List<Contentlet> contentlets) throws JSONException,
            DotDataException {
        final JSONArray resultArray = new JSONArray();
        for (Contentlet contentlet : contentlets) {
            final JSONObject jsonObject = toJson(contentlet);
            final ContentletRelationships relationships = this.contentletAPI.getAllRelationships
                    (contentlet);
            for (ContentletRelationships.ContentletRelationshipRecords rel : relationships
                    .getRelationshipsRecords()) {
                JSONArray jsonArray = (JSONArray) jsonObject.optJSONArray(rel.getRelationship()
                        .getTitle());
                if (jsonArray == null) {
                    jsonArray = new JSONArray();
                }
                for (Contentlet relatedContent : rel.getRecords()) {
                    jsonArray.add(toJson(relatedContent));
                }
                jsonObject.put(rel.getRelationship().getRelationTypeValue(), jsonArray);
            }
            resultArray.add(jsonObject);
        }
        return resultArray.toString();
    }

    /**
     * Converts the specified {@link Contentlet} to JSON format.
     *
     * @param contentlet The {@link Contentlet} object.
     * @return The content as JSON.
     * @throws JSONException    An error occurred when generating the JSON data.
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private JSONObject toJson(final Contentlet contentlet) throws DotDataException, JSONException {
        final JSONObject jsonObject = new JSONObject();
        for (String key : contentlet.getMap().keySet()) {
            jsonObject.put(key, contentlet.getMap().get(key));
        }
        return jsonObject;
    }

}
