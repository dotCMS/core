package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.VisibilityRoles;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;

import java.util.List;


/**
 * Multiple Roles Verifier, verify a set of roles against the user's one.
 * The user can specified if it is an OR, or an AND.
 *
 * If it is an OR, means any match against the user's roles will consider the payload as valid.
 * If it is an AND, means user's roles has to match with the payload roles.
 * @author jsanca
 */
public class MultipleRolesVerifier implements PayloadVerifier {

    private final RoleAPI roleAPI;

    public MultipleRolesVerifier() {
        this(APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public MultipleRolesVerifier(final RoleAPI roleAPI) {
        this.roleAPI = roleAPI;
    }

    @Override
    public boolean verified(final Payload payload, final User sessionUser) {
        try {

            return this.checkRoles(sessionUser,
                    (VisibilityRoles)payload.getVisibilityValue());
        } catch (DotDataException e) {
            throw new VerifierException(e);
        }
    }

    private boolean checkRoles(final User user, final VisibilityRoles visibilityRoles) throws DotDataException {

        return (visibilityRoles.getOperator() == VisibilityRoles.Operator.AND)?
                    this.checkRolesAnd(user, visibilityRoles.getRolesId()):
                    this.checkRolesOr (user, visibilityRoles.getRolesId());
    }

    private boolean checkRolesAnd(final User user, final List<String> rolesId) throws DotDataException {

        boolean verified = true;

        for (String roleId : rolesId) {

            verified &= this.roleAPI.doesUserHaveRole(user, roleId);
            if (!verified) {

                break;
            }
        }

        return verified;
    }

    private boolean checkRolesOr(final User user, final List<String> rolesId) throws DotDataException {

        boolean verified = false;

        for (String roleId : rolesId) {

            verified |= this.roleAPI.doesUserHaveRole(user, roleId);
            if (verified) {

                break;
            }
        }

        return verified;
    }

} // E:O:F:MultipleRolesVerifier.