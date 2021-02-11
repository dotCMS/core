package com.dotcms.rendering.velocity.viewtools.navigation;

import com.dotcms.UnitTestBase;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import org.junit.Assert;
import org.junit.Test;

public class NavResultTest extends UnitTestBase {


    /**
     * Method to test: getEnclosingPermissionClassName
     * Given Scenario: the type attribute is null
     * ExpectedResult: should throw an IllegalStateException
     */
    @Test(expected = IllegalStateException.class)
    public void getEnclosingPermissionClassName_on_null_type() throws DotDataException {

        new NavResult("", "", "", 1l, null).getEnclosingPermissionClassName();
    }

    /**
     * Method to test: getEnclosingPermissionClassName
     * Given Scenario: the type attribute is unknown
     * ExpectedResult: should throw an IllegalStateException
     */
    @Test(expected = IllegalStateException.class)
    public void getEnclosingPermissionClassName_on_unknown_type() throws DotDataException {

        final NavResult navResult = new NavResult("", "", "", 1l, null);
        navResult.setType("unknown");
        navResult.getEnclosingPermissionClassName();
    }

    /**
     * Method to test: getEnclosingPermissionClassName
     * Given Scenario: the type attribute is htmlpage
     * ExpectedResult: should IHTMLPage.class.getCanonicalName()
     */
    @Test()
    public void getEnclosingPermissionClassName_on_htmlpage_type() throws DotDataException {

        final NavResult navResult = new NavResult("", "", "", 1l, null);
        navResult.setType("htmlpage");
        Assert.assertEquals(IHTMLPage.class.getCanonicalName(), navResult.getEnclosingPermissionClassName());
    }

    /**
     * Method to test: getEnclosingPermissionClassName
     * Given Scenario: the type attribute is link
     * ExpectedResult: should IHTMLPage.class.getCanonicalName()
     */
    @Test()
    public void getEnclosingPermissionClassName_on_link_type() throws DotDataException {

        final NavResult navResult = new NavResult("", "", "", 1l, null);
        navResult.setType("link");
        Assert.assertEquals(Link.class.getCanonicalName(), navResult.getEnclosingPermissionClassName());
    }

    /**
     * Method to test: getEnclosingPermissionClassName
     * Given Scenario: the type attribute is folder
     * ExpectedResult: should IHTMLPage.class.getCanonicalName()
     */
    @Test()
    public void getEnclosingPermissionClassName_on_folder_type() throws DotDataException {

        final NavResult navResult = new NavResult("", "", "", 1l, null);
        navResult.setType("folder");
        Assert.assertEquals(Folder.class.getCanonicalName(), navResult.getEnclosingPermissionClassName());
    }
}
