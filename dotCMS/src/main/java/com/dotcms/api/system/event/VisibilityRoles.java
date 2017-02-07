package com.dotcms.api.system.event;

import com.dotmarketing.business.Role;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates the Roles for the roles {@link Visibility}.
 * It contains an operator (AND, OR) and a set of roles.
 * @author jsanca
 */
public class VisibilityRoles implements Serializable {

    public enum Operator { AND, OR };

    private final VisibilityRoles.Operator operator;
    private final List<String> rolesId;


    public VisibilityRoles(final VisibilityRoles.Operator operator, final Set<Role> roles) {

        this.operator    = operator;
        this.rolesId     = roles.stream().map( role ->  role.getId()).collect(Collectors.toList());
    }


    public VisibilityRoles.Operator getOperator() {
        return this.operator;
    }

    public List<String> getRolesId() {
        return this.rolesId;
    }
} // E:O:F:VisibilityRoles.
