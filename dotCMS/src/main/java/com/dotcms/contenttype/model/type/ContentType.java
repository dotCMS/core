package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.component.ImmutableSiteAndFolder;
import com.dotcms.contenttype.model.component.SiteAndFolder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Default;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz"
)
@JsonTypeIdResolver(value = ContentType.ClassNameAliasResolver.class)
@JsonSubTypes({
        @Type(value = FileAssetContentType.class),
        @Type(value = FormContentType.class),
        @Type(value = PageContentType.class),
        @Type(value = PersonaContentType.class),
        @Type(value = SimpleContentType.class),
        @Type(value = WidgetContentType.class),
        @Type(value = VanityUrlContentType.class),
        @Type(value = KeyValueContentType.class),
        @Type(value = DotAssetContentType.class)
})
public abstract class ContentType implements Serializable, Permissionable, ContentTypeIf,
        ManifestItem {

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

  private static final long serialVersionUID = 1L;

  Boolean hasStoryBlockFields = null;

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
    hostDefaultNotChanged = true;
    return Host.SYSTEM_HOST;
  }

  //This property help me determine if I'm seeing the default value or something explicitly set
  private boolean hostDefaultNotChanged = false;

  /**
   * by default our site name is the same as SYSTEM_HOST
   * @return
   */
  @Nullable
  @Value.Default
  public String siteName() {
    siteNameDefaultNotChanged = true;
    return canonicalSiteName();
  }

  private boolean siteNameDefaultNotChanged = false;

  private String canonicalSiteName(){

    final String host = host();

    if (UtilMethods.isNotSet(host) || Host.SYSTEM_HOST.equals(host)) {
      return Host.SYSTEM_HOST_NAME;
    }
    if (UUIDUtil.isUUID(host)) {
      return Try.of(() -> APILocator.getHostAPI().find(host, APILocator.systemUser(), false)
              .getHostname()).getOrNull();
    }
    return Try.of(
            () -> APILocator.getHostAPI().resolveHostName(host, APILocator.systemUser(), false)
                    .getHostname()).getOrNull();

  }


  @Nullable
  @Value.Default
  public String icon() {
    return null;
  }

  @Value.Default
  public int sortOrder() {
    return 0;
  }

  @Nullable
  @Value.Default
  public Map<String, ? extends Object> metadata() {
    return null;
  }

  @JsonIgnore
  @Value.Lazy
  public List<Field> fields() {
    if (innerFields == null) {
      this.hasStoryBlockFields = null;
      try {
        innerFields = APILocator.getContentTypeFieldAPI().byContentTypeId(this.id());
      } catch (final DotDataException e) {
        final String errorMsg = String.format("Unable to load fields for Content Type '%s' [%s]: %s", this.name(),
                this.id(), e.getMessage());
        Logger.error(this, errorMsg);
        throw new DotStateException(errorMsg, e);
      }
    }
    return innerFields;
  }

  /**
   * This method will return a list of fields that are of a specific Field class e.g.
   * <code>contentType.fields(BinaryField.class);</code> will return all the binary fields on that
   * content type. You can also pass in the ImmutableClass, e.g.
   * <code>contentType.fields(ImmutableBinaryField.class);</code> will return all the binary fields as
   * well
   * 
   * @param clazz
   * @return
   */
  @JsonIgnore
  @Value.Lazy
  public List<Field> fields(final Class<? extends Field> clazz) {
    final String clazzName = UtilMethods.replace(clazz.getName(),".Immutable",".");
    return this.fields()
        .stream()
        .filter(field -> 
        UtilMethods.replace(field.getClass().getName(),".Immutable",".").equals(clazzName) 
    )
    .collect(Collectors.toList());
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

    /**
     * Alternative method to get a map of fields using a custom key generator, useful when you want
     * to use an alternative to the field variable as the key.
     * <p>
     * Calling the regular fieldMap() method with fields that have null variables will throw a
     * NullPointerException.
     *
     * @param keyGenerator A function that generates a key for the field
     * @return A map of fields with the generated key as the key
     */
    @JsonIgnore
    @Value.Lazy
    public Map<String, Field> fieldMap(Function<Field, String> keyGenerator) {
        Map<String, Field> fmap = new HashMap<>();
        for (Field f : this.fields()) {
            fmap.put(keyGenerator.apply(f), f);
        }
        return Map.copyOf(fmap);
    }

  private List<Field> innerFields = null;

  public void constructWithFields(List<Field> fields) {

    innerFields = fields;
  }

  @Value.Default
  public String folder() {
    folderDefaultNotChanged = true;
    return Folder.SYSTEM_FOLDER;
  }

  //This property help me determine if I'm seeing the default value or something explicitly set
  private boolean folderDefaultNotChanged = false;

  /**
   * By default, our system folder is "/"
   * @return
   */
  @Nullable
  @Value.Default
  public String folderPath() {
    folderPathDefaultNotChanged = true;
    return canonicalFolderPath();
  }

  private boolean folderPathDefaultNotChanged = false;

  private String canonicalFolderPath(){
    final String folder = folder();
    if (Folder.SYSTEM_FOLDER.equals(folder)) {
        return Folder.SYSTEM_FOLDER_PATH;
    }

    final String host = host();
    final FolderAPI folderAPI = APILocator.getFolderAPI();
    final HostAPI hostAPI = APILocator.getHostAPI();
    return Try.of(() -> {
              final String hostName =
                      UUIDUtil.isUUID(host) ?
                              hostAPI.find(host, APILocator.systemUser(), false).getHostname() :
                              resolveHostNameOrSystemHost(host, hostAPI);
              final String path = folderAPI.find(folder, APILocator.systemUser(), false).getPath();
              return String.format("%s%s%s", hostName, StringPool.COLON, path);
            }
    ).getOrNull();
  }

    private static String resolveHostNameOrSystemHost(final String host, final HostAPI hostAPI) throws DotDataException, DotSecurityException {
        return Host.SYSTEM_HOST.equals(host) ?
                Host.SYSTEM_HOST_SITENAME : hostAPI.resolveHostName(host, APILocator.systemUser(), false).getHostname();
    }

    /**
   * The code below serves as
   * @return
   */
  @JsonIgnore
  @Auxiliary
  public SiteAndFolder siteAndFolder() {
    return ImmutableSiteAndFolder.builder()
            //Here we need to know if we're looking at the default value or a value set
            .host( hostDefaultNotChanged ? null : host())
            .folder( folderDefaultNotChanged ? null : folder())
            // These are calculated fields
            .folderPath( folderPathDefaultNotChanged ? null : folderPath())
            .siteName( siteNameDefaultNotChanged ? null : siteName())
            .build();
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
      Permissionable parent = null;
      if (UtilMethods.isSet(this.folder()) && !FolderAPI.SYSTEM_FOLDER.equals(this.folder())) {
        parent= APILocator.getFolderAPI().find(folder(), APILocator.systemUser(), false);
      }else if(UtilMethods.isSet(host()) && !host().equals(Host.SYSTEM_HOST)){
        parent= APILocator.getHostAPI().find(host(), APILocator.systemUser(), false);
      } else {
        parent= APILocator.systemHost();
      }
      if(parent==null) {
        parent= getProxyPermissionable();
      }
      return parent;
    }catch (Exception e) {
      throw new DotRuntimeException(e.getMessage(), e);
    }
  }
  
  /**
   * this is only needed at startup when there is no
   * data and the api calls to get the pemissionableParent return null;
   * @return
   */
  private Permissionable getProxyPermissionable() {
    final PermissionableProxy proxy = new PermissionableProxy();
    if (FolderAPI.SYSTEM_FOLDER.equals(this.folder())) {
      proxy.setIdentifier(this.host());
      proxy.setInode(this.host());
      proxy.setType(Host.class.getCanonicalName());
    } else {
      proxy.setIdentifier(this.folder());
      proxy.setInode(this.folder());
      proxy.setType(Folder.class.getCanonicalName());
    }
    return proxy;
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
                  BaseContentType.FILEASSET, Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE",false),
                  BaseContentType.PERSONA,   Config.getBooleanProperty("DEFAULT_PERSONA_TO_DEFAULT_LANGUAGE",false)
                  );

  @JsonIgnore
  @Value.Lazy
  public boolean languageFallback() {

      return languageFallbackMap.getOrDefault(baseType(), false);
  }

  @JsonIgnore
  @Override
  public ManifestInfo getManifestInfo(){
    return new ManifestInfoBuilder()
        .objectType(PusheableAsset.CONTENT_TYPE.getType())
        .id(this.id())
        .title(this.name())
        .siteId(this.host())
        .folderId(this.folder())
        .build();
  }

  /**
   * Checks whether this Content Type has any {@link StoryBlockField} fields or not.
   *
   * @return If there is at least one field of type Story Block, returns {@code true}.
   */
  @JsonIgnore
  public boolean hasStoryBlockFields() {
    if (null == this.hasStoryBlockFields) {
      this.hasStoryBlockFields = !this.fields(StoryBlockField.class).isEmpty();
    }
    return this.hasStoryBlockFields;
  }

  @JsonIgnore
  @Default
  public boolean markedForDeletion() { return false; }

  static class ClassNameAliasResolver extends ClassNameIdResolver {
    static TypeFactory typeFactory = TypeFactory.defaultInstance();
    public ClassNameAliasResolver() {
      super(typeFactory.constructType(new TypeReference<ContentType>() {
      }), typeFactory,
       BasicPolymorphicTypeValidator.builder().allowIfSubType(ContentType.class).build()
      );
    }

    @Override
    public JavaType typeFromId(final DatabindContext context, final String id) throws IOException {
      final String packageName = ContentType.class.getPackageName();
      if( !id.contains(".") && !id.startsWith(packageName)){
        final String className = String.format("%s.Immutable%s",packageName,id);
        return super.typeFromId(context, className);
      }
      return super.typeFromId(context, id);
    }

  }


}
