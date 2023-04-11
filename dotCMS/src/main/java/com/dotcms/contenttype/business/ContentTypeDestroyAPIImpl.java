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
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.ContentTypeUtil;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.LogTime;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
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
import java.util.stream.Collectors;

public class ContentTypeDestroyAPIImpl implements ContentTypeDestroyAPI {

    Lazy<ContentletDestroyDelegate> delegate = Lazy.of(ContentletDestroyDelegate::newInstance);

    Lazy<Integer> limitProp = Lazy.of(()->Config.getIntProperty("CT_DELETE_BATCH_SIZE", 100));

    Lazy<Integer> partitionSizeProp = Lazy.of(()-> Config.getIntProperty("CT_DELETE_PARTITION_SIZE", 1));

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

        String updateContentlet = String.format(
                "update contentlet c set structure_inode = ?, \n" +
                        " contentlet_as_json = jsonb_set(contentlet_as_json,'{contentType}', '\"%s\"'::jsonb, false)  \n" +
                        " where c.structure_inode =  ? ", target.inode()
        );

        if(DbConnectionFactory.isMsSql()){
            updateContentlet =  String.format(
                    "update contentlet set structure_inode = ?, \n" +
                            " contentlet_as_json = json_modify(contentlet_as_json,'$.contentType', '%s')  \n" +
                            " where structure_inode =  ? ", target.inode());
        }

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

        String selectContentlets = " select c.inode,  c.identifier , c.language_id, i.host_inode, \n"
                + " (  \n"
                + "  select coalesce(f.inode,'SYSTEM_HOST') as folder_inode from folder f, identifier id where f.identifier = id.id and id.asset_type = 'folder' and id.full_path_lc || '/' = i.parent_path  \n"
                + ") \n"
                + "  from contentlet c \n"
                + "  join structure s on c.structure_inode  = s.inode  \n"
                + "  join identifier i on c.identifier = i.id \n"
                + "  where s.velocity_var_name = ? order by c.identifier \n";

        if(DbConnectionFactory.isMsSql()){
            selectContentlets = " select c.inode,  c.identifier , c.language_id, i.host_inode, \n"
                    + " (  \n"
                    + "  select coalesce(f.inode,'SYSTEM_HOST') as folder_inode from folder f, identifier id where f.identifier = id.id and id.asset_type = 'folder' and concat(id.full_path_lc , '/') = i.parent_path  \n"
                    + ") \n"
                    + "  from contentlet c \n"
                    + "  join structure s on c.structure_inode  = s.inode  \n"
                    + "  join identifier i on c.identifier = i.id \n"
                    + "  where s.velocity_var_name = ? order by c.identifier \n";
        }

        final List<Map<String, Object>> list = new DotConnect().setSQL(selectContentlets)
                .addParam(type.variable()).setMaxRows(limit).setStartRow(offset)
                .loadObjectResults();

        return list.stream().collect(
                Collectors.groupingBy(row -> (String) row.get("identifier"), Collectors.mapping(
                        row -> ContentletVersionInfo.of(
                                (String) row.get("inode"),
                                (Number) row.get("language_id"),
                                (String) row.get("host_inode"),
                                (String) row.get("folder_inode")
                        ), Collectors.toList())));
    }


    @LogTime(loggingLevel = "INFO")
    @Override
    public void destroy(ContentType type, User user) throws DotDataException, DotSecurityException {
        final long allCount = countByType(type);
        final int limit = limitProp.get();
        final int partitionSize = partitionSizeProp.get();
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

        Logger.info(getClass(),
                String.format(" I'm done destroying (%d) pieces of Content of type (%s). ", allCount, type));
    }

    /**
     * This the only method meant to be transaction here. We want to make sure we address transactional smaller batches of contentlets.
     * @param type
     * @throws DotDataException
     */
    @WrapInTransaction
    private void internalDestroy(ContentType type) throws DotDataException{
        try {
            Logger.info(getClass(),
                    String.format("Destroying Content-Type with inode: [%s] and var: [%s].",
                            type.inode(), type.variable()));

            final ContentTypeFactory contentTypeFactory = FactoryLocator.getContentTypeFactory();
            contentTypeFactory.delete(type);
            CacheLocator.getContentTypeCache2().remove(type);
            HibernateUtil.addCommitListener(()-> broadcastEvents(type, APILocator.systemUser()));

        } catch (DotDataException e) {
            Logger.error(getClass(),
                    String.format("Error Removing CT [%s],[%s].", type.inode(), type.variable()),
                    e);
            throw new DotDataException(e);
        }

    }

    /**
     * Broadcasts the events to the system events API and the local system events API.
     * @param type
     * @param user
     */
    void broadcastEvents(final ContentType type, final User user)  {
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
        localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type.variable()));
        notifyContentTypeDestroyed(type);
    }


    /**
     * Middle man that basically calls the delegate
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

    /**
     * Since all contetlets were moved under a disposable field-less content type, we need to create a list of contentlets including all the basic information
     * @param identifier
     * @param contentletVersionInfos
     * @param type
     * @return
     */
    List<Contentlet> makeContentlets(final String identifier,
            final List<ContentletVersionInfo> contentletVersionInfos, final ContentType type) {
        return contentletVersionInfos.stream().map(contentletVersionInfo -> {
            final Contentlet contentlet = new Contentlet();
            contentlet.setInode(contentletVersionInfo.getInode());
            contentlet.setIdentifier(identifier);
            contentlet.setLanguageId(contentletVersionInfo.getLanguageId().longValue());
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

    /**
     * Contentlet and version info DTO
     */
    static class ContentletVersionInfo {
        final String inode;
        final Number languageId;

        final String hostInode;

        final String folderInode;

        private ContentletVersionInfo(final String inode, final Number languageId, final String hostInode, final String folderInode) {
            this.inode = inode;
            this.languageId = languageId;
            this.hostInode = hostInode;
            this.folderInode = folderInode;
        }

        public static ContentletVersionInfo of(final String inode, final Number languageId, final String hostId, final String folderId) {
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

        public Number getLanguageId() {
            return languageId;
        }

        public String getHostInode() {
            return hostInode;
        }
        public String getFolderInode() {
            return null == folderInode ? Host.SYSTEM_HOST : folderInode;
        }

    }

    /**
     * Send out a system-wide notification that a content type has been destroyed
     * @param contentType
     */
    private void notifyContentTypeDestroyed(final ContentType contentType) {
        try {
            final NotificationAPI notificationAPI = APILocator.getNotificationAPI();
            // Search for the CMS Admin role and System User
            final Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
            final User systemUser = APILocator.systemUser();

            notificationAPI.generateNotification(
                    new I18NMessage("contenttype.destroy.complete.title"),
                    new I18NMessage("contenttype.destroy.complete.message", null,
                            contentType.name()), null,
                    // no actions
                    NotificationLevel.WARNING, NotificationType.GENERIC,
                    Visibility.ROLE, cmsAdminRole.getId(), systemUser.getUserId(),
                    systemUser.getLocale()
            );
        } catch (DotDataException e) {
            Logger.error(getClass(), String.format("Failed sending out notification for CT [%s]",contentType.name()), e);
        }
    }


}
