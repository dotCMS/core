package com.dotcms.datagen;

import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;

public class DotAssetDataGen extends ContentletDataGen {

  public DotAssetDataGen(final Host host, final File file) throws DotSecurityException, DotDataException {
    this(file);
    this.host = host;
  }

  public DotAssetDataGen(final Host host, final Folder folder, final File file)
      throws DotSecurityException, DotDataException {
    this(file);
    this.folder = folder;
    this.host = host;
  }


  public DotAssetDataGen(final File file) throws DotDataException, DotSecurityException {

    super(APILocator.getContentTypeAPI(APILocator.systemUser())
        .find("DotAsset").id());
    this.user = APILocator.systemUser();
    setProperty(DotAssetContentType.ASSET_FIELD_VAR, file);
  }

  public DotAssetDataGen folder(final String folder) {
    setProperty(Contentlet.FOLDER_KEY, folder);
    return this;
  }

  public DotAssetDataGen site(final String site) {
    setProperty(DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR, site);
    return this;
  }

  public DotAssetDataGen tags(String... tags) {
    setProperty(DotAssetContentType.TAGS_FIELD_VAR, String.join(",", tags));
    return this;
  }


}
