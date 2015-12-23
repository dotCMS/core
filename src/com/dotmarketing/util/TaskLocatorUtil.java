package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.fixtask.tasks.FixTask00001CheckAssetsMissingIdentifiers;
import com.dotmarketing.fixtask.tasks.FixTask00003CheckContainersInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00004CheckFileAssetsInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00005CheckHTMLPagesInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00006CheckLinksInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00007CheckTemplatesInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00008CheckTreeInconsistencies;
import com.dotmarketing.fixtask.tasks.FixTask00009CheckContentletsInexistentInodes;
import com.dotmarketing.fixtask.tasks.FixTask00011RenameHostInFieldVariableName;
import com.dotmarketing.fixtask.tasks.FixTask00012UpdateAssetsHosts;
import com.dotmarketing.fixtask.tasks.FixTask00020DeleteOrphanedIdentifiers;
import com.dotmarketing.fixtask.tasks.FixTask00030DeleteOrphanedAssets;
import com.dotmarketing.fixtask.tasks.FixTask00040CheckFileAssetsMimeType;
import com.dotmarketing.fixtask.tasks.FixTask00050FixInodesWithoutContentlets;
import com.dotmarketing.fixtask.tasks.FixTask00060FixAssetType;
import com.dotmarketing.fixtask.tasks.FixTask00070FixVersionInfo;
import com.dotmarketing.fixtask.tasks.FixTask00080DeleteOrphanedContentTypeFields;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.startup.runalways.Task00009ClusterInitialize;
import com.dotmarketing.startup.runalways.Task00010CheckAnonymousUser;
import com.dotmarketing.startup.runalways.Task00003CreateSystemRoles;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.dotmarketing.startup.runalways.Task00005LoadFixassets;
import com.dotmarketing.startup.runalways.Task00006CreateSystemLayout;
import com.dotmarketing.startup.runalways.Task00007RemoveSitesearchQuartzJob;

import com.dotmarketing.startup.runonce.*;


public class TaskLocatorUtil {

	public static List<Class<?>> getFixTaskClasses() {
		List<Class<?>> ret = new ArrayList<Class<?>>();
		ret.add(FixTask00001CheckAssetsMissingIdentifiers.class);
		ret.add(FixTask00003CheckContainersInconsistencies.class);
		ret.add(FixTask00004CheckFileAssetsInconsistencies.class);
		ret.add(FixTask00005CheckHTMLPagesInconsistencies.class);
		ret.add(FixTask00006CheckLinksInconsistencies.class);
		ret.add(FixTask00007CheckTemplatesInconsistencies.class);
		ret.add(FixTask00008CheckTreeInconsistencies.class);
		ret.add(FixTask00009CheckContentletsInexistentInodes.class);
		ret.add(FixTask00011RenameHostInFieldVariableName.class);
		ret.add(FixTask00012UpdateAssetsHosts.class);
		ret.add(FixTask00020DeleteOrphanedIdentifiers.class);
		ret.add(FixTask00030DeleteOrphanedAssets.class);
		ret.add(FixTask00040CheckFileAssetsMimeType.class);
		ret.add(FixTask00050FixInodesWithoutContentlets.class);
		ret.add(FixTask00060FixAssetType.class);
		ret.add(FixTask00070FixVersionInfo.class);
		ret.add(FixTask00080DeleteOrphanedContentTypeFields.class);
		return ret;
	}

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
        return ret;
    }

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
