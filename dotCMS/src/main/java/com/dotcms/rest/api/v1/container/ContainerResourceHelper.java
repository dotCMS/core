package com.dotcms.rest.api.v1.container;

import com.dotcms.rendering.velocity.directive.DotParse;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;


public class ContainerResourceHelper implements Serializable {

    private ContainerResourceHelper() {

    }

    private static class SingletonHolder {
        private static final ContainerResourceHelper INSTANCE = new ContainerResourceHelper();
    }

    public static ContainerResourceHelper getInstance() {
        return ContainerResourceHelper.SingletonHolder.INSTANCE;
    }

    /**
     * If the container is a {@link FileAssetContainer} will set on the request the right languages in order to get them on the {@link DotParse}
     *
     * @param container {@link Container}
     * @param request   {@link HttpServletRequest}
     */
    public void setContainerLanguage(final Container container, final HttpServletRequest request) {

        if (container instanceof FileAssetContainer &&
                request.getParameter(WebKeys.LANGUAGE_ID_PARAMETER) == null) {

            final FileAssetContainer fileAssetContainer = (FileAssetContainer)container;
            final Language containerLanguage = APILocator.getLanguageAPI().getLanguage(fileAssetContainer.getLanguageId());

            if (null != containerLanguage && UtilMethods.isSet(containerLanguage.getLanguageCode())) {

                // unfortunately we need this level of linkage between the ContainerResource and the DotParse
                // in order to tell to the DotParse (in a way that it could understand) that the container is
                // in a diff language than the request is.
                DotParse.setContentLanguageId(request, container.getIdentifier(), containerLanguage.getId());
                final List<FileAsset> containerContentTypeFileAssets = fileAssetContainer.getContainerStructuresAssets();
                if (null != containerContentTypeFileAssets) {

                    for (final FileAsset fileAsset: containerContentTypeFileAssets) {

                        DotParse.setContentLanguageId(request, fileAsset.getIdentifier(), fileAsset.getLanguageId());
                    }
                }

                final FileAsset preLoopAsset = fileAssetContainer.getPreLoopAsset();
                if (null != preLoopAsset) {

                    DotParse.setContentLanguageId(request, preLoopAsset.getIdentifier(), preLoopAsset.getLanguageId());
                }

                final FileAsset postLoopAsset = fileAssetContainer.getPostLoopAsset();
                if (null != postLoopAsset) {

                    DotParse.setContentLanguageId(request, postLoopAsset.getIdentifier(), postLoopAsset.getLanguageId());
                }
            }
        }
    } // setContainerLanguage.

}
