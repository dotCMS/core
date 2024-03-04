package com.dotcms.rest.api.v1.asset;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import javax.validation.constraints.NotNull;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractWebAssetHelperParams {

    @NotNull
    LanguageAPI languageAPI();

    @NotNull
    FileAssetAPI fileAssetAPI();

    @NotNull
    ContentletAPI contentletAPI();

    @NotNull
    BrowserAPI browserAPI();

    @NotNull
    TempFileAPI tempFileAPI();

    @NotNull
    ContentTypeAPI contentTypeAPI();

    @NotNull
    FolderAPI folderAPI();

    @NotNull
    PermissionAPI permissionAPI();

}
