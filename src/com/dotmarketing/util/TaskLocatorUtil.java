package com.dotmarketing.util;

import com.dotmarketing.fixtask.tasks.*;
import com.dotmarketing.startup.runalways.*;
import com.dotmarketing.startup.runonce.*;

import java.util.ArrayList;
import java.util.List;

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
		ret.add(FixTask00060FixAssetType.class);
		ret.add(FixTask00070FixVersionInfo.class);
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
		ret.add(Task00911CreateDefaultWorkflowSchema.class);
        ret.add( Task00920AddContentletVersionSystemHost.class );
        ret.add(Task00922FixdotfolderpathMSSQL.class);
        ret.add(Task00925UserIdTypeChange.class);
        ret.add(Task00930AddIdentifierIndex.class);
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
