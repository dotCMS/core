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
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class implements the {@link ContentTypeDestroyAPI} for information stored in a database.
 *
 * @author Fabrizzio Araya
 * @since Apr 10th, 2023
 */
public class ContentTypeDestroyAPIImpl implements ContentTypeDestroyAPI {

    /**
     * Delegate class to handle the contentlet deletion
     */
    Lazy<ContentletDestroyDelegate> delegate = Lazy.of(ContentletDestroyDelegate::newInstance);

    /**
     * Lazy property that says how many contentlets should be deleted per transaction
     */
    Lazy<Integer> pullLimitProp = Lazy.of(()->Config.getIntProperty("CT_DELETE_BATCH_SIZE", 100));

    /**
     * Lazy property that says how many contentlets should be relocated per transaction
     */
    Lazy<Integer> relocateLimitProp = Lazy.of(()->Config.getIntProperty("CT_RELOCATE_BATCH_SIZE", 500));


    private String updateStatement(final ContentType target, final String tempTableName, final int from, final int to ) {
        return DbConnectionFactory.isPostgres() ?
                String.format(
                        "update contentlet c set structure_inode = '%s', \n" +
                                " contentlet_as_json = jsonb_set(contentlet_as_json,'{contentType}', '\"%s\"'::jsonb, false)  \n"  +
                                " from %s ctu \n" +
                                " where ctu.inode = c.inode and ctu.row_num >= %d and ctu.row_num <= %d \n"
                                , target.inode(), target.inode(), tempTableName, from, to )
                :
                        String.format(
                                "update contentlet set structure_inode = ?, \n" +
                                        " contentlet_as_json = json_modify(contentlet_as_json,'$.contentType', '%s')  \n" +
                                        " where  c.inode in ( %s ) \n"
                                        , target.inode(), "");
    }

    /**
     * This method creates a temp table with the inodes of the contentlets to be relocated
     * @param tempTableName
     * @param source
     * @return
     */
    private String dumpTableScript(final String tempTableName, final ContentType source){
        return DbConnectionFactory.isPostgres() ?
                String.format(
                " CREATE TEMP TABLE %s AS\n"
                        + " SELECT ROW_NUMBER() OVER(ORDER BY inode) row_num, inode\n"
                        + " FROM contentlet c where c.structure_inode = '%s' ;\n"
                        + " CREATE index ON %s(row_num); "
                        + " CREATE index ON %s(inode); "
                , tempTableName
                , source.inode()
                , tempTableName
                , tempTableName
        )
                :
                        String.format(
                                         " SELECT ROW_NUMBER () OVER(ORDER BY inode) row_num , inode INTO %s\n"
                                        + " FROM contentlet c where c.structure_inode = '%s' ;\n"
                                        + " CREATE index idx_row_%s ON %s(row_num); "
                                        + " CREATE index idx_inode_%s ON %s(inode); "
                                , tempTableName
                                , source.inode()
                                , tempTableName
                                , tempTableName
                                , tempTableName
                                , tempTableName
                        );
    }

    /**
     * This method relocates all contentlets from a given structure to another structure
     * @param source the structure to be deleted
     * @param target the structure to be used as temp replacement
     * @return the number of contentlets relocated
     * @throws DotDataException
     */

    @WrapInTransaction
    public int relocateContentletsForDeletion(final ContentType source, final ContentType target)
            throws DotDataException {

        if (!source.getClass().equals(target.getClass())) {
            throw new DotDataException(
                    String.format(
                            "Incompatible source and target ContentTypes. source class is (%s) target class is (%s)",
                            source.getClass().getSimpleName(), target.getClass().getSimpleName()
                    )
            );
        }

        final String tempTableName = String.format("contentlets_to_be_updated_%d", System.currentTimeMillis());

        //The quickest way to relocate contentlets is to create a temp table with the inodes of the contentlets to be relocated
        final String dumpContentletToUpdate = dumpTableScript(tempTableName, source);

        Try.of(()->new DotConnect().executeStatement(dumpContentletToUpdate)).onFailure(e->{
            Logger.error(this.getClass(), "Error trying to create temp table to update contentlets", e);
            throw new DotStateException(e);
        });

        final int allCount = new DotConnect().setSQL(String.format("select count(*) as count from %s", tempTableName)).getInt("count");

        int limit = relocateLimitProp.get();
        int from = 0;
        int to = limit;

        int times = allCount <= limit ?  1 : (int)Math.ceil(allCount / (float)limit);

        Logger.debug(this.getClass(),
                String.format("Relocating (%d) contentlets from (%s) to (%s) in (%d) transactions..", allCount, source.name(), target.name(), times));

        for (int i = 0; i < times; i++) {

            Logger.debug(this.getClass(),
                    String.format("Relocated from (%d) to (%d) of (%d) contentlets..", from, to, allCount));

            final String updateStatement = updateStatement(target, tempTableName, from, to);
            final DotConnect updateConnect = new DotConnect().setSQL(updateStatement);
            updateConnect.loadResults();

            from += limit;
            to += limit;

            if(i % 100 == 0){
                Logger.info(this.getClass(),
                        String.format("Relocated from (%d) to (%d) out of (%d) contentlets..", from, to, allCount));
            }

        }

        return allCount;
    }


