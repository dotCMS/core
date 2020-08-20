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
import com.dotcms.scheduler.DotSyncronizedTask;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Date;

/**
 * Stateful job used to remove content type field references before its deletion
 * @author nollymar
 */
public class CleanUpFieldReferencesJob extends DotSyncronizedTask {

    final User user;
    final Field field;
    final Date deletionDate;
    
    CleanUpFieldReferencesJob(User user, Field field){
        this.user=user;
        this.field=field;
        this.deletionDate=new Date();
        
        
    }
    
    
    @Override
    @WrapInTransaction
    public void execute()  {

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
    }

    public static void triggerCleanUpJob(final Field field, final User user) {

        CleanUpFieldReferencesJob task = new CleanUpFieldReferencesJob(user, field);
        
        APILocator.getSchedulerAPI().scheduleOneTimeTask(task);
    }
}
