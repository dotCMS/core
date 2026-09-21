package com.dotcms.rest.api.v1.experiments;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.function.Supplier;

/**
 * Turns an {@code Experiment.createdBy()} user ID into the display name published as
 * {@code createdByUserName} (#37304).
 *
 * <p><b>The field is never null, absent or empty.</b> The system user is reported as
 * {@code "System"}, and everything that cannot be resolved to a named user — a deleted user, an
 * orphaned reference, a user with no name set, or a lookup that blows up — is reported as
 * {@code "unknown"}.
 *
 * <p>The two labels are borrowed from {@link com.dotcms.browser.BrowserAPIImpl}'s {@code ownerName},
 * which answers the same question for the Content Drive folder view: two listings in the same
 * product labelling the same orphaned owner differently is a worse outcome than either label on its
 * own. The alignment is exact for the deleted/orphaned and system-user cases and deliberately
 * stricter in two others: Content Drive publishes the raw {@code getFullName()} for a blank-named
 * user (a bare space) and {@code null} for an unset id, where this resolver reports
 * {@code "unknown"} so the field is never blank. Content Drive also reaches the user through
 * {@link com.liferay.portal.ejb.UserLocalManagerUtil} and so bypasses {@code UserCache}; this one
 * deliberately does not.
 *
 * <p><b>No checked or runtime exception may escape.</b> The caller is a serializer running inside
 * an already-successful API response, so an exception escaping this class would turn a working
 * experiment read into a failed request over a field that is only decoration. {@code Error} and its
 * subclasses are deliberately not caught.
 *
 * <p><b>Cost.</b> {@code UserFactoryImpl.loadUserById} consults {@code UserCache} before the
 * database, so a listing whose experiments share a <i>resolvable</i> creator costs one database
 * read for the first row and an in-memory hit for every row after it. An <i>unresolvable</i>
 * creator is the exception worth knowing about: a miss is not negative-cached, so every row
 * referencing an orphaned id pays its own query and its own {@code NoSuchUserException}. The same
 * residual limitation is documented for Content Drive on
 * {@code BrowserAPIImpl#warmUpUserCache}. This class deliberately adds no memo of its own: the
 * value is resolved per serialization so that a user who renames themselves is reported under the
 * new name.
 */
public class ExperimentCreatorNameResolver {

    /** Shown for an owner that cannot be resolved to a named user. Matches Content Drive. */
    static final String UNKNOWN = "unknown";

    /** Shown for the system user. Matches Content Drive. */
    static final String SYSTEM = "System";

    /** One warning per minute when the user layer is failing, rather than one per experiment. */
    private static final int WARN_THROTTLE_MILLIS = 60_000;

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
     * @return the creator's full name, trimmed; {@code "System"} for the system user;
     * {@code "unknown"} when the ID is unset, resolves to nobody, resolves to a user with no name
     * set, or the lookup itself fails. Never null, never blank.
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
            // getFullName() joins the parts with spaces and never trims, so a user with only a
            // first name yields "Admin ". Trim before publishing: the padding would be visible in
            // the portlet column, and an all-blank name must collapse to the fallback.
            final String fullName = null != creator ? creator.getFullName() : null;

            if (UtilMethods.isSet(fullName)) {
                return fullName.trim();
            }

            Logger.debug(this, () -> String.format(
                    "Experiment creator '%s' resolves to a User with no name set; reporting '%s'",
                    createdById, UNKNOWN));
            return UNKNOWN;
        } catch (final NoSuchUserException e) {
            // A deleted or orphaned creator is a data condition, not an error: debug keeps a
            // listing full of them from flooding the log while leaving the detail reachable.
            Logger.debug(this, e, () -> String.format(
                    "Experiment creator '%s' no longer resolves to a User; reporting '%s'",
                    createdById, UNKNOWN));
            return UNKNOWN;
        } catch (final Exception e) {
            // Unlike the branch above, this one means something is actually wrong, and under a
            // failing user layer it fires once per row. Throttle it and keep the stack trace:
            // without the throwable a NullPointerException logs as "...: null" and is undiagnosable.
            Logger.warnEveryAndDebug(ExperimentCreatorNameResolver.class, String.format(
                    "Failed to resolve the name of Experiment creator '%s'", createdById),
                    e, WARN_THROTTLE_MILLIS);
            return UNKNOWN;
        }
    }
}
