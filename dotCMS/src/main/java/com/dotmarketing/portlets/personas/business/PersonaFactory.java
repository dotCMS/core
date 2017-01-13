package com.dotmarketing.portlets.personas.business;

import com.dotmarketing.exception.DotDataException;

public interface PersonaFactory {

	String INSERT_DEFAULT_STRUC_QUERY = "insert into structure (inode, name, description, structuretype, system, fixed, velocity_var_name, host, folder, mod_date, default_structure) values ( ?, ?, ?, ?, ?, ?, ?, ?, ? ,?,?)";
	String INSERT_DEFAULT_STRUC_INODE_QUERY = "insert into inode (inode, owner, idate, type) values (?, ?, ?, ?)";

	// make sure we have inode and structure entries
	String SELECT_DEFAULT_STRUC_QUERY = "select inode.inode from inode,structure where inode.inode = ? and structure.inode = inode.inode";

	// these need to be fixed and identical in all installations
	String[] DEFAUTL_PERSONA_FIELD_INODES = { "606ac3af-63e5-4bd4-bfa1-c4c672bb8eb8", "0ea2bd92-4b2d-48a2-a394-77fd560b1fce",
		"6b25d960-034d-4030-b785-89cc01baaa3d", "07cfbc2c-47de-4c78-a411-176fe8bb24a5", "2dab7223-ebb5-411b-922f-611a30bc2a2b",
		"65e4e742-d87a-47ff-84ef-fde44e889e27", "f9fdd242-6fac-4d03-9fa3-b346d6995779" };

	void createDefualtPersonaStructure() throws DotDataException;

}
