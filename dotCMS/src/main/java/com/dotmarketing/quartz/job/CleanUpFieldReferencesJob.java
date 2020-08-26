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
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

/**
 * Stateful job used to remove content type field references before its deletion
 * @author nollymar
 */
public class CleanUpFieldReferencesJob extends DotStatefulJob {

    @Override
    @WrapInTransaction
    public void run(final JobExecutionContext jobContext) throws JobExecutionException {

        final Trigger trigger = jobContext.getTrigger();
        final Optional<Map<String, Object>> triggerJobDetailOptional = getTriggerJobDetail(CleanUpFieldReferencesJob.class);
        if(!triggerJobDetailOptional.isPresent()){
            throw new IllegalArgumentException(
                    String.format("Unable to get job detail data %s ", trigger.getName()));
        }
            final Map<String, Object> triggerJobDetail = triggerJobDetailOptional.get();
            @SuppressWarnings("unchecked")
            final Map<String, Serializable> executionData = (Map<String, Serializable>) triggerJobDetail.get(trigger.getName());
            if(null == executionData) {
                throw new IllegalArgumentException(
                        String.format("Unable to get trigger execution data for trigger `%s` ", trigger.getName()));
            }

        final User user = (User)executionData.get("user");
        final Field field = (Field) executionData.get("field");
        final Date deletionDate = (Date) executionData.get("deletionDate");

        System.out.println(":::Job-Started::Thread:" + Thread.currentThread().getName() +"::"+field.name());
/*
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
*/

        try {
            Thread.sleep(25000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("::: done!");
    }

    public static void triggerCleanUpJob(final Field field, final User user) {

        final Map<String, Serializable> nextExecutionData = ImmutableMap
                .of("field", field,
                        "deletionDate", Calendar.getInstance().getTime(),
                        "user", user);

        HibernateUtil.addCommitListenerNoThrow(Sneaky.sneaked(() -> {
                    enqueueTrigger(nextExecutionData, CleanUpFieldReferencesJob.class);
                }
        ));
    }
}
