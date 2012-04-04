package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.List;

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
import com.dotmarketing.startup.runonce.Task00769UpdateTagDataModel;
import com.dotmarketing.startup.runonce.Task00775DropUnusedTables;
import com.dotmarketing.startup.runonce.Task00780UUIDTypeChange;
import com.dotmarketing.startup.runonce.Task00782CleanDataInconsistencies;
import com.dotmarketing.startup.runonce.Task00785DataModelChanges;
import com.dotmarketing.startup.runonce.Task00790DataModelChangesForWebAssets;
import com.dotmarketing.startup.runonce.Task00795LiveWorkingToIdentifier;
import com.dotmarketing.startup.runonce.Task00800CreateTemplateContainers;
import com.dotmarketing.startup.runonce.Task00805AddRenameFolderProcedure;
import com.dotmarketing.startup.runonce.Task00807CreateTagStorageFieldOnHostStructure;
import com.dotmarketing.startup.runonce.Task00810FilesAsContentChanges;
import com.dotmarketing.startup.runonce.Task00815WorkFlowTablesChanges;
import com.dotmarketing.startup.runonce.Task00820CreateNewWorkFlowTables;
import com.dotmarketing.startup.runonce.Task00825UpdateLoadRecordsToIndex;
import com.dotmarketing.startup.runonce.Task00835CreateIndiciesTables;


public class TaskLocatorUtil {

	public static List<Class<?>> getFixTaskClasses() {
		List<Class<?>> ret = new ArrayList<Class<?>>();

		return ret;
	}

	public static List<Class<?>> getStartupRunOnceTaskClasses() {
		List<Class<?>> ret = new ArrayList<Class<?>>();
		ret.add(Task00760AddContentletStructureInodeIndex.class);
		ret.add(Task00765AddUserForeignKeys.class);
		ret.add(Task00807CreateTagStorageFieldOnHostStructure.class);
		ret.add(Task00769UpdateTagDataModel.class);
		ret.add(Task00775DropUnusedTables.class);
		ret.add(Task00780UUIDTypeChange.class);
		ret.add(Task00782CleanDataInconsistencies.class);
		ret.add(Task00785DataModelChanges.class);
		ret.add(Task00790DataModelChangesForWebAssets.class);
		ret.add(Task00766AddFieldVariableTable.class);
		ret.add(Task00767FieldVariableValueTypeChange.class);
		ret.add(Task00795LiveWorkingToIdentifier.class);
		ret.add(Task00800CreateTemplateContainers.class);
        ret.add(Task00805AddRenameFolderProcedure.class);
		ret.add(Task00810FilesAsContentChanges.class);
		ret.add(Task00815WorkFlowTablesChanges.class);
		ret.add(Task00820CreateNewWorkFlowTables.class);
		ret.add(Task00825UpdateLoadRecordsToIndex.class);
		ret.add(Task00835CreateIndiciesTables.class);
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
