package com.dotmarketing.portlets.folders.business;

import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtil;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Folder listener for the application/template folder
 * @author erickgonzalez
 */
public class ApplicationTemplateFolderListener implements FolderListener{

    @Override
    public void folderChildModified(final FolderEvent folderEvent) {

        if (null != folderEvent && null != folderEvent.getChild()) {

            final String fileAssetName = folderEvent.getChildName();
            final Folder templateFolder = folderEvent.getParent();

            if(checkTemplatePropertiesAsset(templateFolder.getHost(),templateFolder) && checkAsset(fileAssetName)) {
                try {
                    final Template template = APILocator.getTemplateAPI()
                            .getTemplateByFolder(templateFolder, templateFolder.getHost(),
                                    folderEvent.getUser(), false);

                    if(template!=null && UtilMethods.isSet(template.getIdentifier())){
                        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
                        new TemplateLoader().invalidate(FileAssetTemplate.class.cast(template));
                        CacheLocator.getTemplateCache().remove(template.getInode());
                    }

                } catch (DotSecurityException | DotDataException e) {
                    Logger.debug(this, "The child: " + fileAssetName + " on the folder: " +
                            templateFolder + ", has been removed, BUT the template could not be invalidated", e);
                }
            }
        }
    }

    /**
     * Checks that the asset needs to be one of the needed for templates
     * (body.vtl or layout.json or properties.vtl)
     * returns true if is.
     */
    private boolean checkAsset(String fileAssetName) {
        return FileAssetTemplateUtil.TEMPLATE_META_INFO.contains(fileAssetName) ||
                FileAssetTemplateUtil.LAYOUT.contains(fileAssetName) ||
                FileAssetTemplateUtil.BODY.contains(fileAssetName);
    }

    /**
     * Checks if inside the folder provided is the properties.vtl file
     * returns true if exists.
     */
    private boolean checkTemplatePropertiesAsset(final Host host, final Folder folder){
        try {

            final Identifier identifier = APILocator.getIdentifierAPI().find(host, builder(folder.getPath(),
                    FileAssetTemplateUtil.TEMPLATE_META_INFO).toString());
            return null != identifier && UtilMethods.isSet(identifier.getId());
        } catch (Exception  e) {
            return false;
        }
    }

}
