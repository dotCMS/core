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
import com.dotmarketing.fixtask.tasks.FixTask00021CheckOrphanedAssets;
import com.dotmarketing.fixtask.tasks.FixTask00030DeleteOrphanedAssets;
import com.dotmarketing.fixtask.tasks.FixTask00040CheckFileAssetsMimeType;
import com.dotmarketing.fixtask.tasks.FixTask00050FixInodesWithoutContentlets;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.startup.runalways.Task00003CreateSystemRoles;
import com.dotmarketing.startup.runalways.Task00004LoadStarter;
import com.dotmarketing.startup.runalways.Task00005LoadFixassets;
import com.dotmarketing.startup.runalways.Task00006CreateSystemLayout;
import com.dotmarketing.startup.runalways.Task00007RemoveSitesearchQuartzJob;
import com.dotmarketing.startup.runalways.Task00008CreateDefaultWorkflowScheme;
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
import com.dotmarketing.startup.runonce.Task00900CreateLogConsoleTable;
import com.dotmarketing.startup.runonce.Task00850DropOldFilesConstraintInWorkflow;
import com.dotmarketing.startup.runonce.Task00855FixRenameFolder;
import com.dotmarketing.startup.runonce.Task00860ExtendServerIdsMSSQL;
import com.dotmarketing.startup.runonce.Task00865AddTimestampToVersionTables;
import com.dotmarketing.startup.runonce.Task00905FixAddFolderAfterDelete;
import com.dotmarketing.startup.runonce.Task00910AddEscalationFields;

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
		ret.add(FixTask00021CheckOrphanedAssets.class);
		ret.add(FixTask00030DeleteOrphanedAssets.class);
		ret.add(FixTask00040CheckFileAssetsMimeType.class);
		ret.add(FixTask00050FixInodesWithoutContentlets.class);

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
		ret.add(Task00008CreateDefaultWorkflowScheme.class);
		return ret;
	}
}
