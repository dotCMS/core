/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.model.ImmutableContentletSearch;
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

import static com.dotcms.content.elasticsearch.business.ContentFactoryIndexOperationsES.addBuilderSort;
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

	/** Why the deprecated Elasticsearch-only path refuses to run in Phase 3, and what to do instead. */
	static final String PHASE3_UNSUPPORTED_MESSAGE = "The deprecated Elasticsearch-only "
			+ "esSearch()/esRaw() path is not available once the OpenSearch migration reaches its final "
			+ "phase: Elasticsearch no longer receives writes, so it could only answer from the index "
			+ "frozen at cutover. Migrate this call to the vendor-neutral $estool.search()/$estool.raw() "
			+ "(or SearchAPI), which reads from OpenSearch. While templates are being migrated, setting "
			+ "FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL=true makes these calls return null "
			+ "from Velocity instead of failing the page.";

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
				list.add(ImmutableContentletSearch.builder()
						.inode(sourceMap.get("inode").toString())
						.build());
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

        // Normalize the query the same way esSearch() does, so the raw path resolves mixed-case
        // field names (e.g. "contentType" -> the physical lower-case index field "contenttype").
        // Reuses the existing lowercasing helper for parity with esSearch(); idempotent when the
        // caller already lowercased (esSearch delegates here after lowercasing).
        esQuery = StringUtils.lowercaseStringExceptMatchingTokens(
                esQuery, ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX);

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
	 * @throws DotStateException
	 *             In Phase 3 of the OpenSearch migration, where Elasticsearch no longer receives
	 *             writes, or when no Elasticsearch content index is registered.
	 */
	@RequestCost(Price.ES_QUERY)
	private SearchResponse esSearchRaw(JSONObject jsonObject, boolean live, User user,
            boolean respectFrontendRoles, int limit, int offset, String sortBy)
			throws DotSecurityException, DotDataException {

		// Past the final migration phase Elasticsearch receives no more writes. The switchover keeps
		// its active pointers, so this path would still resolve an index — the copy frozen at cutover
		// — and answer with results that look valid but miss everything written since, and still
		// include everything deleted or unpublished since, with no error anywhere (issue #37635).
		// Fail instead, and do it on the phase rather than on a missing pointer so the outcome does
		// not depend on what the index store happens to hold. DotStateException is one of the few
		// exception types Velocity's method-exception handler rethrows rather than turning into a null, so from
		// a template the failure surfaces instead of printing unresolved Velocity onto the page.
		// Templates can soften this to a null while they are migrated — see
		// FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_LEGACY_ES_SEARCH_RETURNS_NULL and ESContentTool;
		// Java callers always get the exception.
		if (IndexConfigHelper.MigrationPhase.current().isMigrationComplete()) {
			throw new DotStateException(PHASE3_UNSUPPORTED_MESSAGE);
		}

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

		// Before Phase 3 the pointers should always be set; if the store holds none, carrying the null
		// into SearchRequest dies there with a NullPointerException, which Velocity turns into a null
		// return and the template renders its own unresolved source (issue #37635). Fail clearly.
		if (!UtilMethods.isSet(indexToHit)) {
			throw new DotStateException(String.format(
					"No active Elasticsearch %s content index is registered, so the deprecated "
							+ "esSearch()/esRaw() path has nothing to query. Reindex, or migrate this "
							+ "call to the vendor-neutral $estool.search()/$estool.raw() (or SearchAPI), "
							+ "which resolves the index for the current migration phase.",
					live ? "live" : "working"));
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
                addBuilderSort(sortBy, searchSourceBuilder);
			}

            request.source(searchSourceBuilder);
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new DotStateException(e.getMessage(), e);
        }

        return response;

    }
}
