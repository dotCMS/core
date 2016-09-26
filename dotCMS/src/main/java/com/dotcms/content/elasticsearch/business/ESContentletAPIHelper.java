/**
 *
 */
package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.business.DotMappingException;
import com.dotcms.enterprise.cmis.QueryResult;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.com.google.gson.Gson;
import com.dotcms.repackage.com.google.gson.GsonBuilder;
import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.jboss.util.Strings;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.*;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.*;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletAndBinary;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.*;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.BeanUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper for {@link ESContentletAPIImpl}
 *
 * @author jsanca
 * @since 1.5
 *
 */
public class ESContentletAPIHelper implements Serializable {


    public final static ESContentletAPIHelper INSTANCE =
            new ESContentletAPIHelper();

    private ESContentletAPIHelper() {}

    /**
     * Generate the Notification for the can not delete scenario on the {@link ESContentletAPIImpl}
     * @param notfAPI
     * @param userLocale
     * @param userId
     * @param iNode
     */
    public final void generateNotificationCanNotDelete (final NotificationAPI notfAPI,
                                                  final Locale userLocale,
                                                  final String userId,
                                                  final String iNode) {

        final Locale locale = (null != userLocale)?
                userLocale: Locale.getDefault();
        String errorMsg = StringUtils.EMPTY;

        try {

            errorMsg = LanguageUtil.get(locale,
                    "notification.escontentelet.cannotdelete.info.message",
                    iNode); // message = Contentlet with Inode {0} cannot be deleted because it's not archived. Please archive it first before deleting it.

            Logger.error(this, errorMsg);

            notfAPI.generateNotification(
                    LanguageUtil.get(locale, "notification.escontentelet.cannotdelete.info.title"), // title = Contentlet Notification
                    errorMsg,
                    null, // no actions
                    NotificationLevel.ERROR,
                    NotificationType.GENERIC,
                    userId,
                    locale
            );
        } catch (LanguageException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }

        throw new DotStateException(errorMsg);
    } // generateNotificationCanNotDelete.

} // E:O:F:ESContentletAPIHelper.
