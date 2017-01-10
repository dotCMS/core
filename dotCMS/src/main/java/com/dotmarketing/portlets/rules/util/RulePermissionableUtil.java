package com.dotmarketing.portlets.rules.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Created by Oscar Arrieta on 2/6/16.
 */
public class RulePermissionableUtil {

	/**
	 * Determines the permissionable parent of a {@link Rule} object.
	 * Permissionable parents in dotCMS can be a Site (Host), or a Folder.
	 * 
	 * @param parent
	 *            - The Identifier of the rule's parent.
	 * @return The permissionable parent of the rule.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 */
    public static Permissionable findParentPermissionable(String parent) throws DotDataException {
        Permissionable permissionableParent = null;
        try {
            User systemUser = APILocator.getUserAPI().getSystemUser();
            Identifier iden = APILocator.getIdentifierAPI().find(parent);
            if(UtilMethods.isSet(iden) && UtilMethods.isSet(iden.getAssetType())){
                if(iden.getAssetType().equals("folder")){
                    permissionableParent = APILocator.getFolderAPI().find(parent,systemUser,false);
                }else{
					Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(parent, false,
							APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false);
					if (contentlet.isHost()) {
						permissionableParent = contentlet;
					} else {
						permissionableParent = contentlet.getParentPermissionable();
					}
                }
            } else {
                throw new DotDataException("Parent Identifier: " + parent + " does NOT exist.");
            }
        } catch (DotSecurityException e) {
            Logger.error(Rule.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
        return permissionableParent;
    }

}
