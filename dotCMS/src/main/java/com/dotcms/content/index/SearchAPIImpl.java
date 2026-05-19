package com.dotcms.content.index;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.content.index.elasticsearch.ESSearchAPIImpl;
import com.dotcms.content.index.opensearch.OSSearchAPIImpl;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * Phase-aware router implementation of {@link SearchAPI}.
 *
 * <h2>Routing table</h2>
 * <pre>
 * Phase                     | Read provider
 * --------------------------|---------------
 * 0 — not started           | ES
 * 1 — dual-write, ES reads  | ES
 * 2 — dual-write, OS reads  | OS  (with ES fallback on failure)
 * 3 — OS only               | OS
 * </pre>
 *
 * <p>All search operations are reads; there are no write operations in this API, so the
 * router only delegates to the read-path provider determined by {@link PhaseRouter#read}.</p>
 *
 * @see PhaseRouter
 * @see ESSearchAPIImpl
 * @see OSSearchAPIImpl
 */
@IndexLibraryIndependent
public class SearchAPIImpl implements SearchAPI {

    private final ESSearchAPIImpl esImpl;
    private final OSSearchAPIImpl osImpl;
    private final PhaseRouter<SearchAPI> router;

    public SearchAPIImpl() {
        this(CDIUtils.getBeanThrows(ESSearchAPIImpl.class),
                CDIUtils.getBeanThrows(OSSearchAPIImpl.class));
    }

    /** Package-private constructor for testing. */
    SearchAPIImpl(final ESSearchAPIImpl esImpl, final OSSearchAPIImpl osImpl) {
        this.esImpl = esImpl;
        this.osImpl = osImpl;
        this.router = new PhaseRouter<>(esImpl, osImpl);
    }

    /** Direct access to the ES implementation (for testing / bootstrap checks). */
    public ESSearchAPIImpl esImpl() {
        return esImpl;
    }

    /** Direct access to the OS implementation (for testing / bootstrap checks). */
    public OSSearchAPIImpl osImpl() {
        return osImpl;
    }

    // -------------------------------------------------------------------------
    // SearchAPI — all operations are reads; delegate via router.readChecked
    // -------------------------------------------------------------------------

    @Override
    public ContentSearchResults<Contentlet> search(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {
        try {
            return router.readChecked(impl ->
                    impl.search(query, live, user, respectFrontendRoles));
        } catch (final DotSecurityException | DotDataException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public ContentSearchResponse searchRaw(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {
        try {
            return router.readChecked(impl ->
                    impl.searchRaw(query, live, user, respectFrontendRoles));
        } catch (final DotSecurityException | DotDataException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public ContentSearchResponse searchRelated(
            final String contentletIdentifier,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        try {
            return router.readChecked(impl -> impl.searchRelated(
                    contentletIdentifier, relationshipName, pullParents,
                    live, user, respectFrontendRoles));
        } catch (final DotDataException | DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public ContentSearchResponse searchRelated(
            final Contentlet contentlet,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        try {
            return router.readChecked(impl -> impl.searchRelated(
                    contentlet, relationshipName, pullParents,
                    live, user, respectFrontendRoles));
        } catch (final DotDataException | DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public ContentSearchResponse searchRelated(
            final String contentletIdentifier,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotDataException, DotSecurityException {
        try {
            return router.readChecked(impl -> impl.searchRelated(
                    contentletIdentifier, relationshipName, pullParents,
                    live, user, respectFrontendRoles, limit, offset, sortBy));
        } catch (final DotDataException | DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public ContentSearchResponse searchRelated(
            final Contentlet contentlet,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotDataException, DotSecurityException {
        try {
            return router.readChecked(impl -> impl.searchRelated(
                    contentlet, relationshipName, pullParents,
                    live, user, respectFrontendRoles, limit, offset, sortBy));
        } catch (final DotDataException | DotSecurityException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
