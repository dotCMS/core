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
 * <p><b>The field is never null, absent or empty.</b> The system user is reported as
 * {@code "System"}, and everything that cannot be resolved to a named user — a deleted user, an
 * orphaned reference, a user with no name set, or a lookup that blows up — is reported as
 * {@code "unknown"}.
 *
 * <p>Those two labels are not invented here: they are the ones
 * {@code BrowserAPIImpl.ownerName} already publishes for the Content Drive folder view, which
 * answers exactly this question for a different listing. Two listings in the same product labelling
 * the same orphaned owner differently is a worse outcome than either label on its own. (Note that
 * the Content Drive implementation reaches the user through {@code UserLocalManagerUtil} and so
 * bypasses {@code UserCache}; this one deliberately does not.)
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

    /** Shown for an owner that cannot be resolved to a named user. Matches Content Drive. */
    static final String UNKNOWN = "unknown";

    /** Shown for the system user. Matches Content Drive. */
    static final String SYSTEM = "System";

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
     *
     * @param userAPI supplies the user layer when a lookup is actually needed
     */
    ExperimentCreatorNameResolver(final Supplier<UserAPI> userAPI) {
        this.userAPI = userAPI;
    }

    /**
     * Resolves a creator's display name.
     *
     * @param createdById the experiment's {@code createdBy} user ID
     * @return the creator's full name; {@code "System"} for the system user; {@code "unknown"} when
     * the ID is unset, resolves to nobody, or resolves to a user with no name set. Never null,
     * never empty.
     */
    public String resolve(final String createdById) {
        if (!UtilMethods.isSet(createdById)) {
            return UNKNOWN;
        }

        if (UserAPI.SYSTEM_USER_ID.equalsIgnoreCase(createdById)) {
            return SYSTEM;
        }

        try {
            final User creator = this.userAPI.get().loadUserById(createdById);
            final String fullName = null != creator ? creator.getFullName() : null;

            return UtilMethods.isSet(fullName) ? fullName : UNKNOWN;
        } catch (final NoSuchUserException e) {
            // A deleted or orphaned creator is a data condition, not an error: debug keeps a
            // listing full of them from flooding the log while leaving the detail reachable.
            Logger.debug(this, () -> String.format(
                    "Experiment creator '%s' no longer resolves to a User; reporting '%s'",
                    createdById, UNKNOWN));
            return UNKNOWN;
        } catch (final Exception e) {
            Logger.warn(this, String.format(
                    "Failed to resolve the name of Experiment creator '%s': %s",
                    createdById, e.getMessage()));
            return UNKNOWN;
        }
    }
}
