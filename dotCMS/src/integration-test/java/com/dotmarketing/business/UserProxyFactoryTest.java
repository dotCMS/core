package com.dotmarketing.business;

import com.dotcms.repackage.org.apache.hadoop.util.hash.Hash;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.IntegrationTestInitService;
import com.liferay.portal.model.User;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Nollymar Longa on 8/8/16.
 */
public class UserProxyFactoryTest {
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

    @Test
    public void testSearchUsersAndUsersProxy() throws DotDataException {
        UserProxyFactory proxyFactory = new UserProxyFactoryImpl();
        HashMap<String, Object>
            results = proxyFactory.searchUsersAndUsersProxy("anonymous",null,null,false,null,true,"userid",1,1);

        assertNotNull(results);
        assertTrue(((Long)results.get("total")) > 0);
        assertTrue(((ArrayList<User>)results.get("users")).size() == 1);
        assertTrue(((ArrayList<User>)results.get("users")).get(0).getFirstName().contains("anonymous"));
    }

    @Test
    public void testSearchUsersAndUsersProxyWithoutFilter() throws DotDataException {
        UserProxyFactory proxyFactory = new UserProxyFactoryImpl();
        HashMap<String, Object>
            results = proxyFactory.searchUsersAndUsersProxy(null,null,null,false,null,true,null,1,1);

        assertNotNull(results);
        assertTrue(((Long)results.get("total")) > 0);
        assertTrue(((ArrayList<User>)results.get("users")).size() > 0);
    }
}
