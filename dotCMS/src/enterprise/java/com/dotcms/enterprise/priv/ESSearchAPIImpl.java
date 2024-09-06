/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.priv;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

/**
 * Implementation class for the {@link ESSeachAPI}.
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 7, 2017
 *
 */
public class ESSearchAPIImpl implements ESSeachAPI {

	@Override
	public ESSearchResults esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {
		esQuery = (esQuery != null) ? StringUtils.lowercaseStringExceptMatchingTokens(esQuery,ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX) : esQuery;
		StringBuilder rewrittenESQuery = new StringBuilder(esQuery);
		SearchResponse resp = esSearchRaw(esQuery, live, user, respectFrontendRoles);
		ESSearchResults contents = new ESSearchResults(resp, new ArrayList());
		contents.setQuery(esQuery);
		contents.setRewrittenQuery(rewrittenESQuery.toString());

		List<ContentletSearch> list = new ArrayList<>();
		if (contents.getHits() == null) {
			return contents;
		}

		long start = System.currentTimeMillis();

		for (SearchHit sh : contents.getHits()) {
			try {
				Map<String, Object> sourceMap = sh.getSourceAsMap();
				ContentletSearch conwrapper = new ContentletSearch();
				conwrapper.setInode(sourceMap.get("inode").toString());
				list.add(conwrapper);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
		ArrayList<String> inodes = new ArrayList<>();
		for (ContentletSearch conwrap : list) {
			inodes.add(conwrap.getInode());
		}

		List<Contentlet> contentlets = APILocator.getContentletAPIImpl().findContentlets(inodes);
		for (Contentlet contentlet : contentlets) {
			if (contentlet.getInode() !=null) {
				contents.add(contentlet);
			}
		}

		contents.setPopulationTook(System.currentTimeMillis() - start);
		return contents;
	}

	@Override
	public SearchResponse esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(esQuery)) {
            throw new DotStateException("ES Query is null");
        }

        JSONObject completeQueryJSON;

        try{
            //Parsing the given ES query
            completeQueryJSON = new JSONObject(esQuery);
            completeQueryJSON.put("_source", new JSONArray( "[identifier, inode]" ));
        } catch (JSONException e) {
            throw new DotStateException("Unable to parse the given query.", e);
        }

        return esSearchRaw(completeQueryJSON, live, user, respectFrontendRoles, -1, -1, null);
	}

