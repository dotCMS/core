package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
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
import com.dotmarketing.startup.runalways.Task00030ClusterInitialize;
import com.dotmarketing.startup.runalways.Task00040CheckAnonymousUser;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This utility class provides access to the lists of dotCMS tasks that are meant for the
 * initialization of a fresh dotCMS install, for fixing data inconsistencies, and the database
 * upgrade process usually associated with a new system version.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 */
public class TaskLocatorUtil {

  private static List<Class<?>> userfixTasks = new CopyOnWriteArrayList<>();

  /**
   * Returns the list of tasks that are run to solve internal conflicts related to data
   * inconsistency.
   *
   * @return The list of Fix Tasks.
   */
  private static List<Class<?>> systemfixTasks =
      ImmutableList.of(
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
          FixTask00095DeleteOrphanRelationships.class);

  /**
   * Adds a dotCMS fix task to the main fix task list.
   *
   * @param clazz - The new fix task class.
   */
  public static void addFixTask(Class<?> clazz) {
    userfixTasks.add(clazz);
  }

  /**
   * Removes the specified fix task from the main list.
   *
   * @param clazz - The fix task to remove.
   */
  public static void removeFixTask(Class<?> clazz) {
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
   * Returns the list of tasks that are run <b>only once</b>, which deal with the updating the
   * existing database objects and information to solve existing issues or to add new structures
   * associated to new features.
   *
   * <p>The number after the "Task" word and before the description represents the system version
   * number in the {@code DB_VERSION} table. A successfully executed upgrade task will add a new
   * record in such a table with the number in the class name. This allows dotCMS to keep track of
   * the tasks that have been run.
   *
   * @return The list of Run-Once Tasks.
   */
  public static List<Class<?>> getStartupRunOnceTaskClasses() {
    final List<Class<?>> ret = new ArrayList<>();
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
    ret.add(Task00920AddContentletVersionSystemHost.class);
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
    ret.add(Task03600UpdateMssqlVarcharTextColumns.class);
    ret.add(Task03605FixMSSQLMissingConstraints.class);
    ret.add(Task03700ModificationDateColumnAddedToUserTable.class);
    ret.add(Task03705AddingSystemEventTable.class);
    ret.add(Task03710AddFKForIntegrityCheckerTables.class);
    ret.add(Task03715AddFKForPublishingBundleTable.class);
    ret.add(Task03720AddRolesIntegrityCheckerTable.class);
    ret.add(Task03725NewNotificationTable.class);
    ret.add(Task03735UpdatePortletsIds.class);
    ret.add(Task03740UpdateLayoutIcons.class);
    ret.add(Task03745DropLegacyHTMLPageAndFileTables.class);
    ret.add(Task03800AddIndexLowerStructureTable.class);
    ret.add(Task04100DeleteUnusedJobEntries.class);
    ret.add(Task04105LowercaseVanityUrls.class);
    ret.add(Task04110AddColumnsPublishingPushedAssetsTable.class);
    ret.add(Task04115LowercaseIdentifierUrls.class);
    ret.add(Task04120IncreaseHostColumnOnClusterServerTable.class);
    ret.add(Task04200CreateDefaultVanityURL.class);
    ret.add(Task04205MigrateVanityURLToContent.class);
    ret.add(Task04210CreateDefaultLanguageVariable.class);
    ret.add(Task04215MySQLMissingConstraints.class);
    ret.add(Task04220RemoveDeleteInactiveClusterServersJob.class);
    ret.add(Task04230FixVanityURLInconsistencies.class);
    ret.add(Task04300UpdateSystemFolderIdentifier.class);
    ret.add(Task04305UpdateWorkflowActionTable.class);
    ret.add(Task04310CreateWorkflowRoles.class);
    ret.add(Task04315UpdateMultiTreePK.class);
    ret.add(Task04320WorkflowActionRemoveNextStepConstraint.class);
    ret.add(Task04235RemoveFKFromWorkflowTaskTable.class);
    ret.add(Task04330WorkflowTaskAddLanguageIdColumn.class);
    ret.add(Task04335CreateSystemWorkflow.class);
    ret.add(Task04340TemplateShowOnMenu.class);
    ret.add(Task04345AddSystemWorkflowToContentType.class);
    ret.add(Task04350AddDefaultWorkflowActionStates.class);
    ret.add(Task04355SystemEventAddServerIdColumn.class);
    ret.add(Task04360WorkflowSchemeDropUniqueNameConstraint.class);
    ret.add(Task04365RelationshipUniqueConstraint.class);
    ret.add(Task04370AddVisitorLogger.class);
    ret.add(Task04375UpdateColors.class);
    ret.add(Task04380AddSubActionToWorkflowActions.class);
    ret.add(Task04385UpdateCategoryKey.class);
    ret.add(Task04390ShowEditingListingWorkflowActionTable.class);
    ret.add(Task05030UpdateSystemContentTypesHost.class);
    ret.add(Task05035CreateIndexForQRTZ_EXCL_TRIGGERSTable.class);
    ret.add(Task05040LanguageTableIdentityOff.class);
    ret.add(Task05050FileAssetContentTypeReadOnlyFileName.class);
    ret.add(Task05060CreateApiTokensIssuedTable.class);

    return ret;
  }

  /**
   * Returns list of tasks that are run <b>every time</b> that dotCMS starts up. In the case of a
   * fresh install, these tasks will deploy the default database schema and data, along with the
   * information associated to the Starter Site ("demo.dotcms.com").
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
    ret.add(Task00030ClusterInitialize.class);
    ret.add(Task00040CheckAnonymousUser.class);
    return ret;
  }
}
