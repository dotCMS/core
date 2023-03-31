package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ContentletDestroyAPIImpl implements ContentletDestroyDelegate {

    final ContentletCache contentletCache = CacheLocator.getContentletCache();

    @WrapInTransaction
    @Override
    public void destroy(final List<Contentlet> contentlets, final User user) {
        for (final Contentlet contentlet : contentlets){
            try {
                contentletCache.remove(contentlet.getInode());
                destroy(contentlet, user);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error destroying contents", e);
            }
        }
        deleteVersions(contentlets);
    }

    void destroy(final Contentlet contentlet, final User user) throws DotDataException, DotSecurityException {
        destroyWorkflows(contentlet, user);
        destroyRules(contentlet, user);
        destroyCategories(contentlet, user);
        destroyRelationships(contentlet, user);
        destroyMultiTree(contentlet);
        deleteBinaries(contentlet);
        deleteElementsFromPublishQueueTable(contentlet);
        removeFromCache(contentlet);
        //  destroyMetadata(contentlet);
        //No need to remove anything from index since that duty has already been done when the original CT was deleted
    }

    void deleteElementsFromPublishQueueTable(final Contentlet contentlet) {
        try {
            PublisherAPI.getInstance()
                    .deleteElementFromPublishQueueTable(contentlet.getIdentifier());
        } catch (DotPublisherException e) {
            Logger.error(getClass(),
                    "Error destroying Contentlet from Publishing Queue with Identifier: "
                            + contentlet.getIdentifier());
        }
    }

    void destroyWorkflows(final Contentlet contentlet, final User user) throws DotDataException {
        try {
            FactoryLocator.getWorkFlowFactory().deleteWorkflowTaskByContentletIdAnyLanguage(contentlet.getIdentifier());
        } catch (InvalidLicenseException ile) {
            Logger.warn(this, "An enterprise license is required to delete workflows under pages.");
        }
    }

    void destroyRules(final Contentlet contentlet, final User user) throws  DotDataException, DotSecurityException{
        try {
            APILocator.getRulesAPI()
                    .deleteRulesByParent(contentlet, user, false);
        } catch (InvalidLicenseException  ile) {
            Logger.warn(this, "An enterprise license is required to delete rules under pages.");
        }
    }

    void removeFromCache(final Contentlet contentlet){
        if(contentlet.isVanityUrl()){
           APILocator.getVanityUrlAPI().invalidateVanityUrl(contentlet);
        }
        contentletCache.remove(contentlet.getInode());
    }

    void destroyCategories(final Contentlet contentlet, final User user) throws DotDataException, DotSecurityException {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        categoryAPI.removeChildren(contentlet, user, false);
        categoryAPI.removeParents(contentlet, user, false);
    }

    void destroyRelationships(final Contentlet contentlet, final User user)
            throws DotSecurityException, DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        final List<Relationship> relationships =
                relationshipAPI.byContentType(contentlet.getStructure());
        // Remove related contents
        for (final Relationship relationship : relationships) {
            final boolean hasParent = relationshipAPI.isParent(relationship, contentlet.getStructure());
            contentletAPI.deleteRelatedContent(contentlet, relationship, hasParent, user, false);
        }
    }

    private void destroyMultiTree(Contentlet contentlet) throws DotDataException {
        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final List<MultiTree> multiTrees = multiTreeAPI.getMultiTreesByChild(contentlet.getIdentifier());
        for (final MultiTree multiTree : multiTrees) {
            if(contentlet.isHTMLPage()){
                handlePage(multiTree, contentlet, APILocator.systemUser());
            }
            multiTreeAPI.deleteMultiTree(multiTree);
        }
    }

    private void handlePage(MultiTree multiTree, Contentlet contentlet, User user)
            throws DotDataException {

        final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final Identifier pageIdentifier = identifierAPI.find(multiTree.getHtmlPage());
        if (pageIdentifier != null && UtilMethods.isSet(pageIdentifier.getInode())) {
            try {
                final IHTMLPage page = htmlPageAssetAPI.fromContentlet(contentlet);
                if (page != null && UtilMethods.isSet(page.getIdentifier())) {
                    new PageLoader().invalidate(page);
                }
            } catch (DotStateException dde) {
                Logger.warn(this.getClass(), "Page with id:" + pageIdentifier.getId() + " does not exist");
            }
        }
    }

    void deleteBinaries(Contentlet contentlet) {
        final Path rootPath = binaryPath(contentlet);
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    // .peek(System.out::println)
                    .forEach(File::delete);
        } catch (IOException e) {
            Logger.warn(this,
                    String.format("Unable to delete binaries under [%s] ", rootPath.toString()), e);
        }
    }

    Path binaryPath(final Contentlet con) {
        String inode = con.getInode();
        String path = String.format("%s/%s/%s/%s",
                APILocator.getFileAssetAPI().getRealAssetsRootPath(), inode.charAt(0),
                inode.charAt(1), inode);
        return Paths.get(path);
    }

    void deleteVersions(List<Contentlet> contentlets) {
        try {
            FactoryLocator.getContentletFactory().delete(contentlets);
        } catch (DotDataException e) {
            Logger.error(this, "Error deleting versions", e);
        }
        final IdentifierCache identifierCache = CacheLocator.getIdentifierCache();
        contentlets.forEach(identifierCache::removeFromCacheByVersionable);
    }

}
