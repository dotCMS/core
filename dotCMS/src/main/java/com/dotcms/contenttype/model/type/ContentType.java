package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import java.io.Serializable;
import java.util.*;

@JsonTypeInfo(
        use = Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz"
)
@JsonSubTypes({
        @Type(value = FileAssetContentType.class),
        @Type(value = FormContentType.class),
        @Type(value = PageContentType.class),
        @Type(value = PersonaContentType.class),
        @Type(value = SimpleContentType.class),
        @Type(value = WidgetContentType.class),
        @Type(value = VanityUrlContentType.class),
        @Type(value = KeyValueContentType.class),
})
public abstract class ContentType implements Serializable, Permissionable, ContentTypeIf {

  @Value.Check
  protected void check() {
    Preconditions.checkArgument(StringUtils.isNotEmpty(name()), "Name cannot be empty for " + this.getClass());

    if (!(this instanceof UrlMapable)) {
      Preconditions.checkArgument(detailPage() == null, "Detail Page cannot be set for " + this.getClass());
      Preconditions.checkArgument(urlMapPattern() == null, "urlmap cannot be set for " + this.getClass());
    }
    if (!(this instanceof Expireable)) {
      Preconditions.checkArgument(expireDateVar() == null, "expireDateVar cannot be set for " + this.getClass());
      Preconditions.checkArgument(publishDateVar() == null, "publishDateVar cannot be set for " + this.getClass());
    }
  }

  static final long serialVersionUID = 1L;

  public abstract String name();

  @Nullable
  public abstract String id();


  @Nullable
  @Value.Lazy
  public String inode() {
    return id();
  }

  @Nullable
  public abstract String description();

  @Value.Default
  public boolean defaultType() {
    return false;
  }

  @Value.Default
  @Nullable
  public String detailPage() {
    return null;
  }

  @Value.Default
  public boolean fixed() {
    return false;
  }

  @Value.Default
  public Date iDate() {
    return DateUtils.round(new Date(), Calendar.SECOND);
  }

  @Value.Default
  public boolean system() {
    return false;
  }

  @Value.Default
  public boolean versionable() {
    return true;
  }

  @Value.Default
  public boolean multilingualable() {
    return false;
  }

  @Nullable
  public abstract String variable();

  @Nullable
  @Value.Default
  public String urlMapPattern() {
    return null;
  }

  @Nullable
  @Value.Default
  public String publishDateVar() {
    return null;
  }

  @Nullable
  @Value.Default
  public String expireDateVar() {
    return null;
  }

  @Nullable
  @Value.Default
  public String owner() {
    return null;
  }

  @Value.Default
  public Date modDate() {
    return DateUtils.round(new Date(), Calendar.SECOND);
  }

  public abstract BaseContentType baseType();

  @Value.Default
  public String host() {
    return Host.SYSTEM_HOST;
  }

  @JsonIgnore
  @Value.Lazy
  public List<Field> fields() {
    if (innerFields == null) {
      try {

        innerFields = APILocator.getContentTypeFieldAPI().byContentTypeId(this.id());
      } catch (DotDataException e) {
        throw new DotStateException("unable to load fields:" + e.getMessage(), e);
      }
    }
    return innerFields;
  }

  @JsonIgnore
  @Value.Lazy
  public Map<String, Field> fieldMap() {
    Map<String, Field> fmap = new HashMap<>();
    for (Field f : this.fields()) {
      fmap.put(f.variable(), f);
    }
    return ImmutableMap.copyOf(fmap);
  }
  private List<Field> innerFields = null;

  public void constructWithFields(List<Field> fields) {

    innerFields = fields;
  }

  @Value.Default
  public String folder() {
    return Folder.SYSTEM_FOLDER;
  }

  @JsonIgnore
  public Permissionable permissionable() {
    return this;
  }

  @JsonIgnore
  @Override
  public String getPermissionId() {
    return id();
  }

  @Override
  public String getOwner() {
    return owner();
  }

  @Override
  public void setOwner(String x) {
    throw new DotStateException("Cannot change the owner for an immutable value");
  }

  @JsonIgnore
  @Override
  public List<PermissionSummary> acceptedPermissions() {
    return ImmutableList.of(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ),
            new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE),
            new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH),
            new PermissionSummary("edit-permissions", "edit-permissions-permission-description",
                    PermissionAPI.PERMISSION_EDIT_PERMISSIONS));

  }

  @JsonIgnore
  @Value.Lazy
  public Permissionable getParentPermissionable() {
    try{
      if (FolderAPI.SYSTEM_FOLDER.equals(this.folder())) {
        PermissionableProxy host = new PermissionableProxy();
        host.setIdentifier(this.host());
        host.setInode(this.host());
        host.setType(Host.class.getCanonicalName());
        return host;
      } else {
        return APILocator.getFolderAPI().find(this.folder(), APILocator.systemUser(), false);
      }
    }catch (Exception e) {
      throw new DotRuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public boolean isParentPermissionable() {
    return true;
  }

  @JsonIgnore
  @Override
  public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
    return null;
  }

  @JsonIgnore
  @Override
  public String getPermissionType() {
    return Structure.class.getCanonicalName();
  }

  @JsonIgnore
  @Default
  public List<Field> requiredFields() {
    return ImmutableList.of();
  }

  private final static Map<BaseContentType, Boolean> languageFallbackMap =
          CollectionsUtils.imap(
                  BaseContentType.CONTENT,   Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",false),
                  BaseContentType.WIDGET,    Config.getBooleanProperty("DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE", false),
                  BaseContentType.FILEASSET, Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE",false)
                  );

  @JsonIgnore
  @Value.Lazy
  public boolean languageFallback() {

      return languageFallbackMap.getOrDefault(baseType(), false);
  }

}