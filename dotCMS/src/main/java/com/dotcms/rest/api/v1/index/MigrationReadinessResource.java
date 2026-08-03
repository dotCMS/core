package com.dotcms.rest.api.v1.index;

import com.dotcms.content.index.MigrationIndexVisibility;
import com.dotcms.content.index.migration.MigrationReadinessService;
import com.dotcms.rest.InitDataObject;
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
 * API-playground schema, and every method requires a backend user who is a CMS administrator
 * <strong>and</strong> a member of the migration support role
 * ({@value com.dotcms.content.index.MigrationIndexVisibility#VISIBILITY_ROLE_KEY}, default
 * {@value com.dotcms.content.index.MigrationIndexVisibility#DEFAULT_VISIBILITY_ROLE_KEY}) — a plain
 * admin without the role is not enough. Anyone else gets a 403, so regular users never learn a
 * migration is running.</p>
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
     * Returns the migration-readiness report for the current phase. Requires a CMS administrator who
     * also holds the configured migration support role.
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
                    "Migration readiness is restricted to CMS administrators who also hold the "
                            + "migration support role.");
        }

        // Return the readiness model directly (no ResponseEntityView envelope) — this internal endpoint
        // has no use for the errors/messages/pagination/permissions wrapper.
        return Response.ok(readinessService.evaluate()).build();
    }

    /**
     * Whether {@code user} may read the migration-readiness report: a CMS administrator who
     * <strong>also</strong> holds the configured support role
     * ({@link MigrationIndexVisibility#VISIBILITY_ROLE_KEY}, default
     * {@link MigrationIndexVisibility#DEFAULT_VISIBILITY_ROLE_KEY}) — both are required. This is an
     * internal support tool in every phase; it never opens up to everyone (a plain admin is not
     * enough), unlike the phase-based index portlet display.
     */
    @VisibleForTesting
    static boolean isMigrationSupportUser(final User user) {
        if (user == null) {
            return false;
        }
        return Try.of(() -> {
            // Must be BOTH a CMS administrator AND a member of the migration support role — a plain
            // admin without the role is not enough, and the role without admin is not enough, so a
            // regular user never accesses or learns of the migration (issue #36360).
            if (!APILocator.getUserAPI().isCMSAdmin(user)) {
                return false;
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
