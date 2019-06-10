package com.dotcms.datagen;

import com.dotcms.util.ConfigTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.Files;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Jonathan Gamba 2019-06-10
 */
public class ThemeDataGen extends AbstractDataGen<Contentlet> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "testTheme" + currentTime;
    private Folder applicationFolder;
    private Folder themesFolder;
    private Host site;

    public ThemeDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public ThemeDataGen applicationFolder(final Folder applicationFolder) {
        this.applicationFolder = applicationFolder;
        return this;
    }

    public ThemeDataGen themesFolder(final Folder themesFolder) {
        this.themesFolder = themesFolder;
        return this;
    }

    public ThemeDataGen site(final Host site) {
        this.site = site;
        return this;
    }

    public Host getSite() {
        return site;
    }

    public Folder getThemesFolder() {
        return themesFolder;
    }

    public Folder getApplicationFolder() {
        return applicationFolder;
    }

    @Override
    public Contentlet next() {

        try {
            if (null == site) {
                site = new SiteDataGen().nextPersisted();
            }

            if (null == applicationFolder) {
                applicationFolder = APILocator.getFolderAPI()
                        .findFolderByPath("/application/", site, user, false);
            }
            if (null == applicationFolder || !UtilMethods
                    .isSet(applicationFolder.getIdentifier())) {
                applicationFolder = new FolderDataGen().site(site)
                        .name("application")
                        .nextPersisted();
            }

            if (null == themesFolder) {
                themesFolder = APILocator.getFolderAPI()
                        .findFolderByPath("/application/themes/", site, user, false);
            }
            if (null == themesFolder || !UtilMethods.isSet(themesFolder.getIdentifier())) {
                themesFolder = new FolderDataGen().site(site)
                        .parent(applicationFolder)
                        .name("themes")
                        .nextPersisted();
            }

            final Folder themeFolder = new FolderDataGen().site(site)
                    .parent(themesFolder)
                    .name(name)
                    .nextPersisted();

            return new FileAssetDataGen(themeFolder, getTemplateFile())
                    .setProperty(FileAssetAPI.TITLE_FIELD, "template.vtl")
                    .setProperty(FileAssetAPI.FILE_NAME_FIELD, "template.vtl").next();

        } catch (Exception e) {
            throw new RuntimeException("Unable to create theme.", e);
        }
    }

    private File getTemplateFile() throws URISyntaxException, IOException {

        final String testImagePath = "com/dotmarketing/portlets/contentlet/business/theme/template.vtl";

        final File originalTestImage = new File(
                ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
        final File testTemplateVtl = new File(Files.createTempDir(),
                "template" + System.currentTimeMillis() + ".vtl");
        FileUtil.copyFile(originalTestImage, testTemplateVtl);

        return testTemplateVtl;
    }

    @Override
    public Contentlet persist(final Contentlet templateVtlFile) {

        Contentlet templateFile = new ContentletDataGen(templateVtlFile.getContentTypeId())
                .persist(templateVtlFile);
        ContentletDataGen.publish(templateFile);

        return templateFile;
    }

    /**
     * Creates a new {@link Contentlet} template vtl instance and persists it in DB
     *
     * @return A new Contentlet instance persisted in DB
     */
    @Override
    public Contentlet nextPersisted() {
        return persist(next());
    }

}