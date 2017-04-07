package com.dotmarketing.business;

import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * PermissionableAPI allows to resolve {@link Permissionable} elements based on identifiers and inode ids.
 * @author jsanca
 */
public interface PermissionableAPI extends Serializable {

    /**
     * This method tries to determine the type and resolve the Permissionable with the better method.
     * The id could be an inode or identifier, even a host. The method will try to figure out which kind of type is an
     * consequiently returns the right instance, null or empty {@link Permissionable} in case it is not possible to determinate
     * the nature of the entity.
     * @param id {@link String}
     * @param language {@link Long}
     * @param user {@link User}
     * @param respectFrontendRoles boolean
     * @return Permissionable
     */
    Permissionable resolvePermissionable(String id, Long language, User user, boolean respectFrontendRoles);

    /**
     * This method returns a permissionable based on the inodeId (just for INode), if it couldn't retrieve the object
     * could return null or empty {@link Permissionable}
     * @param inodeId {@link String}
     * @param language {@link Long}
     * @param user {@link User}
     * @param respectFrontendRoles boolean
     * @return Permissionable
     */
    Permissionable resolvePermissionableByINode (final String inodeId, final Long language,
                                                 final User user, final boolean respectFrontendRoles);

    /**
     * This method returns a permissionable based on the identifier (just for Identifier), if it couldn't retrieve the object
     * could return null or empty {@link Permissionable}
     * @param identifier {@link String}
     * @param language {@link Long}
     * @param user {@link User}
     * @param respectFrontendRoles boolean
     * @return Permissionable
     */
    Permissionable resolvePermissionableByIdentifier (final String identifier, final Long language,
                                                      final User user, final boolean respectFrontendRoles);

} // E:O:F:PermissionableAPI.
