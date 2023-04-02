package com.dotcms.contenttype.business;

import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletDestroyDelegate;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ContentTypeDestroyAPIImpl implements ContentTypeDestroyAPI {

    /**
     * This method relocates all contentlets from a given structure to another structure And returns
     * the new structure purged from fields
     *
     * @param source
     * @param target
     * @throws DotDataException
     */
    @WrapInTransaction
    public void relocateContentletsForDeletion(final ContentType source, final ContentType target) throws DotDataException {

        if (!source.getClass().equals(target.getClass())) {
            throw new DotDataException(
                    String.format(
                            "Incompatible source and target ContentTypes. source class is (%s) target class is (%s)",
                            source.getClass().getSimpleName(), target.getClass().getSimpleName()
                    )
            );
        }

        final DotConnect dotConnect = new DotConnect();

        final String selectContentlets = "select c.inode from contentlet c where c.structure_inode =  ? \n";
        dotConnect.setSQL(selectContentlets).addParam(source.inode());

        final String updateContentlet = String.format(
                "update contentlet c set structure_inode = ?, \n" +
                        " contentlet_as_json = jsonb_set(contentlet_as_json,'{contentType}', '\"%s\"'::jsonb, false)  \n" +
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
    Map<String, List<ContentletVersionInfo>> nextBatch(final ContentType type, int limit, int offset)
            throws DotDataException {

        final String selectContentlets = " select c.inode,  c.identifier , c.language_id, i.host_inode, \n"
                + " (  \n"
                + "  select coalesce(f.inode,'SYSTEM_HOST') as folder_inode from folder f, identifier id where f.identifier = id.id and id.asset_type = 'folder' and id.full_path_lc || '/' = i.parent_path  \n"
                + ") \n"
                + "  from contentlet c \n"
                + "  join structure s on c.structure_inode  = s.inode  \n"
                + "  join identifier i on c.identifier = i.id \n"
                + "  where s.velocity_var_name = ? order by c.identifier \n";

        final List<Map<String, Object>> list = new DotConnect().setSQL(selectContentlets)
                .addParam(type.variable()).setMaxRows(limit).setStartRow(offset)
                .loadObjectResults();

        return list.stream().collect(
                Collectors.groupingBy(row -> (String) row.get("identifier"), Collectors.mapping(
                        row -> ContentletVersionInfo.of(
                                (String) row.get("inode"),
                                (Long) row.get("language_id"),
                                (String) row.get("host_inode"),
                                (String) row.get("folder_inode")
                        ), Collectors.toList())));
    }


    @Override
    public void destroy(ContentType type, User user) throws DotDataException, DotSecurityException {
        final long t1 = System.currentTimeMillis();
        final long allCount = countByType(type);
        final int limit = Config.getIntProperty("CT_DELETE_BATCH_SIZE", 600);
        final int partitionSize = Config.getIntProperty("CT_DELETE_PARTITION_SIZE", 1);
        Logger.info(getClass(), String.format(
                "There are (%d) contents. Will remove then sequentially using (%d) batchSize ",
                allCount, limit));

        int offset = 0;

        while (true) {

            Map<String, List<ContentletVersionInfo>> batch = nextBatch(type, limit, offset);
            if (batch.isEmpty()) {
                //We're done lets get out of here
                Logger.info(getClass(), "We're done collecting batch!");
                break;
            }

            final List<Map<String, List<ContentletVersionInfo>>> partitions = partitionInput(batch, partitionSize);
            Logger.debug(getClass(),
                    String.format(" ::: Partitions size %d ", partitions.size()));
                for (Map<String, List<ContentletVersionInfo>> partition : partitions) {
                    destroy(partition, type);
                    Logger.info(getClass(),
                            String.format("Finished destroying a batch of (%d) contentlets!",
                                    partition.size()));
                }

            offset += limit;
            Logger.debug(getClass(),
                    String.format(" Offset is (%d) of (%d) total. ", offset, allCount));
        }

        internalDestroy(type);

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
    private void internalDestroy(ContentType type) throws DotDataException{
        try {
            Logger.info(getClass(),
                    String.format("Destroying Content-Type with inode: [%s] and var: [%s].",
                            type.inode(), type.variable()));

            final ContentTypeFactory contentTypeFactory = FactoryLocator.getContentTypeFactory();
            contentTypeFactory.delete(type);
            CacheLocator.getContentTypeCache2().remove(type);

            fireDeleteEvent(type, APILocator.systemUser());

        } catch (DotDataException e) {
            Logger.error(getClass(),
                    String.format("Error Removing CT [%s],[%s].", type.inode(), type.variable()),
                    e);
            throw new DotDataException(e);
        }

    }

    /**
     *
     * @param type
     * @param user
     * @throws DotHibernateException
     */
    void fireDeleteEvent(ContentType type, User user) throws DotHibernateException {
        try {
            final String actionUrl = ContentTypeUtil.getInstance().getActionUrl(type, user);
            ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper(actionUrl, type);
            APILocator.getSystemEventsAPI().pushAsync(SystemEventType.DELETE_BASE_CONTENT_TYPE, new Payload(
                    contentTypePayloadDataWrapper, Visibility.PERMISSION, String.valueOf(
                    PermissionAPI.PERMISSION_READ)));
        } catch (DotStateException | DotDataException e) {
            Logger.error(ContentType.class, e.getMessage(), e);
            throw new BaseRuntimeInternationalizationException(e);
        }
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        HibernateUtil.addCommitListener(() -> localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type.variable())));
    }

    Lazy<ContentletDestroyDelegate> delegate = Lazy.of(ContentletDestroyDelegate::newInstance);

    /**
     * @param partition
     * @param type
     */
    void destroy(final Map<String, List<ContentletVersionInfo>> partition, ContentType type) {
        final User user = APILocator.systemUser();
        partition.forEach((identifier, inodeAndLanguages) -> {
            final List<Contentlet> contentlets = makeContentlets(identifier, inodeAndLanguages, type);
            delegate.get().destroy(contentlets, user);
        });
    }

    List<Contentlet> makeContentlets(final String identifier,
            final List<ContentletVersionInfo> contentletVersionInfos, final ContentType type) {
        return contentletVersionInfos.stream().map(contentletVersionInfo -> {
            final Contentlet contentlet = new Contentlet();
            contentlet.setInode(contentletVersionInfo.getInode());
            contentlet.setIdentifier(identifier);
            contentlet.setLanguageId(contentletVersionInfo.getLanguageId());
            contentlet.setHost(contentletVersionInfo.getHostInode());
            contentlet.setFolder(contentletVersionInfo.getFolderInode());
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
    private List<Map<String, List<ContentletVersionInfo>>> partitionInput(
            Map<String, List<ContentletVersionInfo>> contents, final int maxThreads) {

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

    static class ContentletVersionInfo {
        final String inode;
        final Long languageId;

        final String hostInode;

        final String folderInode;

        private ContentletVersionInfo(final String inode, final Long languageId, final String hostInode, final String folderInode) {
            this.inode = inode;
            this.languageId = languageId;
            this.hostInode = hostInode;
            this.folderInode = folderInode;
        }

        public static ContentletVersionInfo of(final String inode, final long languageId, final String hostId, final String folderId) {
            return new ContentletVersionInfo(inode, languageId, hostId, folderId);
        }

        @Override
        public String toString() {
            return "ContentletVersionInfo{" +
                    "inode='" + inode + '\'' +
                    ", languageId=" + languageId +
                    ", hostInode='" + hostInode + '\'' +
                    ", folderInode='" + folderInode + '\'' +
                    '}';
        }

        public String getInode() {
            return inode;
        }

        public Long getLanguageId() {
            return languageId;
        }

        public String getHostInode() {
            return hostInode;
        }
        public String getFolderInode() {
            return null == folderInode ? Host.SYSTEM_HOST : folderInode;
        }

    }


}
