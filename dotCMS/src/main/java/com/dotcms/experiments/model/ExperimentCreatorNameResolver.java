package com.dotcms.experiments.model;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.function.Supplier;

/**
 * Turns an {@link AbstractExperiment#createdBy()} user ID into the display name published as
 * {@code createdByUserName} (#37304).
 *
 * <p><b>The field is never null, absent or empty.</b> Every failure — a deleted user, an orphaned
 * reference, a user with no name set, or a lookup that blows up — resolves to the raw user ID
 * instead. That is a deliberate product choice: the Experiments portlet renders this in a
 * Created By column, and an ID an administrator can search on beats a blank cell. Note that other
 * parts of dotCMS answer the same question differently — push-publish history reports
 * {@code "Deleted"}, and the Content Drive folder view reports {@code "System"} / {@code "unknown"}.
 *
 * <p><b>Nothing here may throw.</b> The caller is a serializer running inside an already-successful
 * API response, so an exception escaping this class would turn a working experiment read into a
 * failed request over a field that is only decoration.
 *
 * <p><b>Cost.</b> {@code loadUserById} is served by {@code UserCache}, so a listing whose
 * experiments share a creator costs one database read for the first row and an in-memory hit for
 * every row after it. This class deliberately adds no memo of its own: the value is resolved per
 * serialization so that a user who renames themselves is reported under the new name.
 */
public class ExperimentCreatorNameResolver {

    /**
     * The instance used by the model. The {@link UserAPI} is supplied lazily rather than captured,
     * so class initialization never depends on {@code APILocator} being ready.
     */
    public static final ExperimentCreatorNameResolver INSTANCE =
            new ExperimentCreatorNameResolver(APILocator::getUserAPI);

    private final Supplier<UserAPI> userAPI;

    /**
     * Visible for testing: lets a test drive every fallback branch with a mocked {@link UserAPI}
     * and no database.
     */
    ExperimentCreatorNameResolver(final Supplier<UserAPI> userAPI) {
        this.userAPI = userAPI;
    }

    /**
     * Resolves a creator's display name, falling back to the ID itself.
     *
     * @param createdById the experiment's {@code createdBy} user ID
     * @return the creator's full name, or {@code createdById} when it cannot be resolved to a named
     * user. The argument is returned untouched when it is not set — unreachable through the model,
     * where {@code createdBy()} is a mandatory attribute.
     */
    public String resolve(final String createdById) {
        if (!UtilMethods.isSet(createdById)) {
            return createdById;
        }

        try {
            final User creator = this.userAPI.get().loadUserById(createdById);
            final String fullName = null != creator ? creator.getFullName() : null;

            return UtilMethods.isSet(fullName) ? fullName : createdById;
        } catch (final NoSuchUserException e) {
            // A deleted or orphaned creator is a data condition, not an error: debug keeps a
            // listing full of them from flooding the log while leaving the detail reachable.
            Logger.debug(this, () -> String.format(
                    "Experiment creator '%s' no longer resolves to a User; reporting the ID",
                    createdById));
            return createdById;
        } catch (final Exception e) {
            Logger.warn(this, String.format(
                    "Failed to resolve the name of Experiment creator '%s': %s",
                    createdById, e.getMessage()));
            return createdById;
        }
    }
}
