package com.dotmarketing.business;
public enum PermissionLevel {
	
	
/*
	final int PERMISSION_USE = 1;
	final int PERMISSION_EDIT = 2;
	final int PERMISSION_WRITE = 2;
	final int PERMISSION_PUBLISH = 4;
	final int PERMISSION_EDIT_PERMISSIONS = 8;
	final int PERMISSION_CAN_ADD_CHILDREN = 16;
*/
	
	NONE(0),
	READ(1),
	USE(1),
	EDIT(2),
	WRITE(2),
	PUBLISH(4),
	EDIT_PERMISSIONS(8),
	CAN_ADD_CHILDREN(16);

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	int type;

	
	PermissionLevel(int type) {
		this.type = type;
	}

	/**
	 * Gets the integer representation of this value.
	 * @return the integer representation
     */
	public int getType() {
		return type;
	}

	public static PermissionLevel getPermissionLevel (int value) {
		PermissionLevel[] types = PermissionLevel.values();
		for (PermissionLevel type : types) {
			if (type.type==value){
				return type;
			}
		}
		return NONE;
	}
	

}