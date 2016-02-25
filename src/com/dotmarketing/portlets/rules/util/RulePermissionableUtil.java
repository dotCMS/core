package com.dotmarketing.portlets.rules.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Created by Oscar Arrieta on 2/6/16.
 */
public class RulePermissionableUtil {

    public static Permissionable findParentPermissionable(String parent) throws DotDataException {
        Permissionable pp;

        try {
            User systemUser = APILocator.getUserAPI().getSystemUser();
            Identifier iden = APILocator.getIdentifierAPI().find(parent);

            if(UtilMethods.isSet(iden) && UtilMethods.isSet(iden.getAssetType())){
                if(iden.getAssetType().equals("folder")){
                    pp = APILocator.getFolderAPI().find(parent,systemUser,false);
                }else{
                    pp = APILocator.getContentletAPI()
                            .findContentletByIdentifier(parent, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false);
                }
            } else {
                throw new DotDataException("Parent Identifier: " + parent + " does NOT exist.");
            }

        } catch (DotSecurityException e) {
            Logger.error(Rule.class, e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }

        return pp;
    }
}
