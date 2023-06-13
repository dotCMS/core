package com.dotcms.enterprise.priv;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotcms.repackage.org.json.JSONArray;
import com.dotcms.repackage.org.json.JSONException;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

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
		esQuery = (esQuery != null) ? esQuery.toLowerCase() : esQuery;
		StringBuilder rewrittenESQuery = new StringBuilder(esQuery);
		SearchResponse resp = esSearchRaw(rewrittenESQuery, live, user, respectFrontendRoles);
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
		return esSearchRaw(new StringBuilder(esQuery), live, user, respectFrontendRoles);
	}

	/**
	 * Returns the list of inodes as hits, and does not load the contentlets
	 * from cache.
	 *
	 * @param esQuery
	 *            - The query that will be executed.
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
	private SearchResponse esSearchRaw(StringBuilder esQuery, boolean live, User user, boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {
		if (!UtilMethods.isSet(esQuery)) {
			throw new DotStateException("ES Query is null");
		}

		JSONObject completeQueryJSON;
        String originalESQuery = esQuery.toString();

		try{
			//Parsing the given ES query
			completeQueryJSON = new JSONObject(originalESQuery);
			completeQueryJSON.put("_source", new JSONArray( "[identifier, inode]" ));
		} catch (JSONException e) {
			throw new DotStateException("Unable to parse the given query.", e);
		}

        String indexToHit;
        IndiciesAPI.IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
			if (live) {
                indexToHit = info.live;
			} else {
                indexToHit = info.working;
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

        Client client = new ESClient().getClient();
        SearchRequestBuilder srb = client.prepareSearch( indexToHit );

        /*
        Getting the permissions query but only for non admin users
        and if the user is not already passing permissions on the query.
         */
		StringBuffer perms = new StringBuffer();
		if (!isAdmin && !originalESQuery.contains("permissions:")) {
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
				if (completeQueryJSON.has("query")) {
					JSONObject queryJson = completeQueryJSON.getJSONObject("query");
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
				completeQueryJSON.put("query", filteredJSON);

			} catch (JSONException e) {
				throw new DotStateException("Unable to parse the given query.", e);
			}
		}

		esQuery.delete(0, esQuery.length());
		esQuery.append(completeQueryJSON.toString());

		try{
			srb.setSource(SearchSourceBuilderUtil.getSearchSourceBuilder(esQuery.toString()));
		} catch (IOException e){
			throw new DotStateException(e.getMessage(), e);
		}
        return srb.execute().actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS);
    }
}