    @Override
    public SearchResponse esSearchRelated(final String contentletIdentifier,
            final String relationshipName, final boolean pullParents, final boolean live,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

	    final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletIdentifier);
        return esSearchRelated(contentlet, relationshipName, pullParents, false, user,
                respectFrontendRoles, -1, -1, null);
    }

    @Override
    public SearchResponse esSearchRelated(final Contentlet contentlet,
            final String relationshipName, final boolean pullParents, final boolean live,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        return esSearchRelated(contentlet, relationshipName, pullParents, false, user,
                respectFrontendRoles, -1, -1, null);
    }

    @Override
    public SearchResponse esSearchRelated(final String contentletIdentifier,
            final String relationshipName, final boolean pullParents, final boolean live,
            final User user, final boolean respectFrontendRoles, int limit, int offset, String sortBy)
            throws DotDataException, DotSecurityException {

        final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletIdentifier);
	    return esSearchRelated(contentlet, relationshipName, pullParents, live, user, respectFrontendRoles, limit, offset,
                sortBy);
    }

    @Override
    public SearchResponse esSearchRelated(final Contentlet contentlet,
            final String relationshipName, final boolean pullParents, final boolean live,
            final User user, final boolean respectFrontendRoles, int limit, int offset, String sortBy)
            throws DotDataException, DotSecurityException {

        final JSONObject criteriaMap = new JSONObject();
        final JSONObject completeQueryJSON;

        try {
            if (pullParents) {
                criteriaMap.put("_source", "identifier");
                criteriaMap.put("query", new JSONObject().put("match",
                        Map.of(relationshipName.toLowerCase(), contentlet.getIdentifier())));
            } else {
                criteriaMap.put("_source", relationshipName.toLowerCase());
                criteriaMap.put("query", new JSONObject().put("match", Map.of("inode", contentlet.getInode())));
            }
            completeQueryJSON = new JSONObject(criteriaMap.toString());
        } catch (JSONException e) {
            throw new DotStateException("Unable to parse the given query.", e);
        }
        return esSearchRaw(completeQueryJSON, false, user, respectFrontendRoles, limit, offset, sortBy);
    }

	/**
	 * Returns the list of inodes as hits, and does not load the contentlets
	 * from cache.
	 *
	 * @param jsonObject
	 *            - JSON object with the query to be executed.
	 * @param live
	 *            - If {@code true}, only live content will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param respectFrontendRoles
	 *            -
	 * @return The result object.
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform this action.
	 * @throws DotDataException
	 *             An error occurred when retrieving the data.
	 */
	private SearchResponse esSearchRaw(JSONObject jsonObject, boolean live, User user,
            boolean respectFrontendRoles, int limit, int offset, String sortBy)
			throws DotSecurityException, DotDataException {

        String indexToHit;
        IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
			if (live) {
                indexToHit = info.getLive();
			} else {
                indexToHit = info.getWorking();
			}
		} catch (DotDataException ee) {
			Logger.fatal(this, "Can't get indicies information", ee);
			return null;
		}

        List<Role> roles = new ArrayList<>();
		if (user == null && !respectFrontendRoles) {
			throw new DotSecurityException("You must specify a user if you are not respecting frontend roles");
		}

        boolean isAdmin = false;
		if (user != null) {
			if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
				roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            } else {
                isAdmin = true;
            }
        }

        final RestHighLevelClient client = RestHighLevelClientProvider.getInstance().getClient();
        final SearchRequest request = new SearchRequest(indexToHit);

        /*
        Getting the permissions query but only for non admin users
        and if the user is not already passing permissions on the query.
         */
		StringBuffer perms = new StringBuffer();
		if (!isAdmin && !jsonObject.has("permissions:")) {
			APILocator.getContentletAPIImpl().addPermissionsToQuery(perms, user, roles, respectFrontendRoles);
		}

        //Verify if we have permissions to apply
		if (perms.length() > 0) {
            try {

                //Generating our permissions query in order to be use in the Bool filter
				JSONObject permissionsFilter = new JSONObject().put("query_string",
						new JSONObject().put("query", perms.toString().trim()));
                //Creating a Bool filter with our permissions query
                JSONArray boolFilters = new JSONArray( "[" + permissionsFilter + "]" );

                /*
                Verify if the user sent a query attribute inside the ES search query,
                If a query attribute is found we will concat the query sent by the user with our permissions query
                into a Bool filter.
                 */
				if (jsonObject.has("query")) {
					JSONObject queryJson = jsonObject.getJSONObject("query");
                    String currentQuery = queryJson.toString();
                    //Query sent by the user
                    JSONObject currentQueryJSON = new JSONObject( currentQuery );
                    //Concatenating our permissions query with the query given to the user in a JSON array in order to use it in the Bool filter
					boolFilters = new JSONArray("[" + permissionsFilter + "," + currentQueryJSON + "]");

                    /*
                     EXAMPLE OF THE QUERY AFTER ADDED PERMISSIONS

                        {
                          "query": {
                            "filtered": {
                              "query": {
                                "bool": {
                                  "must": [
                                    {

                                      //ON THIS SEGMENT WE WILL ADD THE QUERY SENT BY THE USER
                                      "bool": {
                                        "must": {
                                          "term": {
                                            "catchall": "gas"
                                          }
                                        }
                                      }
                                      //ON THIS SEGMENT WE WILL ADD THE QUERY SENT BY THE USER

                                    }
                                    ,
                                    {

                                      //OURS PERMISSIONS QUERY
                                      "query_string": {
                                        "query": "+((+owner:anonymous +ownerCanRead:true) (permissions:Pedecd377-2321-4803-aa8b-89797dd0d61f.1P* permissions:P654b0931-1027-41f7-ad4d-173115ed8ec1.1P* ) (permissions:P654b0931-1027-41f7-ad4d-173115ed8ec1.1P*))"
                                      }
                                      //OURS PERMISSIONS QUERY

                                    }
                                  ]
                                }
                              }
                            }
                          },
                          "aggs": {
                            "tags": {
                              "terms": {
                                "field": "news.tags"
                              }
                            }
                          },
                          "fields": [
                            "inode",
                            "identifier"
                          ]
                        }
                     */
                }

                //Building a Bool filter in order to combine the query sent by the user and our permissions query.
                JSONObject filteredJSON = new JSONObject().
						put("bool", new JSONObject().put("must",
								new JSONObject().put("bool", new JSONObject().put("must", boolFilters))));

                //Replacing the original given query with our modified version that includes permissions
				jsonObject.put("query", filteredJSON);

			} catch (JSONException e) {
				throw new DotStateException("Unable to parse the given query.", e);
			}
		}

        final SearchResponse response;
        try {
			final SearchSourceBuilder searchSourceBuilder =  SearchSourceBuilderUtil
					.getSearchSourceBuilder(jsonObject.toString())
					.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

			if(limit>0)
				searchSourceBuilder.size(limit);
			if(offset>0)
				searchSourceBuilder.from(offset);

			if(UtilMethods.isSet(sortBy) ) {
				ESContentFactoryImpl.addBuilderSort(sortBy, searchSourceBuilder);
			}

            request.source(searchSourceBuilder);
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DotStateException(e.getMessage(), e);
        }

        return response;

    }
}
