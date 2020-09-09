package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
<<<<<<< HEAD
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
=======
>>>>>>> 3e7f99f163... #18554: Introducing IntegritiGenerationDataJob to support multi node receivers creation of file and query process for status
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
<<<<<<< HEAD
import org.quartz.Trigger;
=======
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
>>>>>>> 3e7f99f163... #18554: Introducing IntegritiGenerationDataJob to support multi node receivers creation of file and query process for status

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Stateful job used to remove content type field references before its deletion
 * @author nollymar
 */
public class CleanUpFieldReferencesJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {
        final Map<String, Serializable> executionData;
        final JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();

        if (jobDataMap.containsKey(EXECUTION_DATA)) {
          //This bit is here to continue to support the integration-tests
            executionData = (Map<String, Serializable>) jobDataMap.get(EXECUTION_DATA);
        } else {
          //But the `executionData` must be grabbed frm the persisted job detail. Through the trigger name.
            final Trigger trigger = jobContext.getTrigger();
            executionData = getExecutionData(trigger, CleanUpFieldReferencesJob.class);
        }

        final User user = (User) executionData.get("user");
        final Field field = (Field) executionData.get("field");
        final Date deletionDate = (Date) executionData.get("deletionDate");

<<<<<<< HEAD
        Logger.info(CleanUpFieldReferencesJob.class,String.format("CleanUpFieldReferencesJob ::: started for field `%s`.",field.variable()));

=======
        final User user = (User)jobContext.getJobDetail().getJobDataMap().get("user");
        final Field field = (Field) jobContext.getJobDetail().getJobDataMap().get("field");
        final Date deletionDate = (Date) jobContext.getJobDetail().getJobDataMap().get("deletionDate");
>>>>>>> 3e7f99f163... #18554: Introducing IntegritiGenerationDataJob to support multi node receivers creation of file and query process for status
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final UserAPI userAPI = APILocator.getUserAPI();

        try {
            final ContentType type = contentTypeAPI.find(field.contentTypeId());

            final Structure structure = new StructureTransformer(type).asStructure();

            com.dotmarketing.portlets.structure.model.Field legacyField = new LegacyFieldTransformer(field).asOldField();

            if (!(field instanceof CategoryField) &&
                    !(field instanceof ConstantField) &&
                    !(field instanceof HiddenField) &&
                    !(field instanceof LineDividerField) &&
                    !(field instanceof TabDividerField) &&
                    !(field instanceof RelationshipsTabField) &&
                    !(field instanceof RelationshipField) &&
                    !(field instanceof PermissionTabField) &&
                    !(field instanceof HostFolderField) &&
                    structure != null
            ) {

                contentletAPI.cleanField(structure, deletionDate, legacyField, userAPI.getSystemUser(), false);

            }

            //Refreshing permissions
            if (field instanceof HostFolderField) {
                try {
                    contentletAPI.cleanHostField(structure, userAPI.getSystemUser(), false);
                } catch(DotMappingException e) {}

                permissionAPI.resetChildrenPermissionReferences(structure);
            }

            // remove the file from the cache
            new ContentletLoader().invalidate(structure);
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(CleanUpFieldReferencesJob.class,
                    "Error cleaning up field references. Field velocity var: " + field.variable(), e);
        }
        Logger.info(CleanUpFieldReferencesJob.class,String.format("CleanUpFieldReferencesJob ::: finished for field `%s`.",field.variable()));

    }

    public static void triggerCleanUpJob(final Field field, final User user) {
<<<<<<< HEAD

        final Map<String, Serializable> nextExecutionData = ImmutableMap
                .of("field", field,
                        "deletionDate", Calendar.getInstance().getTime(),
                        "user", user);
=======
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("field", field);
        jobDataMap.put("deletionDate", Calendar.getInstance().getTime());
        jobDataMap.put("user", user);

        final String randomID = UUID.randomUUID().toString();
>>>>>>> 3e7f99f163... #18554: Introducing IntegritiGenerationDataJob to support multi node receivers creation of file and query process for status

        HibernateUtil.addCommitListenerNoThrow(Sneaky.sneaked(() -> {
                    enqueueTrigger(nextExecutionData, CleanUpFieldReferencesJob.class);
                }
        ));
    }

}
