package com.dotmarketing.util;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UtilHTML}
 */
public class UtilHTMLTest {

    /**
     * Method to test: getIconClass
     * Given Scenario: Given an unknown base type
     * ExpectedResult: Unknown icon should be returned
     */
    @Test
    public void test_getIconClass_unknown () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.ANY;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("uknIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a contentlet base type
     * ExpectedResult: contentIcon icon should be returned
     */
    @Test
    public void test_getIconClass_content () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.CONTENT;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("contentIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a widget base type
     * ExpectedResult: gearIcon icon should be returned
     */
    @Test
    public void test_getIconClass_widget () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.WIDGET;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("gearIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a form base type
     * ExpectedResult: formIcon icon should be returned
     */
    @Test
    public void test_getIconClass_form () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.FORM;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("formIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a file  base type and txt file
     * ExpectedResult: txtIcon icon should be returned
     */
    @Test
    public void test_getIconClass_txt_file () throws DotDataException, IOException, DotSecurityException {

        final Folder folder1 = new FolderDataGen().site(APILocator.systemHost()).nextPersisted();
        final Contentlet contentlet = FileAssetDataGen.createFileAsset(folder1, "text1", ".txt");

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("txtIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a file  base type and vtl file
     * ExpectedResult: vtlIcon icon should be returned
     */
    @Test
    public void test_getIconClass_vtl_file () throws DotDataException, IOException, DotSecurityException {

        final Folder folder1 = new FolderDataGen().site(APILocator.systemHost()).nextPersisted();
        final Contentlet contentlet = FileAssetDataGen.createFileAsset(folder1, "text2", ".vtl");

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("vtlIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a page base type
     * ExpectedResult: pageIcon icon should be returned
     */
    @Test
    public void test_getIconClass_page () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.HTMLPAGE;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("pageIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a key value base type
     * ExpectedResult: keyValueIcon icon should be returned
     */
    @Test
    public void test_getIconClass_keyvalue () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.KEY_VALUE;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("keyValueIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a persona base type
     * ExpectedResult: personaIcon icon should be returned
     */
    @Test
    public void test_getIconClass_persona () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.PERSONA;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("personaIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a vanity url base type
     * ExpectedResult: vanityIcon icon should be returned
     */
    @Test
    public void test_getIconClass_vanity () throws DotDataException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.VANITY_URL;
        when(contentlet.getContentType()).thenReturn(type);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("vanityIcon", iconClass);
    }

    /**
     * Method to test: getIconClass
     * Given Scenario: Given a dotasset base type and txt file
     * ExpectedResult: txtIcon icon should be returned
     */
    @Test
    public void test_getIconClass_dotasset_txt () throws DotDataException, IOException {

        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType type      = mock(ContentType.class);
        final BaseContentType any   = BaseContentType.DOTASSET;
        File txtFile                = FileUtil.createTemporalFile("testtxt", ".txt");
        when(contentlet.getContentType()).thenReturn(type);
        when(contentlet.getBinary(DotAssetContentType.ASSET_FIELD_VAR)).thenReturn(txtFile);
        when(type.baseType()).thenReturn(any);

        final String iconClass = UtilHTML.getIconClass(contentlet);
        assertNotNull(iconClass);
        assertEquals("txtIcon", iconClass);
    }
}
