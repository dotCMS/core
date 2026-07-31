package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.MigrationIndexVisibility;
import com.dotcms.content.index.migration.MigrationReadinessService;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.vavr.control.Try;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Internal, role-gated ES→OS migration-readiness endpoint (issue #36360). It condenses the migration
 * status a support technician needs <em>before</em> changing the phase: the current phase and its
 * read/write engines, the per-index ES↔OS mirror diff for both mirrored families (content and Site
 * Search) with missing-counterpart / count-drift verdicts and re-crawl/reindex recommendations, and an
 * overall safe-to-advance / safe-to-rollback verdict. Read-only; it never mutates any index.
 *
 * <p><strong>Not public.</strong> The class is {@link Hidden} so it never appears in the OpenAPI /
 * API-playground schema, and every method requires a backend user who is a CMS administrator or a
 * member of the migration support role
 * ({@value com.dotcms.content.index.MigrationIndexVisibility#VISIBILITY_ROLE_KEY}, default
 * {@value com.dotcms.content.index.MigrationIndexVisibility#DEFAULT_VISIBILITY_ROLE_KEY}); anyone
 * else gets a 403.</p>
 */
@Path("/v1/index/migration")
@Hidden
public class MigrationReadinessResource {

    private final MigrationReadinessService readinessService;

    public MigrationReadinessResource() {
        this(new MigrationReadinessService());
    }

    @VisibleForTesting
    MigrationReadinessResource(final MigrationReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    /**
     * Returns the migration-readiness report for the current phase. Requires a CMS administrator or a
     * member of the configured migration support role.
     */
    @GET
    @JSONP
    @NoCache
    @Hidden
    @Path("/readiness")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response readiness(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .init();

        final User user = initData.getUser();
        if (!isMigrationSupportUser(user)) {
            throw new ForbiddenException(
                    "Migration readiness is restricted to CMS administrators and the migration "
                            + "support role.");
        }

        return Response.ok(new ResponseEntityView<>(readinessService.evaluate())).build();
    }

    /**
     * Whether {@code user} may read the migration-readiness report: a CMS administrator, or a member
     * of the configured support role (same config key as the {@code .os} visibility policy). Unlike
     * {@link MigrationIndexVisibility#canSeeMigrationIndices(User)} this does <em>not</em> open up to
     * everyone in Phase 3 — the report is an internal support tool in every phase.
     */
    private static boolean isMigrationSupportUser(final User user) {
        if (user == null) {
            return false;
        }
        return Try.of(() -> {
            if (APILocator.getUserAPI().isCMSAdmin(user)) {
                return true;
            }
            final String roleKey = Config.getStringProperty(
                    MigrationIndexVisibility.VISIBILITY_ROLE_KEY,
                    MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY);
            if (!UtilMethods.isSet(roleKey)) {
                return false;
            }
            final Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
            return role != null && APILocator.getRoleAPI().doesUserHaveRole(user, role);
        }).getOrElse(false);
    }
}
