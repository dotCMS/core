package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.fixtask.tasks.FixTask00001CheckAssetsMissingIdentifiers;
import com.dotmarketing.fixtask.tasks.FixTask00003CheckContainersInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00006CheckLinksInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00007CheckTemplatesInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00008CheckTreeInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00009CheckContentletsInexistentInodes;
import com.dotmarketing.fixtask.tasks.FixTask00011RenameHostInFieldVariableName;
import com.dotmarketing.fixtask.tasks.FixTask00012UpdateAssetsHosts;
import com.dotmarketing.fixtask.tasks.FixTask00015FixAssetTypesInIdentifiers;
import com.dotmarketing.fixtask.tasks.FixTask00020DeleteOrphanedIdentifiers;
import com.dotmarketing.fixtask.tasks.FixTask00030DeleteOrphanedAssets;
import com.dotmarketing.fixtask.tasks.FixTask00050FixInodesWithoutContentlets;
import com.dotmarketing.fixtask.tasks.FixTask00060FixAssetType;
import com.dotmarketing.fixtask.tasks.FixTask00070FixVersionInfo;
import com.dotmarketing.fixtask.tasks.FixTask00080DeleteOrphanedContentTypeFields;
import com.dotmarketing.fixtask.tasks.FixTask00085FixEmptyParentPathOnIdentifier;
import com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPath;
import com.dotmarketing.fixtask.tasks.FixTask00095DeleteOrphanRelationships;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.startup.runalways.Task00002LoadClusterLicenses;
import com.dotmarketing.startup.runalways.Task00003CreateSystemRoles;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.dotmarketing.startup.runalways.Task00005LoadFixassets;
import com.dotmarketing.startup.runalways.Task00006CreateSystemLayout;
import com.dotmarketing.startup.runalways.Task00007RemoveSitesearchQuartzJob;
import com.dotmarketing.startup.runalways.Task00040CheckAnonymousUser;
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

	private static List<Class<?>> userfixTasks = new CopyOnWriteArrayList<>();

	/**
	 * Returns the list of tasks that are run to solve internal conflicts
	 * related to data inconsistency.
	 *
	 * @return The list of Fix Tasks.
	 */
	private static List<Class<?>> systemfixTasks = ImmutableList.of(
			FixTask00001CheckAssetsMissingIdentifiers.class,
			FixTask00003CheckContainersInconsistencies.class,
			FixTask00006CheckLinksInconsistencies.class,
			FixTask00007CheckTemplatesInconsistencies.class,
			FixTask00008CheckTreeInconsistencies.class,
			FixTask00009CheckContentletsInexistentInodes.class,
			FixTask00011RenameHostInFieldVariableName.class,
			FixTask00012UpdateAssetsHosts.class,
			FixTask00015FixAssetTypesInIdentifiers.class,
			FixTask00020DeleteOrphanedIdentifiers.class,
			FixTask00030DeleteOrphanedAssets.class,
			FixTask00050FixInodesWithoutContentlets.class,
			FixTask00060FixAssetType.class,
			FixTask00070FixVersionInfo.class,
			FixTask00080DeleteOrphanedContentTypeFields.class,
			FixTask00085FixEmptyParentPathOnIdentifier.class,
			FixTask00090RecreateMissingFoldersInParentPath.class,
			FixTask00095DeleteOrphanRelationships.class
	);

	/**
	 * Adds a dotCMS fix task to the main fix task list.
	 *
	 * @param clazz
	 *            - The new fix task class.
	 */
	public static void addFixTask(Class<?> clazz){
		userfixTasks.add(clazz);
	}

	/**
	 * Removes the specified fix task from the main list.
	 *
	 * @param clazz
	 *            - The fix task to remove.
	 */
	public static void removeFixTask(Class<?> clazz){
		userfixTasks.remove(clazz);
	}

	/**
	 * Returns the list of fix task classes for the current dotCMS instance.
	 *
	 * @return The list of fix tasks.
	 */
	public static List<Class<?>> getFixTaskClasses() {
		final List<Class<?>> l = new ArrayList<>();
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
		final List<Class<?>> ret = ImmutableList.<Class<?>>builder()
		.add(Task00760AddContentletStructureInodeIndex.class)
		.add(Task00765AddUserForeignKeys.class)
		.add(Task00766AddFieldVariableTable.class)
		.add(Task00767FieldVariableValueTypeChange.class)
		.add(Task00768CreateTagStorageFieldOnHostStructure.class)
		.add(Task00769UpdateTagDataModel.class)
		.add(Task00775DropUnusedTables.class)
		.add(Task00780UUIDTypeChange.class)
		.add(Task00782CleanDataInconsistencies.class)
		.add(Task00785DataModelChanges.class)
		.add(Task00790DataModelChangesForWebAssets.class)
		.add(Task00795LiveWorkingToIdentifier.class)
		.add(Task00800CreateTemplateContainers.class)
		.add(Task00805AddRenameFolderProcedure.class)
		.add(Task00810FilesAsContentChanges.class)
		.add(Task00815WorkFlowTablesChanges.class)
		.add(Task00820CreateNewWorkFlowTables.class)
		.add(Task00825UpdateLoadRecordsToIndex.class)
		.add(Task00835CreateIndiciesTables.class)
		.add(Task00840FixContentletVersionInfo.class)
		.add(Task00845ChangeLockedOnToTimeStamp.class)
		.add(Task00850DropOldFilesConstraintInWorkflow.class)
		.add(Task00855FixRenameFolder.class)
		.add(Task00860ExtendServerIdsMSSQL.class)
		.add(Task00865AddTimestampToVersionTables.class)
		.add(Task00900CreateLogConsoleTable.class)
		.add(Task00905FixAddFolderAfterDelete.class)
		.add(Task00910AddEscalationFields.class)
		.add( Task00920AddContentletVersionSystemHost.class )
		.add(Task00922FixdotfolderpathMSSQL.class)
		.add(Task00925UserIdTypeChange.class)
		.add(Task00930AddIdentifierIndex.class)
		.add(Task00935LogConsoleTableData.class)
		.add(Task00940AlterTemplateTable.class)
		// Content Publishing Framework
		.add(Task00945AddTableContentPublishing.class)
		// Content Publishing Framework - End Point Management
		.add(Task00950AddTablePublishingEndpoint.class)
		.add(Task01000LinkChequerTable.class)
		.add(Task01005TemplateThemeField.class)
		.add(Task01015AddPublishExpireDateToIdentifier.class)
		.add(Task01016AddStructureExpireFields.class)
		.add(Task01020CreateDefaultWorkflow.class)
		.add(Task01030AddSiteSearchAuditTable.class)
		.add(Task01035FixTriggerVarLength.class)
		.add(Task03000CreateContainertStructures.class)
		.add(Task01045FixUpgradeTriggerVarLength.class)
		.add(Task01050AddPushPublishLogger.class)
		.add(Task01055CreatePushPublishEnvironmentTable.class)
		.add(Task01060CreatePushPublishPushedAssets.class)
		.add(Task01065IndexOnPublishingQueueAuditStatus.class)
		.add(Task01070BundleNameDropUnique.class)
		.add(Task01085CreateBundleTablesIfNotExists.class)
		.add(Task01080CreateModDateForMissingObjects.class)
		.add(Task01090AddWorkflowSchemeUniqueNameContraint.class)
		.add(Task01095CreateIntegrityCheckerResultTables.class)
		.add(Task01096CreateContainerStructuresTable.class)
		.add(Task03005CreateModDateForFieldIfNeeded.class)
		.add(Task03010AddContentletIdentifierIndex.class)
		.add(Task03015CreateClusterConfigModel.class)
		.add(Task03020PostgresqlIndiciesFK.class)
		.add(Task03025CreateFoundationForNotificationSystem.class)
		.add(Task03030CreateIndicesForVersionTables.class)
		.add(Task03035FixContainerCheckTrigger.class)
		.add(Task03040AddIndexesToStructureFields.class)
		.add(Task03042AddLicenseRepoModel.class)
		.add(Task03045TagnameTypeChangeMSSQL.class)
		.add(Task03050updateFormTabName.class)
		.add(Task03055RemoveLicenseManagerPortlet.class)
		.add(Task03060AddClusterServerAction.class)
		.add(Task03065AddHtmlPageIR.class)
		.add(Task03100HTMLPageAsContentChanges.class)
		.add(Task03105HTMLPageGenericPermissions.class)
		.add(Task03120AddInodeToContainerStructure.class)
		.add(Task03130ActionletsFromPlugin.class)
		.add(Task03135FixStructurePageDetail.class)
		.add(Task03140AddFileAssetsIntegrityResultTable.class)
		.add(Task03150LoweCaseURLOnVirtualLinksTable.class)
		.add(Task03160PublishingPushedAssetsTable.class)
		.add(Task03165ModifyLoadRecordsToIndex.class)
		.add(Task03500RulesEngineDataModel.class)
		.add(Task03505PublishingQueueAuditTable.class)
		.add(Task03510CreateDefaultPersona.class)
		.add(Task03515AlterPasswordColumnFromUserTable.class)
		.add(Task03520AlterTagsForPersonas.class)
		.add(Task03525LowerTagsTagname.class)
		.add(Task03530AlterTagInode.class)
		.add(Task03535RemoveTagsWithoutATagname.class)
		.add(Task03540UpdateTagInodesReferences.class)
		.add(Task03545FixVarcharSizeInFolderOperations.class)
		.add(Task03550RenameContainersTable.class)
		.add(Task03555AddFlagToDeleteUsers.class)
		.add(Task03560TemplateLayoutCanonicalName.class)
		.add(Task03565FixContainerVersionsCheck.class)
		.add(Task03600UpdateMssqlVarcharTextColumns.class)
		.add(Task03605FixMSSQLMissingConstraints.class)
		.add(Task03700ModificationDateColumnAddedToUserTable.class)
		.add(Task03705AddingSystemEventTable.class)
		.add(Task03710AddFKForIntegrityCheckerTables.class)
		.add(Task03715AddFKForPublishingBundleTable.class)
		.add(Task03720AddRolesIntegrityCheckerTable.class)
		.add(Task03725NewNotificationTable.class)
		.add(Task03735UpdatePortletsIds.class)
		.add(Task03740UpdateLayoutIcons.class)
		.add(Task03745DropLegacyHTMLPageAndFileTables.class)
		.add(Task03800AddIndexLowerStructureTable.class)
		.add(Task04100DeleteUnusedJobEntries.class)
		.add(Task04105LowercaseVanityUrls.class)
		.add(Task04110AddColumnsPublishingPushedAssetsTable.class)
		.add(Task04115LowercaseIdentifierUrls.class)
		.add(Task04120IncreaseHostColumnOnClusterServerTable.class)
		.add(Task04200CreateDefaultVanityURL.class)
		.add(Task04205MigrateVanityURLToContent.class)
        .add(Task04210CreateDefaultLanguageVariable.class)
		.add(Task04215MySQLMissingConstraints.class)
		.add(Task04220RemoveDeleteInactiveClusterServersJob.class)
		.add(Task04230FixVanityURLInconsistencies.class)
		.add(Task04300UpdateSystemFolderIdentifier.class)
		.add(Task04305UpdateWorkflowActionTable.class)
		.add(Task04310CreateWorkflowRoles.class)
		.add(Task04315UpdateMultiTreePK.class)
		.add(Task04320WorkflowActionRemoveNextStepConstraint.class)
		.add(Task04235RemoveFKFromWorkflowTaskTable.class)
		.add(Task04330WorkflowTaskAddLanguageIdColumn.class)
		.add(Task04335CreateSystemWorkflow.class)
		.add(Task04340TemplateShowOnMenu.class)
		.add(Task04345AddSystemWorkflowToContentType.class)
		.add(Task04350AddDefaultWorkflowActionStates.class)
		.add(Task04355SystemEventAddServerIdColumn.class)
		.add(Task04360WorkflowSchemeDropUniqueNameConstraint.class)
		.add(Task04365RelationshipUniqueConstraint.class)
		.add(Task04370AddVisitorLogger.class)
		.add(Task04375UpdateColors.class)
		.add(Task04380AddSubActionToWorkflowActions.class)
		.add(Task04385UpdateCategoryKey.class)
		.add(Task04390ShowEditingListingWorkflowActionTable.class)
		.add(Task05030UpdateSystemContentTypesHost.class)
		.add(Task05035CreateIndexForQRTZ_EXCL_TRIGGERSTable.class)
		.add(Task05040LanguageTableIdentityOff.class)
		.add(Task05050FileAssetContentTypeReadOnlyFileName.class)
		.add(Task05060CreateApiTokensIssuedTable.class)
		.add(Task05070AddIdentifierVirtualColumn.class)
		.add(Task05080RecreateIdentifierIndex.class)
		.add(Task05150CreateIndicesForContentVersionInfoMSSQL.class)
		.add(Task05160MultiTreeAddPersonalizationColumnAndChangingPK.class)
		.add(Task05165CreateContentTypeWorkflowActionMappingTable.class)
		.add(Task05170DefineFrontEndAndBackEndRoles.class)
		.add(Task05175AssignDefaultActionsToTheSystemWorkflow.class)
	    .add(Task05180UpdateFriendlyNameField.class)
    	.add(Task05190UpdateFormsWidgetCodeField.class)
		.add(Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflow.class)
		.add(Task05200WorkflowTaskUniqueKey.class)
		.add(Task05210CreateDefaultDotAsset.class)
		.add(Task05215AddSystemWorkflowToDotAssetContentType.class)
		.add(Task05220MakeFileAssetContentTypeBinaryFieldIndexedListed.class)
        .add(Task05225RemoveLoadRecordsToIndex.class)
        .add(Task05300UpdateIndexNameLength.class)
        .add(Task05305AddPushPublishFilterColumn.class)
		.add(Task05350AddDotSaltClusterColumn.class)
		.add(Task05370AddAppsPortletToLayout.class)
		.add(Task05390RemoveEndpointIdForeignKeyInIntegrityResolverTables.class)
		.add(Task05370AddAppsPortletToLayout.class)
        .build();
        
        return ret.stream().sorted(classNameComparator).collect(Collectors.toList());

	}

	
    final static private Comparator<Class<?>> classNameComparator = new Comparator<Class<?>>() {
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    
	
	
	/**
	 * Returns list of tasks that are run <b>every time</b> that dotCMS starts
	 * up. In the case of a fresh install, these tasks will deploy the default
	 * database schema and data, along with the information associated to the
	 * Starter Site ("demo.dotcms.com").
	 *
	 * @return The list of Run-Always Tasks.
	 */
	public static List<Class<?>> getStartupRunAlwaysTaskClasses() {
		final List<Class<?>> ret = new ArrayList<Class<?>>();
		ret.add(Task00001LoadSchema.class);
		ret.add(Task00003CreateSystemRoles.class);
		ret.add(Task00004LoadStarter.class);
		ret.add(Task00005LoadFixassets.class);
		ret.add(Task00006CreateSystemLayout.class);
		ret.add(Task00007RemoveSitesearchQuartzJob.class);
		ret.add(Task00002LoadClusterLicenses.class);
		//ret.add(Task00030ClusterInitialize.class);
		ret.add(Task00040CheckAnonymousUser.class);
        return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

}
