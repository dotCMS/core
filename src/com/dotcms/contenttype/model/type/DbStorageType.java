package com.dotcms.contenttype.model.type;

public class DbStorageType implements StorageType{
	static StorageType storage = new DbStorageType();
	@Override
	public StorageType instance(){

		 return storage;
	}
}