    @CloseDBIfOpened
    private int countByType(final ContentType type) {
        return FactoryLocator.getContentletFactory().countByType(type, true);
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


    /**
     * Deletes absolutely all contents of the specified Content Type. It is meant to operate
     * destroying <b>small batches of contentlets transactionally every time</b>.
     * <p>It's very important t note that this method isn't meant to be transactional. So, <b>DO NOT
     * ADD</b> any {@link WrapInTransaction} annotation here.</p>
     *
     * @param type The {@link ContentType} to be destroyed.
     * @param user The {@link User} performing the operation.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified user does not have the necessary permissions to
     *                              perform the operation.
     */
    @Override
    public void destroy(final ContentType type, final User user) throws DotDataException, DotSecurityException {
        final long t1 = System.currentTimeMillis();
        final long allCount = countByType(type);
        final int limit = pullLimitProp.get();
        if (allCount > 0) {
            Logger.info(getClass(), String.format(
                    "There are (%d) contents of type (%s). Will remove them sequentially using (%d) batchSize ",
                    allCount, type.name(), limit));

            int offset = 0;

            while (true) {

                Map<String, List<ContentletVersionInfo>> batch = nextBatch(type, limit, offset);
                if (batch.isEmpty()) {
                    //We're done lets get out of here
                    Logger.debug(getClass(), "We're done collecting contentlets to destroy.");
                    break;
                }

                Logger.debug(getClass(), String.format(" For (%s) This batch is (%d) Big. ", type.name(), batch.size()));

                batch.forEach((identifier, versions) -> destroy(identifier, versions, type, user));

                offset += limit;
                Logger.debug(getClass(),
                        String.format(" Offset is (%d) of (%d) total. ", offset, allCount));
            }

            final long leftovers = countByType(type);
            Logger.info(getClass(), String.format(" :: for (%s) allCount is (%d) with leftovers (%d) :: ", type.name(), allCount, leftovers));
        } else {
            Logger.info(getClass(), String.format("There are no contents of type '%s' to be deleted", type.name()));
        }
        internalDestroy(type);

        //Some custom formatted stats logging
        final long diff = System.currentTimeMillis() - t1;
        final long hours = TimeUnit.MILLISECONDS.toHours(diff);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        final String timeInHHMMSS = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        Logger.info(getClass(), String.format("Finished destroying '%d' contents of type '%s'. Processing time: '%s' ",
                allCount, type.variable(), timeInHHMMSS));
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
        localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type));
        notifyContentTypeDestroyed(type);
    }


    /**
     * Middle man that basically calls the delegate
     * @param identifier
     * @param inodeAndLanguages
     * @param type
     */
    void destroy(final String identifier, final List<ContentletVersionInfo> inodeAndLanguages, final ContentType type, final User user) {
        Logger.info(getClass(), String.format(" For (%s) Destroying (%d) contentlets for identifier (%s) ", type.name(), inodeAndLanguages.size(), identifier));
        final List<Contentlet> contentlets = makeContentlets(identifier, inodeAndLanguages, type);
        delegate.get().destroy(contentlets, user);
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
     * @return
     */
    private List<Map<String, List<ContentletVersionInfo>>> partitionInput(
            Map<String, List<ContentletVersionInfo>> contents) {

        return Lists.partition(new ArrayList<>(contents.entrySet()), 1).stream().map(
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
