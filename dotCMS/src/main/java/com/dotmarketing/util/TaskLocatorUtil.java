package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.fixtask.tasks.*;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.startup.runalways.Task00009ClusterInitialize;
import com.dotmarketing.startup.runalways.Task00010CheckAnonymousUser;
import com.dotmarketing.startup.runalways.Task00003CreateSystemRoles;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.dotmarketing.startup.runalways.Task00005LoadFixassets;
import com.dotmarketing.startup.runalways.Task00006CreateSystemLayout;
import com.dotmarketing.startup.runalways.Task00007RemoveSitesearchQuartzJob;

import com.dotmarketing.startup.runonce.*;

/**
 * This utility class provides access to the lists of dotCMS tasks that are
 * meant for the initialization of a fresh dotCMS install, for fixing data
 * inconsistencies, and the database upgrade process usually associated with a
 * new system version.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class TaskLocatorUtil {

	/**
	 * Returns the list of tasks that are run to solve internal conflicts
	 * related to data inconsistency.
	 * 
	 * @return The list of Fix Tasks.
	 */
    
    private static List<Class<?>> systemfixTasks = ImmutableList.of(
            FixTask00001CheckAssetsMissingIdentifiers.class,
            FixTask00003CheckContainersInconsistencies.class,
            FixTask00004CheckFileAssetsInconsistencies.class,
            FixTask00005CheckHTMLPagesInconsistencies.class,
            FixTask00006CheckLinksInconsistencies.class,
            FixTask00007CheckTemplatesInconsistencies.class,
            FixTask00008CheckTreeInconsistencies.class,
            FixTask00009CheckContentletsInexistentInodes.class,
            FixTask00011RenameHostInFieldVariableName.class,
            FixTask00012UpdateAssetsHosts.class,
            FixTask00015FixAssetTypesInIdentifiers.class,
            FixTask00020DeleteOrphanedIdentifiers.class,
            FixTask00030DeleteOrphanedAssets.class,
            FixTask00040CheckFileAssetsMimeType.class,
            FixTask00050FixInodesWithoutContentlets.class,
            FixTask00060FixAssetType.class,
            FixTask00070FixVersionInfo.class,
            FixTask00080DeleteOrphanedContentTypeFields.class,
            FixTask00090RecreateMissingFoldersInParentPath.class
        );
            
    private static List<Class<?>> userfixTasks = new CopyOnWriteArrayList<>();
            
    public static void addFixTask(Class clazz){
        userfixTasks.add(clazz);
    }
            
    public static void removeFixTask(Class clazz){
        userfixTasks.remove(clazz);
    }    
    
    
	public static List<Class<?>> getFixTaskClasses() {
	    List<Class<?>> l = new ArrayList<>();
		l.addAll(systemfixTasks);
		l.addAll(userfixTasks);
        
	    return l;
	}

	/**
	 * Returns the list of tasks that are run <b>only once</b>, which deal with
	 * the updating the existing database objects and information to solve
	 * existing issues or to add new structures associated to new features.
	 * <p>
	 * The number after the "Task" word and before the description represents
	 * the system version number in the {@code DB_VERSION} table. A successfully
	 * executed upgrade task will add a new record in such a table with the
	 * number in the class name. This allows dotCMS to keep track of the tasks 
	 * that have been run.
	 * 
	 * @return The list of Run-Once Tasks.
	 */
	public static List<Class<?>> getStartupRunOnceTaskClasses() {
		List<Class<?>> ret = new ArrayList<Class<?>>();
		ret.add(Task00760AddContentletStructureInodeIndex.class);
		ret.add(Task00765AddUserForeignKeys.class);
		ret.add(Task00766AddFieldVariableTable.class);
		ret.add(Task00767FieldVariableValueTypeChange.class);
		ret.add(Task00768CreateTagStorageFieldOnHostStructure.class);
		ret.add(Task00769UpdateTagDataModel.class);
		ret.add(Task00775DropUnusedTables.class);
		ret.add(Task00780UUIDTypeChange.class);
		ret.add(Task00782CleanDataInconsistencies.class);
		ret.add(Task00785DataModelChanges.class);
		ret.add(Task00790DataModelChangesForWebAssets.class);
		ret.add(Task00795LiveWorkingToIdentifier.class);
		ret.add(Task00800CreateTemplateContainers.class);
		ret.add(Task00805AddRenameFolderProcedure.class);
		ret.add(Task00810FilesAsContentChanges.class);
		ret.add(Task00815WorkFlowTablesChanges.class);
		ret.add(Task00820CreateNewWorkFlowTables.class);
		ret.add(Task00825UpdateLoadRecordsToIndex.class);
		ret.add(Task00835CreateIndiciesTables.class);
		ret.add(Task00840FixContentletVersionInfo.class);
		ret.add(Task00845ChangeLockedOnToTimeStamp.class);
		ret.add(Task00850DropOldFilesConstraintInWorkflow.class);
		ret.add(Task00855FixRenameFolder.class);
		ret.add(Task00860ExtendServerIdsMSSQL.class);
		ret.add(Task00865AddTimestampToVersionTables.class);
		ret.add(Task00900CreateLogConsoleTable.class);
		ret.add(Task00905FixAddFolderAfterDelete.class);
		ret.add(Task00910AddEscalationFields.class);
        ret.add( Task00920AddContentletVersionSystemHost.class );
        ret.add(Task00922FixdotfolderpathMSSQL.class);
        ret.add(Task00925UserIdTypeChange.class);
        ret.add(Task00930AddIdentifierIndex.class);
        ret.add(Task00935LogConsoleTableData.class);
        ret.add(Task00940AlterTemplateTable.class);
        // Content Publishing Framework
        ret.add(Task00945AddTableContentPublishing.class);
        // Content Publishing Framework - End Point Management
        ret.add(Task00950AddTablePublishingEndpoint.class);
        ret.add(Task01000LinkChequerTable.class);
        ret.add(Task01005TemplateThemeField.class);
        ret.add(Task01015AddPublishExpireDateToIdentifier.class);
        ret.add(Task01016AddStructureExpireFields.class);
        ret.add(Task01020CreateDefaultWorkflow.class);
        ret.add(Task01030AddSiteSearchAuditTable.class);
        ret.add(Task01035FixTriggerVarLength.class);
        ret.add(Task03000CreateContainertStructures.class);
        ret.add(Task01045FixUpgradeTriggerVarLength.class);
        ret.add(Task01050AddPushPublishLogger.class);
        ret.add(Task01055CreatePushPublishEnvironmentTable.class);
        ret.add(Task01060CreatePushPublishPushedAssets.class);
        ret.add(Task01065IndexOnPublishingQueueAuditStatus.class);
        ret.add(Task01070BundleNameDropUnique.class);
        ret.add(Task01085CreateBundleTablesIfNotExists.class);
        ret.add(Task01080CreateModDateForMissingObjects.class);
        ret.add(Task01090AddWorkflowSchemeUniqueNameContraint.class);
        ret.add(Task01095CreateIntegrityCheckerResultTables.class);
        ret.add(Task01096CreateContainerStructuresTable.class);
        ret.add(Task03005CreateModDateForFieldIfNeeded.class);
        ret.add(Task03010AddContentletIdentifierIndex.class);
        ret.add(Task03015CreateClusterConfigModel.class);
        ret.add(Task03020PostgresqlIndiciesFK.class);
        ret.add(Task03025CreateFoundationForNotificationSystem.class);
        ret.add(Task03030CreateIndicesForVersionTables.class);
        ret.add(Task03035FixContainerCheckTrigger.class);
        ret.add(Task03040AddIndexesToStructureFields.class);
        ret.add(Task03042AddLicenseRepoModel.class);
        ret.add(Task03045TagnameTypeChangeMSSQL.class);
        ret.add(Task03050updateFormTabName.class);
        ret.add(Task03055RemoveLicenseManagerPortlet.class);
        ret.add(Task03060AddClusterServerAction.class);
		ret.add(Task03065AddHtmlPageIR.class);
        ret.add(Task03100HTMLPageAsContentChanges.class);
        ret.add(Task03105HTMLPageGenericPermissions.class);
        ret.add(Task03120AddInodeToContainerStructure.class);
		ret.add(Task03130ActionletsFromPlugin.class);
		ret.add(Task03135FixStructurePageDetail.class);
		ret.add(Task03140AddFileAssetsIntegrityResultTable.class);
		ret.add(Task03150LoweCaseURLOnVirtualLinksTable.class);
		ret.add(Task03160PublishingPushedAssetsTable.class);
		ret.add(Task03165ModifyLoadRecordsToIndex.class);
		ret.add(Task03500RulesEngineDataModel.class);
		ret.add(Task03505PublishingQueueAuditTable.class);
		ret.add(Task03510CreateDefaultPersona.class);
		ret.add(Task03515AlterPasswordColumnFromUserTable.class);
		ret.add(Task03520AlterTagsForPersonas.class);
		ret.add(Task03525LowerTagsTagname.class);
		ret.add(Task03530AlterTagInode.class);
		ret.add(Task03535RemoveTagsWithoutATagname.class);
		ret.add(Task03540UpdateTagInodesReferences.class);
		ret.add(Task03545FixVarcharSizeInFolderOperations.class);
		ret.add(Task03550RenameContainersTable.class);
		ret.add(Task03555AddFlagToDeleteUsers.class);
		ret.add(Task03560TemplateLayoutCanonicalName.class);
		ret.add(Task03565FixContainerVersionsCheck.class);
        ret.add(Task03700ModificationDateColumnAddedToUserTable.class);
        ret.add(Task03705AddingSystemEventTable.class);
        ret.add(Task03710AddFKForIntegrityCheckerTables.class);
        ret.add(Task03715AddFKForPublishingBundleTable.class);
        return ret;
    }

	/**
	 * Returns list of tasks that are run <b>every time</b> that dotCMS starts
	 * up. In the case of a fresh install, these tasks will deploy the default
	 * database schema and data, along with the information associated to the
	 * Starter Site ("demo.dotcms.com").
	 * 
	 * @return The list of Run-Always Tasks.
	 */
    public static List<Class<?>> getStartupRunAlwaysTaskClasses() {
		List<Class<?>> ret = new ArrayList<Class<?>>();
		ret.add(Task00001LoadSchema.class);
		ret.add(Task00003CreateSystemRoles.class);
		ret.add(Task00004LoadStarter.class);
		ret.add(Task00005LoadFixassets.class);
		ret.add(Task00006CreateSystemLayout.class);
		ret.add(Task00007RemoveSitesearchQuartzJob.class);
		ret.add(Task00009ClusterInitialize.class);
		ret.add(Task00010CheckAnonymousUser.class);
		return ret;
	}

}
