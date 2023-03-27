package com.dotcms.contenttype.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;

import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContentletDisposeAPIImpl implements ContentletDisposeAPI {

    /**
     * This method relocates all contentlets from a given structure to another structure And returns
     * the new structure purged from fields
     *
     * @param source
     * @param target
     * @throws DotDataException
     */
    @WrapInTransaction
    public void relocateContentletsForDeletion(final ContentType source, final ContentType target)
            throws DotDataException {

        if (!source.getClass().equals(target.getClass())) {
            throw new DotDataException(
                    String.format(
                            "Incompatible source and target ContentTypes. source class is (%s) target class is (%s)",
                            source.getClass().getSimpleName(), target.getClass().getSimpleName())
            );
        }

        //Avoid conflicts with CTs coming from old starters
        final List<String> requiredFields = source.requiredFields().stream()
                .map(com.dotcms.contenttype.model.field.Field::variable)
                .collect(Collectors.toList());
        final List<com.dotcms.contenttype.model.field.Field> sourceFields = new ArrayList<>(
                source.fields());
        sourceFields.removeIf(field -> requiredFields.contains(field.variable()));

        final Map<String, Field> targetMappedByVar = target.fields().stream().collect(
                Collectors.toMap(com.dotcms.contenttype.model.field.Field::variable,
                        Function.identity()));

        for (final com.dotcms.contenttype.model.field.Field field : sourceFields) {
            final com.dotcms.contenttype.model.field.Field targetField = targetMappedByVar.get(
                    field.variable());
            if (null == targetField) {
                Logger.warn(this, String.format(
                        "Unable to match field (%s) in target structure (%s) by name.",
                        field.variable(), target.name()));
                continue;
            }

            if (!field.dataType().equals(targetField.dataType())) {
                Logger.warn(this, String.format(
                        "Unable to match field (%s:%s) in target structure (%s:%s) by Data-Type ",
                        field.variable(), field.dataType(), targetField.variable(),
                        targetField.dataType()));
            }
        }
        final DotConnect dotConnect = new DotConnect();

        final String updateContentlet = String.format(
                "update contentlet c set structure_inode = ?, \n" +
                        " contentlet_as_json = jsonb_set(contentlet_as_json,'{contentType}', '\"%s\"'::jsonb, false)  \n"
                        +
                        " where c.structure_inode =  ? ", target.inode()
        );

        dotConnect.setSQL(updateContentlet).addParam(target.inode()).addParam(source.inode())
                .loadResult();
    }


    @CloseDBIfOpened
    private int countByType(ContentType type) {
        return FactoryLocator.getContentletFactory().countByType(type, false);
    }

    @CloseDBIfOpened
    Map<String, List<InodeAndLanguage>> nextBatch(final ContentType type, int limit, int offset)
            throws DotDataException {

        final String selectContentlets = " select c.inode,  c.identifier , c.language_id \n"
                + "  from contentlet c \n"
                + "  join structure s on c.structure_inode  = s.inode  \n"
                + "  where s.velocity_var_name = ? order by c.identifier\n";

        final List<Map<String, Object>> list = new DotConnect().setSQL(selectContentlets)
                .addParam(type.variable()).setMaxRows(limit).setStartRow(offset)
                .loadObjectResults();

        return list.stream().collect(
                Collectors.groupingBy(row -> (String) row.get("identifier"), Collectors.mapping(
                        row -> InodeAndLanguage.of((String) row.get("inode"),
                                (Long) row.get("language_id")), Collectors.toList())));
    }



    @Override
    public void tearDown(ContentType type) throws DotDataException, DotSecurityException {
        final long t1 = System.currentTimeMillis();
        final int maxThreads = Config.getIntProperty("CT_DELETE_THREADS", 1);
        final ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
        final CompletionService<Boolean> service = new ExecutorCompletionService<>(pool);
        final long allCount = countByType(type);
        final int limit = Config.getIntProperty("CT_DELETE_BATCH_SIZE", 600);
        Logger.info(getClass(), String.format(
                "There are (%d) contents. Will attack using (%d) batchSize & (%d) threads. ",
                allCount, limit, maxThreads));

        int futures = 0;
        int offset = 0;

        try {
            while (!pool.isTerminated()) {

                Map<String, List<InodeAndLanguage>> batch = nextBatch(type, limit, offset);
                if (batch.isEmpty()) {
                    //We're done lets get out of here
                    Logger.info(getClass(), "We're done collecting batch!");
                    break;
                }

                final List<Map<String, List<InodeAndLanguage>>> partitions = partitionInput(batch,
                        maxThreads);
                Logger.debug(getClass(),
                        String.format(" ::: Partitions size %d ", partitions.size()));

                for (Map<String, List<InodeAndLanguage>> partition : partitions) {
                    service.submit(() -> {
                        try {
                            return destroy(partition, type);
                        } finally {
                            DateUtil.sleep(400);
                        }
                    });
                    futures++;
                }

                offset += limit;
                Logger.debug(getClass(),
                        String.format(" Offset is (%d) of (%d) total. ", offset, allCount));
            }

            for (int i = 0; i < futures; i++) {
                try {
                    service.take().get();
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(getClass(), "Failure calling future ", e);
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            pool.shutdown();
        }

        //Fallback in case something went wrong
        final int failuresCount = countByType(type);
        if (failuresCount > 0) {
            Logger.info(getClass(),
                    String.format(" There were still (%d) that failed getting removed. ",
                            failuresCount));
            //  deleteContentletsByType(type);
        }

        destroy(type);

        final long diff = System.currentTimeMillis() - t1;
        final long hours = TimeUnit.MILLISECONDS.toHours(diff);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        String timeInHHMMSS = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        Logger.info(getClass(),
                String.format(" it took me (%s) to tear down (%d) of CT (%s) ", timeInHHMMSS,
                        allCount, type));
    }

    @Override
    public void sequentialTearDown(ContentType type) throws DotDataException, DotSecurityException {
        final long t1 = System.currentTimeMillis();
        final long allCount = countByType(type);
        final int limit = Config.getIntProperty("CT_DELETE_BATCH_SIZE", 600);
        Logger.info(getClass(), String.format(
                "There are (%d) contents. Will attack using (%d) batchSize ",
                allCount, limit));

        int offset = 0;

        while (true) {

            Map<String, List<InodeAndLanguage>> batch = nextBatch(type, limit, offset);
            if (batch.isEmpty()) {
                //We're done lets get out of here
                Logger.info(getClass(), "We're done collecting batch!");
                break;
            }

            final List<Map<String, List<InodeAndLanguage>>> partitions = partitionInput(batch, 1);
            Logger.debug(getClass(),
                    String.format(" ::: Partitions size %d ", partitions.size()));

            for (Map<String, List<InodeAndLanguage>> partition : partitions) {
                destroy(partition, type);
                Logger.info(getClass(), String.format("Finished destroying a batch of (%d) contentlets!",partition.size()));
            }

            offset += limit;
            Logger.debug(getClass(),
                    String.format(" Offset is (%d) of (%d) total. ", offset, allCount));
        }

        destroy(type);

        final long diff = System.currentTimeMillis() - t1;
        final long hours = TimeUnit.MILLISECONDS.toHours(diff);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        String timeInHHMMSS = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        Logger.info(getClass(),
                String.format(" it took me (%s) to tear down (%d) of CT (%s) ", timeInHHMMSS,
                        allCount, type));
    }

    @WrapInTransaction
    private void destroy(ContentType type) {
        try {
            Logger.info(getClass(),
                    String.format("Destroying Content-Type with inode: [%s] and var: [%s].",
                            type.inode(), type.variable()));

            // make sure folders don't refer to this structure as default fileasset structure

            //updateFolderFileAssetReferences(type);

            // delete container structures
            //APILocator.getContainerAPI().deleteContainerStructureByContentType(type);

            // delete workflow schema references
            //deleteWorkflowSchemeReference(type);

            // remove structure permissions
            //APILocator.getPermissionAPI().removePermissions(type);

            // delete relationships
            //deleteRelationships(type);

            //FactoryLocator.getFieldFactory().deleteByContentType(type);
            // remove structure itself
            DotConnect dc = new DotConnect();
            dc.setSQL(ContentTypeSql.DELETE_TYPE_BY_INODE).addParam(type.id()).loadResult();
            dc.setSQL(ContentTypeSql.DELETE_INODE_BY_INODE).addParam(type.id()).loadResult();
            Logger.info(getClass(),
                    String.format("We're done with Content-Type [%s],[%s].", type.inode(),
                            type.variable()));

            CacheLocator.getContentTypeCache2().remove(type);

            //HibernateUtil.addCommitListener(()-> localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type.variable())));

        } catch (DotDataException e) {
            Logger.error(getClass(),
                    String.format("Error Removing CT [%s],[%s].", type.inode(), type.variable()),
                    e);
        }

    }

    /**
     * @param partition
     * @param type
     * @return
     */
    boolean destroy(final Map<String, List<InodeAndLanguage>> partition, ContentType type) {

        final User user = APILocator.systemUser();
        partition.forEach((identifier, inodeAndLanguages) -> {
            final List<Contentlet> contentlets = makeContentlets(identifier, inodeAndLanguages, type);
            destroy(contentlets, user);
        });

        return true;
    }


    @WrapInTransaction
    void destroy(List<Contentlet> contentlets, User user) {
        try {
            APILocator.getContentletAPI().destroy(contentlets, user, false);
        } catch (DotDataException | DotSecurityException e) {
           Logger.error(this, "Error destroying contents", e);
        }

/*
        for (Contentlet contentlet : contentlets) 
            try {
                destroy(contentlet, user);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error destroying contents", e);
            }
        }
 */
    }

    void destroy(Contentlet contentlet, User user) throws DotDataException, DotSecurityException {
         destroyRules(contentlet, user);
         destroyCategories(contentlet, user);
         destroyRelationships(contentlet, user);
         destroyMultiTree(contentlet);
      //  deleteVersions(contentlet);
      //  deleteBinaries(contentlet);
      //  deleteElementsFromPublishQueueTable(contentlet);
      //  destroyMetadata(contentlet);
    }

    void destroyRules(Contentlet contentlet, User user) throws  DotDataException, DotSecurityException{
        try {
            APILocator.getRulesAPI()
                    .deleteRulesByParent(contentlet, user, false);
        } catch (InvalidLicenseException  ile) {
            Logger.warn(this, "An enterprise license is required to delete rules under pages.");
        }
    }

    void destroyCategories(Contentlet contentlet, User user)
            throws DotDataException, DotSecurityException {
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
            } catch (DotStateException dcse) {
                Logger.warn(this.getClass(), "Page with id:" + pageIdentifier.getId() + " does not exist");
            }
        }
    }


    List<Contentlet> makeContentlets(final String identifier,
            final List<InodeAndLanguage> inodeAndLanguages, final ContentType type) {
        return inodeAndLanguages.stream().map(inodeAndLanguage -> {
            final Contentlet contentlet = new Contentlet();
            contentlet.setInode(inodeAndLanguage.getInode());
            contentlet.setIdentifier(identifier);
            contentlet.setLanguageId(inodeAndLanguage.getLanguageId());
            contentlet.setContentTypeId(type.id());
            return contentlet;
        }).collect(Collectors.toList());
    }


    /**
     * This method takes an input of contentlet and partitions it into lists that can be used to
     * feed threads as their assigned workload
     *
     * @param contents
     * @param maxThreads
     * @return
     */
    private List<Map<String, List<InodeAndLanguage>>> partitionInput(
            Map<String, List<InodeAndLanguage>> contents, final int maxThreads) {

        final int partitionSize = Math
                .max((contents.size() / maxThreads), 10);

        Logger.info(getClass(),
                String.format(
                        "Number of threads is limited to (%d). Number of Contentlets to process is (%d). Load will be distributed in groups of (%d) ",
                        maxThreads, contents.size(),
                        partitionSize)
        );

        return Lists.partition(new ArrayList<>(contents.entrySet()), partitionSize).stream().map(
                partition -> partition.stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ).collect(Collectors.toList());

    }

    static class InodeAndLanguage {

        final String inode;
        final long languageId;

        private InodeAndLanguage(final String inode, final long languageId) {
            this.inode = inode;
            this.languageId = languageId;
        }

        public static InodeAndLanguage of(final String inode, final long languageId) {
            return new InodeAndLanguage(inode, languageId);
        }

        @Override
        public String toString() {
            return "InodeAndLanguage [inode=" + inode + ", languageId=" + languageId + "]";
        }

        public String getInode() {
            return inode;
        }

        public long getLanguageId() {
            return languageId;
        }

    }


}
