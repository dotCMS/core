package com.dotcms.util.pagination;

import com.dotcms.content.elasticsearch.business.SearchCriteria;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This implementation of the {@link PaginatorOrdered} interface is used to return data related to
 * versions of a specific Contentlet Identifier, as seen in the {@code History} tab in the
 * Contentlet Editor window.
 *
 *
 * @author Jose Castro
 * @since Jul 31st, 2025
 */
public class ContentHistoryPaginator implements PaginatorOrdered<Map<String, Object>> {

    public static final String IDENTIFIER = "identifier";
    public static final String LANGUAGE_ID = "languageId";
    public static final String RESPECT_FRONTEND_ROLES = "respectFrontedRoles";
    public static final String GROUP_BY_LANG = "groupByLang";
    public static final String BRING_OLD_VERSIONS = "bringOldVersions";

    private final ContentletAPI contentletAPI;
    private final LanguageAPI languageAPI;

    /**
     *
     */
    public ContentHistoryPaginator() {
        this.contentletAPI = APILocator.getContentletAPI();
        this.languageAPI = APILocator.getLanguageAPI();
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final String filter,
                                                            final int limit, final int offset,
                                                            final String orderBy,
                                                            final OrderDirection direction,
                                                            final Map<String, Object> extraParams) throws PaginationException {
        final Identifier identifier = Try.of(() -> (Identifier) extraParams.get(IDENTIFIER)).getOrElse(new Identifier());
        final long languageId = Try.of(() -> (long) extraParams.get(LANGUAGE_ID)).getOrElse(-1L);
        final boolean respectFrontendRoles = Try.of(() -> (Boolean) extraParams.get(RESPECT_FRONTEND_ROLES)).getOrElse(false);
        final boolean groupByLang = Try.of(() -> (Boolean) extraParams.get(GROUP_BY_LANG)).getOrElse(false);
        final boolean bringOldVersions = Try.of(() -> (Boolean) extraParams.get(BRING_OLD_VERSIONS)).getOrElse(false);
        final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList<>();
        try {
            final SearchCriteria criteria = new SearchCriteria.Builder()
                    .withIdentifier(identifier)
                    .withLanguageId(languageId)
                    .withUser(user)
                    .withBringOldVersions(bringOldVersions)
                    .withLimit(limit)
                    .withOffset(offset)
                    .withOrderDirection(direction)
                    .withRespectFrontendRoles(respectFrontendRoles)
                    .build();
            final List<Contentlet> contentletVersions = contentletAPI.findAllVersions(criteria);
            if (groupByLang) {
                final Map<String, Object> historyByLang = this.mapHistoryByLang(contentletVersions);
                result.add(historyByLang);
            } else {
                final List<Map<String, Object>> versions = this.mapHistory(contentletVersions);
                result.addAll(versions);
            }
            result.setTotalResults(this.getTotalRecords(identifier, languageId, bringOldVersions, user, respectFrontendRoles));
            return result;
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Failed to return history for Content ID " +
                    "'%s': %s", identifier.getId(), ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Retrieves the total number of records that are handled by this paginator. This number will
     * change in case a Language is specified, as the total will be the number of versions for that
     * specific Language only. If you DO NOT specify a limit for the REST Endpoint, then all records
     * will be returned. However, keep in mind that doing so may cause performance issues.
     *
     * @param identifier           The {@link Identifier} of the Contentlet whose versions will be
     *                             returned.
     * @param languageId           The language ID to filter the versions by.
     * @param bringOldVersions     If true, then all versions will be returned, not only the latest
     *                             for every language.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If true, then the user's frontend roles will be considered when
     *                             retrieving the contentlet versions.
     *
     * @return The total number of records.
     */
    private long getTotalRecords(final Identifier identifier, long languageId,
                                 final boolean bringOldVersions, final User user,
                                 final boolean respectFrontendRoles) {
        final SearchCriteria criteria = new SearchCriteria.Builder()
                .withIdentifier(identifier)
                .withLanguageId(languageId)
                .withBringOldVersions(bringOldVersions)
                .withUser(user)
                .withRespectFrontendRoles(respectFrontendRoles)
                .build();
        return Sneaky.sneak(() -> this.contentletAPI.findAllVersions(criteria).size());
    }

    /**
     * Generates a list of {@link Map} objects that represent the history of the specified
     * contentlet versions. Such versions will be transformed to the appropriate JSON views.
     *
     * @param contentlets The list of contentlet versions.
     *
     * @return A list of {@link Map} objects with the contentlet versions.
     */
    private List<Map<String, Object>> mapHistory(final List<Contentlet> contentlets) {
        return contentlets.stream().map(this::contentletHistoryToMap).collect(Collectors.toList());
    }

    /**
     * Generates a list of {@link Map} objects that represent the history of the specified
     * contentlet versions when they must be grouped by Language. Such versions will be transformed
     * to the appropriate JSON views.
     *
     * @param contentlets The list of contentlet versions.
     *
     * @return A {@link Map} of languages, each containing the list of versions that belong to each
     * of them.
     */
    private Map<String, Object> mapHistoryByLang(final List<Contentlet> contentlets){
        final Map<String, Object> versionsByLang = new HashMap<>();
        final Map<Long, List<Contentlet>> contentByLangMap = contentlets.stream()
                .collect(Collectors.groupingBy(Contentlet::getLanguageId));
        contentByLangMap.forEach((langId, contentletList) -> {
            final Language lang = this.languageAPI.getLanguage(langId);
            final List<Map<String, Object>> asMapList = contentletList.stream()
                    .map(this::contentletHistoryToMap).collect(Collectors.toList());
            versionsByLang.put(lang.toString(), asMapList);
        });
        return versionsByLang;
    }

    /**
     * Takes a {@link Contentlet} and transforms it into the expected JSON view that exposes it in
     * the for of History data.
     *
     * @param contentlet The {@link Contentlet} to be transformed.
     *
     * @return The transformed {@link Map} object.
     */
    private Map<String, Object> contentletHistoryToMap(final Contentlet contentlet) {
        return new DotTransformerBuilder().historyToMapTransformer().content(contentlet).build().toMaps().get(0);
    }

}
