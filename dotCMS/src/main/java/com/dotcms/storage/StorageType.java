package com.dotcms.storage;

/**
 * Defines the default storages on dotCMS, however the api only uses the name as a reference, so you can
 * create your own implemetation and just use the enum name string as key
 * @author jsanca
 */
public enum StorageType {

    FILE_SYSTEM, DB, S3
}
