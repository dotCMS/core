package com.dotcms.util.user;

import com.liferay.portal.model.User;
import java.util.Map;


/**
 * This interface define the User transformer methods
 *
 * @author oswaldogallango
 * @since 2017
 */
public interface UserTransformer {

    /**
     * Return a com.liferay.portal.model.User from the Map<String, Object> object
     * @param map DB map returned by the dotconnect user seach
     * @return User
     */
    User fromMap(Map<String, Object> map);

}
