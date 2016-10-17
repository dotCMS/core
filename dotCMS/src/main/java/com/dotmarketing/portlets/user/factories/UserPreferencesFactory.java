package com.dotmarketing.portlets.user.factories;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.user.model.UserPreference;
import com.dotmarketing.util.Logger;

/**
 *
 * @author  maria
 */
public class UserPreferencesFactory {

	private static HashMap usersPreferences = new HashMap ();
	
	public static UserPreference getUserPreference(String x) {
        try {
            return ( UserPreference ) new HibernateUtil(UserPreference.class).load(Long.parseLong(x));
        } catch (Exception e) {
            try {
				return ( UserPreference ) new HibernateUtil(UserPreference.class).load(x);
			} catch (DotHibernateException e1) {
				Logger.error(UserPreferencesFactory.class, "getUserPreference failed:" + e,e);
				return new UserPreference();
			}
        }
    }

    public static java.util.List getUserPreferences(String userId) {
        try {
            Logger.debug(UserPreferencesFactory.class, "Retrieving user preferences from DB.");
            HibernateUtil dh = new HibernateUtil(UserPreference.class);
            dh.setQuery("from user_preferences in class com.dotmarketing.portlets.user.model.UserPreference where user_id = ?");
            dh.setParam(userId);

            List list = dh.list();
        	HashMap preferences = getUserPreferencesMap(userId);
        	Iterator it = list.iterator();
        	while (it.hasNext()) {
        		UserPreference pref = (UserPreference)it.next();
        		preferences.put(pref.getPreference(), pref);
        	}
        	return list;
        } catch (Exception e) {
            Logger.warn(UserPreferencesFactory.class, "getUserPreferences failed:" + e, e);
        }
        return new java.util.ArrayList();
    }

    public static UserPreference getUserPreferenceValue(String userId, String preference) {
    	HashMap preferences = getUserPreferencesMap(userId);
    	if (preferences.containsKey(preference))
    		return (UserPreference) preferences.get(preference);
    	else {
	        try {
	            Logger.debug(UserPreferencesFactory.class, "Retrieving preference from DB.");
	            HibernateUtil dh = new HibernateUtil(UserPreference.class);
	            dh.setQuery("from user_preferences in class com.dotmarketing.portlets.user.model.UserPreference where user_id = ? and preference = ?");
	            dh.setParam(userId);
	            dh.setParam(preference);
	
	            UserPreference up = (UserPreference) dh.load();
	            preferences.put(preference, up);
	            return up;
	
	        } catch (Exception e) {
	            Logger.warn(UserPreferencesFactory.class, "getUserPreferenceValue failed:" + e, e);
	        }
	        return new UserPreference();
    	}
    }

    public static void deleteUserPreference(UserPreference u) {
    	HashMap preferences = getUserPreferencesMap(u.getUserId());
    	if (preferences.containsKey(u.getPreference()))
    		preferences.remove(u.getPreference());
    	
        try {
			HibernateUtil.delete(u);
		} catch (DotHibernateException e) {
			Logger.error(UserPreferencesFactory.class, "deleteUserPreference failed:" + e,e);
		}
    }

	public static void deleteUserPreference(String userId, String preference) {

    	UserPreference up = getUserPreferenceValue(userId, preference);

    	HashMap preferences = getUserPreferencesMap(userId);
    	if (preferences.containsKey(up.getPreference()))
    		preferences.remove(up.getPreference());

    	try {
			HibernateUtil.delete(up);
		} catch (DotHibernateException e) {
			Logger.error(UserPreferencesFactory.class, "deleteUserPreference failed:" + e,e);
		}
	}

    public static void saveUserPreference(UserPreference u) {
    	
    	HashMap preferences = getUserPreferencesMap(u.getUserId());
    	if (preferences.containsKey(u.getPreference()))
    		preferences.remove(u.getPreference());
		preferences.put(u.getPreference(), u);

        try {
			HibernateUtil.saveOrUpdate(u);
		} catch (DotHibernateException e) {
			Logger.error(UserPreferencesFactory.class, "saveUserPreference failed:",e);
		}
    }

    public static void addUserPreference(String userId, String preference, String value) {
    	try {
			//creates new permission to read
	    	UserPreference u = new UserPreference();
	    	u.setUserId(userId);
	    	u.setPreference(preference);
	    	u.setValue(value);
	    	saveUserPreference(u);
    	}
    	catch (Exception e) {

    	}
    }

    private static HashMap getUserPreferencesMap (String userId) {
    	HashMap preferences = (HashMap)usersPreferences.get(userId);
    	if (preferences == null) {
    		preferences = new HashMap ();
    		usersPreferences.put (userId, preferences);
    	}
    	return preferences;
    }


}
