package com.dotcms.datagen;

import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import java.io.IOException;

public class TemplateAsFileDataGen extends AbstractDataGen<FileAssetTemplate> {

    private Host host;
    private String folderName = "/file-template" + System.currentTimeMillis();
    private String metaDataBasic = "$dotJSON.put(\"title\", \"Test Template\")\n";
    private String metaDataDesign = metaDataBasic + "$dotJSON.put(\"theme\", \"system_theme\")\n";
    private FileAsset metaData;
    private FileAsset templateCode;
    private boolean isDesignTemplate = true;

    public TemplateAsFileDataGen() throws DotSecurityException, DotDataException {
        host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),false);
    }

    @Override
    public FileAssetTemplate next() {
        return new FileAssetTemplate();
    }

    public TemplateAsFileDataGen host(final Host host) {
        this.host = host;
        return this;
    }

    public TemplateAsFileDataGen folderName(final String folderName) {
        this.folderName = folderName;
        return this;
    }

    public TemplateAsFileDataGen metaDataBasic(final String metaDataBasic) {
        this.metaDataBasic = metaDataBasic;
        return this;
    }

    public TemplateAsFileDataGen designTemplate(final boolean isDesignTemplate){
        this.isDesignTemplate = isDesignTemplate;
        return this;
    }

    public FileAsset getMetaData() {
        return metaData;
    }

    public FileAsset getTemplateCode() {
        return templateCode;
    }

    @WrapInTransaction
    @Override
    public FileAssetTemplate persist(FileAssetTemplate object){
        try{
            final Folder templateFolder = createFileAsTemplateFolderIfNeeded();
            String templateDefinitionFileName = "layout.json";
            String templateDefinitionFileCode = layoutFile();
            if(!isDesignTemplate) {
                templateDefinitionFileName = "body.vtl";
                templateDefinitionFileCode = bodyFile();
            }
            final java.io.File file = java.io.File.createTempFile(
                    templateDefinitionFileName.split("\\.")[0],
                    templateDefinitionFileName.split("\\.")[1]);
            FileUtil.write(file, templateDefinitionFileCode);
            final Contentlet templateCodeFileAsset = new FileAssetDataGen(templateFolder,file)
                    .host(host).setProperty("title",templateDefinitionFileName)
                    .setProperty("fileName",templateDefinitionFileName)
                    .nextPersisted();
            templateCode = APILocator.getFileAssetAPI().fromContentlet(templateCodeFileAsset);

            final java.io.File fileMetaData = java.io.File.createTempFile(
                    Constants.TEMPLATE_META_INFO_FILE_NAME.split("\\.")[0],
                    Constants.TEMPLATE_META_INFO_FILE_NAME.split("\\.")[1]);
            FileUtil.write(fileMetaData, isDesignTemplate ? metaDataDesign : metaDataBasic);
            final Contentlet templateMetaDataFileAsset = new FileAssetDataGen(templateFolder,fileMetaData)
                    .host(host).setProperty("title",Constants.TEMPLATE_META_INFO_FILE_NAME)
                    .setProperty("fileName",Constants.TEMPLATE_META_INFO_FILE_NAME)
                    .nextPersisted();
            metaData = APILocator.getFileAssetAPI().fromContentlet(templateMetaDataFileAsset);

            return toFileAssetTemplate(object,templateFolder,metaData,templateCode);
        } catch (DotDataException | DotSecurityException | IOException e) {
            Logger.warnAndDebug(TemplateAsFileDataGen.class,e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    private FileAssetTemplate toFileAssetTemplate(final FileAssetTemplate template, final Folder templateFolder,
            final FileAsset metaData, final FileAsset templateCode) {
        template.setIdentifier(buildTemplateId(templateFolder));
        template.setInode(metaData.getInode());
        template.setOwner      (metaData.getOwner());
        template.setIDate      (metaData.getIDate());
        template.setModDate    (metaData.getModDate());
        template.setModUser    (metaData.getModUser());
        template.setShowOnMenu (metaData.isShowOnMenu());
        template.setSortOrder  (templateFolder.getSortOrder());
        template.setTitle      (templateFolder.getTitle());
        template.setLanguage(metaData.getLanguageId());
        template.setPath(templateFolder.getPath());
        if (isDesignTemplate) {
            template.setLayoutAsset(templateCode);
        } else {
            template.setBodyAsset(templateCode);
        }
        return template;
    }

    private String buildTemplateId(final Folder templateFolder) {

        return builder("//", host.getHostname(), templateFolder.getPath()).toString();
    }

    private String bodyFile() {
        return "#parseContainer('//default/application/containers/system/','1615825205295')";
    }

    private String layoutFile() {
       return "{" 
               + "\"body\":{" 
               + "\"rows\":[ " 
               + "{" 
               + "\"styleClass\":\"\"," 
               + "\"columns\":[ " 
               + "{" 
               + "\"styleClass\":\"\"," 
               + "\"leftOffset\":1," 
               + "\"width\":12," 
               + "\"containers\":[ " 
               + "{" 
               + "\"identifier\":\"//default/application/containers/system/\"," 
               + "\"uuid\":\"1\" " 
               + "}" 
               + "] " 
               + "}" 
               + "] " 
               + "}" 
               + "] " 
               + "}," 
               + "\"header\":true," 
               + "\"footer\":true," 
               + "\"sidebar\":null " 
               + "}";
    }

    private boolean checkFileAsTemplatePathExist() throws DotSecurityException, DotDataException {
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(Constants.TEMPLATE_FOLDER_PATH, host, APILocator.systemUser(),
                        false);
        return (null != folder && UtilMethods.isSet(folder.getIdentifier()));
    }

    private void createApplicationTemplateFolderIfNeeded()
            throws DotDataException, DotSecurityException {
        if (!checkFileAsTemplatePathExist()) {
            APILocator.getFolderAPI()
                    .createFolders(Constants.TEMPLATE_FOLDER_PATH, host, APILocator.systemUser(),
                            false);
        }
    }

    private synchronized Folder createFileAsTemplateFolderIfNeeded()
            throws DotDataException, DotSecurityException {
        createApplicationTemplateFolderIfNeeded();
        String fullPath = Constants.TEMPLATE_FOLDER_PATH + "/" + folderName;
        fullPath = !fullPath.endsWith("/") ? fullPath + "/" : fullPath;

        Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(fullPath, host, APILocator.systemUser(), false);
        if (null == folder || !UtilMethods.isSet(folder.getIdentifier())) {
            folder = APILocator.getFolderAPI()
                    .createFolders(fullPath, host, APILocator.systemUser(), false);
        }
        return folder;
    }
}
