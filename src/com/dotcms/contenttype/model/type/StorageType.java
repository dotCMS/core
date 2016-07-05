package com.dotcms.contenttype.model.type;

public interface StorageType {

	default StorageType instance(){
		return new LegacyDBStorageType();
	}

}
