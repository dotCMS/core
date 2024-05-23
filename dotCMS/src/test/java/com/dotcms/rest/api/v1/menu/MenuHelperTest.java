package com.dotcms.rest.api.v1.menu;

import org.junit.Test;

import static org.junit.Assert.*;

public class MenuHelperTest {

    @Test
    public void test_null_values() {
        assertEquals(MenuHelper.INSTANCE.normalizeLinkName(null), "ukn");
    }


    @Test
    public void test_portlet_values() {
        final String PORTLET_KEY_NULL="com.dotcms.repackage.javax.portlet.title.i_dont_exist";
        assertEquals(MenuHelper.INSTANCE.normalizeLinkName(PORTLET_KEY_NULL), "I Dont Exist");
    }

    @Test
    public void test_custom_portlet_values() {
        // custom portlets start with `c_`
        final String PORTLET_KEY_NULL="com.dotcms.repackage.javax.portlet.title.c_i_dont_exist";
        assertEquals(MenuHelper.INSTANCE.normalizeLinkName(PORTLET_KEY_NULL), "I Dont Exist");
    }

}