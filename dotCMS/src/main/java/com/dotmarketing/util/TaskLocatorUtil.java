package com.dotmarketing.util;

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
import com.dotmarketing.startup.runalways.Task00050LoadAppsSecrets;
import com.dotmarketing.startup.runonce.Task00760AddContentletStructureInodeIndex;
import com.dotmarketing.startup.runonce.Task00765AddUserForeignKeys;
import com.dotmarketing.startup.runonce.Task00766AddFieldVariableTable;
import com.dotmarketing.startup.runonce.Task00767FieldVariableValueTypeChange;
import com.dotmarketing.startup.runonce.Task00768CreateTagStorageFieldOnHostStructure;
import com.dotmarketing.startup.runonce.Task00769UpdateTagDataModel;
import com.dotmarketing.startup.runonce.Task00775DropUnusedTables;
import com.dotmarketing.startup.runonce.Task00780UUIDTypeChange;
import com.dotmarketing.startup.runonce.Task00782CleanDataInconsistencies;
import com.dotmarketing.startup.runonce.Task00785DataModelChanges;
import com.dotmarketing.startup.runonce.Task00790DataModelChangesForWebAssets;
import com.dotmarketing.startup.runonce.Task00795LiveWorkingToIdentifier;
import com.dotmarketing.startup.runonce.Task00800CreateTemplateContainers;
import com.dotmarketing.startup.runonce.Task00805AddRenameFolderProcedure;
import com.dotmarketing.startup.runonce.Task00810FilesAsContentChanges;
import com.dotmarketing.startup.runonce.Task00815WorkFlowTablesChanges;
import com.dotmarketing.startup.runonce.Task00820CreateNewWorkFlowTables;
import com.dotmarketing.startup.runonce.Task00825UpdateLoadRecordsToIndex;
import com.dotmarketing.startup.runonce.Task00835CreateIndiciesTables;
import com.dotmarketing.startup.runonce.Task00840FixContentletVersionInfo;
import com.dotmarketing.startup.runonce.Task00845ChangeLockedOnToTimeStamp;
import com.dotmarketing.startup.runonce.Task00850DropOldFilesConstraintInWorkflow;
import com.dotmarketing.startup.runonce.Task00855FixRenameFolder;
import com.dotmarketing.startup.runonce.Task00860ExtendServerIdsMSSQL;
import com.dotmarketing.startup.runonce.Task00865AddTimestampToVersionTables;
import com.dotmarketing.startup.runonce.Task00900CreateLogConsoleTable;
import com.dotmarketing.startup.runonce.Task00905FixAddFolderAfterDelete;
import com.dotmarketing.startup.runonce.Task00910AddEscalationFields;
import com.dotmarketing.startup.runonce.Task00920AddContentletVersionSystemHost;
import com.dotmarketing.startup.runonce.Task00922FixdotfolderpathMSSQL;
import com.dotmarketing.startup.runonce.Task00925UserIdTypeChange;
import com.dotmarketing.startup.runonce.Task00930AddIdentifierIndex;
import com.dotmarketing.startup.runonce.Task00935LogConsoleTableData;
import com.dotmarketing.startup.runonce.Task00940AlterTemplateTable;
import com.dotmarketing.startup.runonce.Task00945AddTableContentPublishing;
import com.dotmarketing.startup.runonce.Task00950AddTablePublishingEndpoint;
import com.dotmarketing.startup.runonce.Task01000LinkChequerTable;
import com.dotmarketing.startup.runonce.Task01005TemplateThemeField;
import com.dotmarketing.startup.runonce.Task01015AddPublishExpireDateToIdentifier;
import com.dotmarketing.startup.runonce.Task01016AddStructureExpireFields;
import com.dotmarketing.startup.runonce.Task01020CreateDefaultWorkflow;
import com.dotmarketing.startup.runonce.Task01030AddSiteSearchAuditTable;
import com.dotmarketing.startup.runonce.Task01035FixTriggerVarLength;
import com.dotmarketing.startup.runonce.Task01045FixUpgradeTriggerVarLength;
import com.dotmarketing.startup.runonce.Task01050AddPushPublishLogger;
import com.dotmarketing.startup.runonce.Task01055CreatePushPublishEnvironmentTable;
import com.dotmarketing.startup.runonce.Task01060CreatePushPublishPushedAssets;
import com.dotmarketing.startup.runonce.Task01065IndexOnPublishingQueueAuditStatus;
import com.dotmarketing.startup.runonce.Task01070BundleNameDropUnique;
import com.dotmarketing.startup.runonce.Task01080CreateModDateForMissingObjects;
import com.dotmarketing.startup.runonce.Task01085CreateBundleTablesIfNotExists;
import com.dotmarketing.startup.runonce.Task01090AddWorkflowSchemeUniqueNameContraint;
import com.dotmarketing.startup.runonce.Task01095CreateIntegrityCheckerResultTables;
import com.dotmarketing.startup.runonce.Task01096CreateContainerStructuresTable;
import com.dotmarketing.startup.runonce.Task03000CreateContainertStructures;
import com.dotmarketing.startup.runonce.Task03005CreateModDateForFieldIfNeeded;
import com.dotmarketing.startup.runonce.Task03010AddContentletIdentifierIndex;
import com.dotmarketing.startup.runonce.Task03015CreateClusterConfigModel;
import com.dotmarketing.startup.runonce.Task03020PostgresqlIndiciesFK;
import com.dotmarketing.startup.runonce.Task03025CreateFoundationForNotificationSystem;
import com.dotmarketing.startup.runonce.Task03030CreateIndicesForVersionTables;
import com.dotmarketing.startup.runonce.Task03035FixContainerCheckTrigger;
import com.dotmarketing.startup.runonce.Task03040AddIndexesToStructureFields;
import com.dotmarketing.startup.runonce.Task03042AddLicenseRepoModel;
import com.dotmarketing.startup.runonce.Task03045TagnameTypeChangeMSSQL;
import com.dotmarketing.startup.runonce.Task03050updateFormTabName;
import com.dotmarketing.startup.runonce.Task03055RemoveLicenseManagerPortlet;
import com.dotmarketing.startup.runonce.Task03060AddClusterServerAction;
import com.dotmarketing.startup.runonce.Task03065AddHtmlPageIR;
import com.dotmarketing.startup.runonce.Task03100HTMLPageAsContentChanges;
import com.dotmarketing.startup.runonce.Task03105HTMLPageGenericPermissions;
import com.dotmarketing.startup.runonce.Task03120AddInodeToContainerStructure;
import com.dotmarketing.startup.runonce.Task03130ActionletsFromPlugin;
import com.dotmarketing.startup.runonce.Task03135FixStructurePageDetail;
import com.dotmarketing.startup.runonce.Task03140AddFileAssetsIntegrityResultTable;
import com.dotmarketing.startup.runonce.Task03150LoweCaseURLOnVirtualLinksTable;
import com.dotmarketing.startup.runonce.Task03160PublishingPushedAssetsTable;
import com.dotmarketing.startup.runonce.Task03165ModifyLoadRecordsToIndex;
import com.dotmarketing.startup.runonce.Task03500RulesEngineDataModel;
import com.dotmarketing.startup.runonce.Task03505PublishingQueueAuditTable;
import com.dotmarketing.startup.runonce.Task03510CreateDefaultPersona;
import com.dotmarketing.startup.runonce.Task03515AlterPasswordColumnFromUserTable;
import com.dotmarketing.startup.runonce.Task03520AlterTagsForPersonas;
import com.dotmarketing.startup.runonce.Task03525LowerTagsTagname;
import com.dotmarketing.startup.runonce.Task03530AlterTagInode;
import com.dotmarketing.startup.runonce.Task03535RemoveTagsWithoutATagname;
import com.dotmarketing.startup.runonce.Task03540UpdateTagInodesReferences;
import com.dotmarketing.startup.runonce.Task03545FixVarcharSizeInFolderOperations;
import com.dotmarketing.startup.runonce.Task03550RenameContainersTable;
import com.dotmarketing.startup.runonce.Task03555AddFlagToDeleteUsers;
import com.dotmarketing.startup.runonce.Task03560TemplateLayoutCanonicalName;
import com.dotmarketing.startup.runonce.Task03565FixContainerVersionsCheck;
import com.dotmarketing.startup.runonce.Task03600UpdateMssqlVarcharTextColumns;
import com.dotmarketing.startup.runonce.Task03605FixMSSQLMissingConstraints;
import com.dotmarketing.startup.runonce.Task03700ModificationDateColumnAddedToUserTable;
import com.dotmarketing.startup.runonce.Task03705AddingSystemEventTable;
import com.dotmarketing.startup.runonce.Task03710AddFKForIntegrityCheckerTables;
import com.dotmarketing.startup.runonce.Task03715AddFKForPublishingBundleTable;
import com.dotmarketing.startup.runonce.Task03720AddRolesIntegrityCheckerTable;
import com.dotmarketing.startup.runonce.Task03725NewNotificationTable;
import com.dotmarketing.startup.runonce.Task03735UpdatePortletsIds;
import com.dotmarketing.startup.runonce.Task03740UpdateLayoutIcons;
import com.dotmarketing.startup.runonce.Task03745DropLegacyHTMLPageAndFileTables;
import com.dotmarketing.startup.runonce.Task03800AddIndexLowerStructureTable;
import com.dotmarketing.startup.runonce.Task04100DeleteUnusedJobEntries;
import com.dotmarketing.startup.runonce.Task04105LowercaseVanityUrls;
import com.dotmarketing.startup.runonce.Task04110AddColumnsPublishingPushedAssetsTable;
import com.dotmarketing.startup.runonce.Task04115LowercaseIdentifierUrls;
import com.dotmarketing.startup.runonce.Task04120IncreaseHostColumnOnClusterServerTable;
import com.dotmarketing.startup.runonce.Task04200CreateDefaultVanityURL;
import com.dotmarketing.startup.runonce.Task04205MigrateVanityURLToContent;
import com.dotmarketing.startup.runonce.Task04210CreateDefaultLanguageVariable;
import com.dotmarketing.startup.runonce.Task04215MySQLMissingConstraints;
import com.dotmarketing.startup.runonce.Task04220RemoveDeleteInactiveClusterServersJob;
import com.dotmarketing.startup.runonce.Task04230FixVanityURLInconsistencies;
import com.dotmarketing.startup.runonce.Task04235RemoveFKFromWorkflowTaskTable;
import com.dotmarketing.startup.runonce.Task04300UpdateSystemFolderIdentifier;
import com.dotmarketing.startup.runonce.Task04305UpdateWorkflowActionTable;
import com.dotmarketing.startup.runonce.Task04310CreateWorkflowRoles;
import com.dotmarketing.startup.runonce.Task04315UpdateMultiTreePK;
import com.dotmarketing.startup.runonce.Task04320WorkflowActionRemoveNextStepConstraint;
import com.dotmarketing.startup.runonce.Task04330WorkflowTaskAddLanguageIdColumn;
import com.dotmarketing.startup.runonce.Task04335CreateSystemWorkflow;
import com.dotmarketing.startup.runonce.Task04340TemplateShowOnMenu;
import com.dotmarketing.startup.runonce.Task04345AddSystemWorkflowToContentType;
import com.dotmarketing.startup.runonce.Task04350AddDefaultWorkflowActionStates;
import com.dotmarketing.startup.runonce.Task04355SystemEventAddServerIdColumn;
import com.dotmarketing.startup.runonce.Task04360WorkflowSchemeDropUniqueNameConstraint;
import com.dotmarketing.startup.runonce.Task04365RelationshipUniqueConstraint;
import com.dotmarketing.startup.runonce.Task04370AddVisitorLogger;
import com.dotmarketing.startup.runonce.Task04375UpdateColors;
import com.dotmarketing.startup.runonce.Task04380AddSubActionToWorkflowActions;
import com.dotmarketing.startup.runonce.Task04385UpdateCategoryKey;
import com.dotmarketing.startup.runonce.Task04390ShowEditingListingWorkflowActionTable;
import com.dotmarketing.startup.runonce.Task05030UpdateSystemContentTypesHost;
import com.dotmarketing.startup.runonce.Task05035CreateIndexForQRTZ_EXCL_TRIGGERSTable;
import com.dotmarketing.startup.runonce.Task05040LanguageTableIdentityOff;
import com.dotmarketing.startup.runonce.Task05050FileAssetContentTypeReadOnlyFileName;
import com.dotmarketing.startup.runonce.Task05060CreateApiTokensIssuedTable;
import com.dotmarketing.startup.runonce.Task05070AddIdentifierVirtualColumn;
import com.dotmarketing.startup.runonce.Task05080RecreateIdentifierIndex;
import com.dotmarketing.startup.runonce.Task05150CreateIndicesForContentVersionInfoMSSQL;
import com.dotmarketing.startup.runonce.Task05160MultiTreeAddPersonalizationColumnAndChangingPK;
import com.dotmarketing.startup.runonce.Task05165CreateContentTypeWorkflowActionMappingTable;
import com.dotmarketing.startup.runonce.Task05170DefineFrontEndAndBackEndRoles;
import com.dotmarketing.startup.runonce.Task05175AssignDefaultActionsToTheSystemWorkflow;
import com.dotmarketing.startup.runonce.Task05180UpdateFriendlyNameField;
import com.dotmarketing.startup.runonce.Task05190UpdateFormsWidgetCodeField;
import com.dotmarketing.startup.runonce.Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflow;
import com.dotmarketing.startup.runonce.Task05200WorkflowTaskUniqueKey;
import com.dotmarketing.startup.runonce.Task05210CreateDefaultDotAsset;
import com.dotmarketing.startup.runonce.Task05215AddSystemWorkflowToDotAssetContentType;
import com.dotmarketing.startup.runonce.Task05220MakeFileAssetContentTypeBinaryFieldIndexedListed;
import com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndex;
import com.dotmarketing.startup.runonce.Task05300UpdateIndexNameLength;
import com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumn;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumn;
import com.dotmarketing.startup.runonce.Task05370AddAppsPortletToLayout;
import com.dotmarketing.startup.runonce.Task05380ChangeContainerPathToAbsolute;
import com.dotmarketing.startup.runonce.Task05390MakeRoomForLongerJobDetail;
import com.dotmarketing.startup.runonce.Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables;
import com.dotmarketing.startup.runonce.Task201013AddNewColumnsToIdentifierTable;
import com.dotmarketing.startup.runonce.Task201014UpdateColumnsValuesInIdentifierTable;
import com.dotmarketing.startup.runonce.Task201102UpdateColumnSitelicTable;
import com.dotmarketing.startup.runonce.Task210218MigrateUserProxyTable;
import com.dotmarketing.startup.runonce.Task210316UpdateLayoutIcons;
import com.dotmarketing.startup.runonce.Task210319CreateStorageTable;
import com.dotmarketing.startup.runonce.Task210321RemoveOldMetadataFiles;
import com.dotmarketing.startup.runonce.Task210506UpdateStorageTable;
import com.dotmarketing.startup.runonce.Task210510UpdateStorageTableDropMetadataColumn;
import com.dotmarketing.startup.runonce.Task210520UpdateAnonymousEmail;
import com.dotmarketing.startup.runonce.Task210527DropReviewFieldsFromContentletTable;
import com.dotmarketing.startup.runonce.Task210719CleanUpTitleField;
import com.dotmarketing.startup.runonce.Task210802UpdateStructureTable;
import com.dotmarketing.startup.runonce.Task210805DropUserProxyTable;
import com.dotmarketing.startup.runonce.Task210816DeInodeRelationship;
import com.dotmarketing.startup.runonce.Task210901UpdateDateTimezones;
import com.dotmarketing.startup.runonce.Task211007RemoveNotNullConstraintFromCompanyMXColumn;
import com.dotmarketing.startup.runonce.Task211012AddCompanyDefaultLanguage;
import com.dotmarketing.startup.runonce.Task211020CreateHostIntegrityCheckerResultTables;
import com.dotmarketing.startup.runonce.Task211101AddContentletAsJsonColumn;
import com.dotmarketing.startup.runonce.Task211103RenameHostNameLabel;
import com.dotmarketing.startup.runonce.Task220202RemoveFKStructureFolderConstraint;
import com.dotmarketing.startup.runonce.Task220203RemoveFolderInodeConstraint;
import com.dotmarketing.startup.runonce.Task220214AddOwnerAndIDateToFolderTable;
import com.dotmarketing.startup.runonce.Task220215MigrateDataFromInodeToFolder;
import com.dotmarketing.startup.runonce.Task220330ChangeVanityURLSiteFieldType;
import com.dotmarketing.startup.runonce.Task220401CreateClusterLockTable;
import com.dotmarketing.startup.runonce.Task220402UpdateDateTimezones;
import com.dotmarketing.startup.runonce.Task220413IncreasePublishedPushedAssetIdCol;
import com.dotmarketing.startup.runonce.Task220512UpdateNoHTMLRegexValue;
import com.dotmarketing.startup.runonce.Task220606UpdatePushNowActionletName;
import com.dotmarketing.startup.runonce.Task220822CreateVariantTable;
import com.dotmarketing.startup.runonce.Task220824CreateDefaultVariant;
import com.dotmarketing.startup.runonce.Task220825CreateVariantField;
import com.dotmarketing.startup.runonce.Task220829CreateExperimentsTable;
import com.dotmarketing.startup.runonce.Task220912UpdateCorrectShowOnMenuProperty;
import com.dotmarketing.startup.runonce.Task220928AddLookbackWindowColumnToExperiment;
import com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKey;
import com.dotmarketing.startup.runonce.Task221018CreateVariantFieldInMultiTree;
import com.dotmarketing.startup.runonce.Task230110MakeSomeSystemFieldsRemovableByBaseType;
import com.dotmarketing.startup.runonce.Task230119MigrateContentToProperPersonaTagAndRemoveDupTags;
import com.dotmarketing.startup.runonce.Task230320FixMissingContentletAsJSON;
import com.dotmarketing.startup.runonce.Task230328AddMarkedForDeletionColumn;
import com.dotmarketing.startup.runonce.Task230426AlterVarcharLengthOfLockedByCol;
import com.dotmarketing.startup.runonce.Task230523CreateVariantFieldInContentlet;
import com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentField;
import com.dotmarketing.startup.runonce.Task230701AddHashIndicesToWorkflowTables;
import com.dotmarketing.startup.runonce.Task230707CreateSystemTable;
import com.dotmarketing.startup.runonce.Task230713IncreaseDisabledWysiwygColumnSize;
import com.dotmarketing.startup.runonce.Task231109AddPublishDateToContentletVersionInfo;
import com.dotmarketing.startup.runonce.Task231207AddMetadataColumnToWorkflowAction;
import com.dotmarketing.startup.runonce.Task240102AlterVarcharLengthOfRelationType;
import com.dotmarketing.startup.runonce.Task240111AddInodeAndIdentifierLeftIndexes;
import com.dotmarketing.startup.runonce.Task240112AddMetadataColumnToStructureTable;
import com.dotmarketing.startup.runonce.Task240131UpdateLanguageVariableContentType;
import com.dotmarketing.startup.runonce.Task240306MigrateLegacyLanguageVariables;
import com.dotmarketing.startup.runonce.Task240513UpdateContentTypesSystemField;
import com.dotmarketing.startup.runonce.Task240530AddDotAIPortletToLayout;
import com.dotmarketing.startup.runonce.Task240606AddVariableColumnToWorkflow;
import com.dotmarketing.startup.runonce.Task241013RemoveFullPathLcColumnFromIdentifier;
import com.dotmarketing.startup.runonce.Task241014AddTemplateValueOnContentletIndex;
import com.dotmarketing.startup.runonce.Task241015ReplaceLanguagesWithLocalesPortlet;
import com.dotmarketing.startup.runonce.Task241016AddCustomLanguageVariablesPortletToLayout;
import com.dotmarketing.startup.runonce.Task250107RemoveEsReadOnlyMonitorJob;
import com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTables;
import com.dotmarketing.startup.runonce.Task250603UpdateIdentifierParentPathCheckTrigger;
import com.dotmarketing.startup.runonce.Task250604UpdateFolderInodes;
import com.dotmarketing.startup.runonce.Task250826AddIndexesToUniqueFieldsTable;
import com.dotmarketing.startup.runonce.Task250828CreateCustomAttributeTable;
import com.dotmarketing.startup.runonce.Task251029RemoveContentTypesLegacyPortletFromLayouts;
import com.dotmarketing.startup.runonce.Task251103AddStylePropertiesColumnInMultiTree;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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

	private static final List<Class<?>> userFixTasks = new CopyOnWriteArrayList<>();

	/**
	 * Returns the list of tasks that are run to solve internal conflicts
	 * related to data inconsistency.
	 *
	 * @return The list of Fix Tasks.
	 */
	private static final List<Class<?>> systemFixTasks = ImmutableList.of(
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
		userFixTasks.add(clazz);
	}

	/**
	 * Removes the specified fix task from the main list.
	 *
	 * @param clazz
	 *            - The fix task to remove.
	 */
	public static void removeFixTask(Class<?> clazz){
		userFixTasks.remove(clazz);
	}

	/**
	 * Returns the list of fix task classes for the current dotCMS instance.
	 *
	 * @return The list of fix tasks.
	 */
	public static List<Class<?>> getFixTaskClasses() {
		final List<Class<?>> l = new ArrayList<>();
		l.addAll(systemFixTasks);
		l.addAll(userFixTasks);
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
		.add(Task05370AddAppsPortletToLayout.class)
    	.add(Task05380ChangeContainerPathToAbsolute.class)
    	.add(Task05390MakeRoomForLongerJobDetail.class)
		.add(Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTables.class)
		//New task date-based naming convention starts here
        .add(Task201013AddNewColumnsToIdentifierTable.class)
        .add(Task201014UpdateColumnsValuesInIdentifierTable.class)
        .add(Task201102UpdateColumnSitelicTable.class)
		.add(Task210218MigrateUserProxyTable.class)
		.add(Task210316UpdateLayoutIcons.class)
        .add(Task210319CreateStorageTable.class)
		.add(Task210321RemoveOldMetadataFiles.class)
        .add(Task210506UpdateStorageTable.class)
		.add(Task210510UpdateStorageTableDropMetadataColumn.class)
		.add(Task210520UpdateAnonymousEmail.class)
        .add(Task210527DropReviewFieldsFromContentletTable.class)
        .add(Task210719CleanUpTitleField.class)
		.add(Task210802UpdateStructureTable.class)
        .add(Task210805DropUserProxyTable.class)
		.add(Task210816DeInodeRelationship.class)
		.add(Task210901UpdateDateTimezones.class)
		.add(Task211007RemoveNotNullConstraintFromCompanyMXColumn.class)
		.add(Task211012AddCompanyDefaultLanguage.class)
		.add(Task211020CreateHostIntegrityCheckerResultTables.class)
		.add(Task211101AddContentletAsJsonColumn.class)
		.add(Task211103RenameHostNameLabel.class)
		.add(Task220202RemoveFKStructureFolderConstraint.class)
		.add(Task220203RemoveFolderInodeConstraint.class)
		.add(Task220214AddOwnerAndIDateToFolderTable.class)
		.add(Task220215MigrateDataFromInodeToFolder.class)
		.add(Task220330ChangeVanityURLSiteFieldType.class)
		.add(Task220401CreateClusterLockTable.class)
		.add(Task220402UpdateDateTimezones.class)
		.add(Task220413IncreasePublishedPushedAssetIdCol.class)
		.add(Task220512UpdateNoHTMLRegexValue.class)
		.add(Task220606UpdatePushNowActionletName.class)
		.add(Task220822CreateVariantTable.class)
    	.add(Task220822CreateVariantTable.class)
		.add(Task220829CreateExperimentsTable.class)
		.add(Task220912UpdateCorrectShowOnMenuProperty.class)
		.add(Task220824CreateDefaultVariant.class)
		.add(Task220825CreateVariantField.class)
		.add(Task220928AddLookbackWindowColumnToExperiment.class)
		.add(Task221007AddVariantIntoPrimaryKey.class)
		.add(Task221018CreateVariantFieldInMultiTree.class)
		.add(Task230119MigrateContentToProperPersonaTagAndRemoveDupTags.class)
	    .add(Task230110MakeSomeSystemFieldsRemovableByBaseType.class)
		.add(Task230328AddMarkedForDeletionColumn.class)
		.add(Task230426AlterVarcharLengthOfLockedByCol.class)
		.add(Task230523CreateVariantFieldInContentlet.class)
		.add(Task230630CreateRunningIdsExperimentField.class)
		.add(Task230701AddHashIndicesToWorkflowTables.class)
		.add(Task230707CreateSystemTable.class)
		.add(Task230713IncreaseDisabledWysiwygColumnSize.class)
		.add(Task231109AddPublishDateToContentletVersionInfo.class)
		.add(Task231207AddMetadataColumnToWorkflowAction.class)
		.add(Task240102AlterVarcharLengthOfRelationType.class)
		.add(Task240111AddInodeAndIdentifierLeftIndexes.class)
		.add(Task240131UpdateLanguageVariableContentType.class)
		.add(Task240112AddMetadataColumnToStructureTable.class)
		.add(Task240513UpdateContentTypesSystemField.class)
		.add(Task240530AddDotAIPortletToLayout.class)
		.add(Task240606AddVariableColumnToWorkflow.class)
		.add(Task241013RemoveFullPathLcColumnFromIdentifier.class)
		.add(Task241014AddTemplateValueOnContentletIndex.class)
		.add(Task241015ReplaceLanguagesWithLocalesPortlet.class)
    	.add(Task241016AddCustomLanguageVariablesPortletToLayout.class)
		.add(Task250107RemoveEsReadOnlyMonitorJob.class)
        .add(Task250113CreatePostgresJobQueueTables.class)
		.add(Task250603UpdateIdentifierParentPathCheckTrigger.class)
		.add(Task250604UpdateFolderInodes.class)
        .add(Task250604UpdateFolderInodes.class)
        .add(Task250826AddIndexesToUniqueFieldsTable.class)
        .add(Task250828CreateCustomAttributeTable.class)
        .add(Task251029RemoveContentTypesLegacyPortletFromLayouts.class)
        .add(Task251103AddStylePropertiesColumnInMultiTree.class)
        .build();

        return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

    private static final Comparator<Class<?>> classNameComparator = Comparator.comparing(Class::getName);

	/**
	 * Returns list of tasks that are run <b>every time</b> that dotCMS starts
	 * up. In the case of a fresh install, these tasks will deploy the default
	 * database schema and data, along with the information associated to the
	 * Starter Site ("demo.dotcms.com").
	 *
	 * @return The list of Run-Always Tasks.
	 */
	public static List<Class<?>> getStartupRunAlwaysTaskClasses() {
		final List<Class<?>> ret = new ArrayList<>();
		ret.add(Task00001LoadSchema.class);
		ret.add(Task00003CreateSystemRoles.class);
		ret.add(Task00004LoadStarter.class);
		ret.add(Task00005LoadFixassets.class);
		ret.add(Task00006CreateSystemLayout.class);
		ret.add(Task00007RemoveSitesearchQuartzJob.class);
		ret.add(Task00002LoadClusterLicenses.class);
		ret.add(Task00040CheckAnonymousUser.class);
		ret.add(Task00050LoadAppsSecrets.class);
        return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

	/**
	 * Handles Upgrade Tasks that must be back-ported to a given LTS release.
	 *
	 * @return The back-ported UTs.
	 */
	public static List<Class<?>> getBackportedUpgradeTaskClasses() {
		final List<Class<?>> ret = new ArrayList<>();
		return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

	/**
	 * Returns the list of data tasks that are run <b>only once</b>, which allows to solve
	 * existing data issues, data tasks are mostly tasks to solve data issues using our existing APIs.
	 * <p>
	 * The number after the "Task" word and before the description represents
	 * the system data version number in the {@code DATA_VERSION} table. A successfully
	 * executed upgrade task will add a new record in such a table with the
	 * number in the class name. This allows dotCMS to keep track of the tasks
	 * that have been run.
	 *
	 * @return The list of Run-Once Data Tasks.
	 */
	public static List<Class<?>> getStartupRunOnceDataTaskClasses() {
		final List<Class<?>> ret = ImmutableList.<Class<?>>builder()
				.add(Task230320FixMissingContentletAsJSON.class)
				.add(Task240306MigrateLegacyLanguageVariables.class)
				.build();
		return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

	/**
	 * List of tasks that are need to run without transaction. It can be all kind: Run-Once, Run-Always, Data, Backported, etc
	 * @return The list of tasks
	 */
	public static List<Class<?>> getTaskClassesNoTransaction() {
		final List<Class<?>> ret = new ArrayList<>();
		ret.add(Task241014AddTemplateValueOnContentletIndex.class);
		return ret.stream().sorted(classNameComparator).collect(Collectors.toList());
	}

}
