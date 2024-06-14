package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.BASE_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.CONTENT_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.CREATION_DATE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HAS_TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_NAME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LANGUAGEID_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LIVE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.PUBLISH_DATE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.PUBLISH_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.PUBLISH_USER_NAME_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_NOT_FOUND;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.URL_MAP_FOR_CONTENT_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKING_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.LanguageViewStrategy.mapLanguage;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_NAME;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.FILTER_BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.TAGS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

/**
 * If any Options marked as property is found this class gets instantiated since props are most likely to be resolved here
 */
public class DefaultTransformStrategy extends AbstractTransformStrategy<Contentlet> {

    private static final String FILE_ASSET = FileAssetAPI.BINARY_FIELD;

    /**
     * Main constructor
     * @param toolBox
     */
    public DefaultTransformStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Regular transformation handler
     * @param contentlet
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotDataException, DotSecurityException {
        addCommonProperties(contentlet, map, options);
        addLanguage(contentlet, map, options);
        addVersionProperties(contentlet, map, options);
        addConstants(contentlet, map, options);
        addCategories(contentlet, map, options, user);
        addTags(contentlet, map, options, user);


        return map;
    }

    /**
     * Handle common properties found on all contentlets
     * @param contentlet
     * @param map
     * @param options
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private void addCommonProperties(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options)
            throws DotSecurityException, DotDataException {
        if(!options.contains(COMMON_PROPS)){
            return;
        }
        final ContentType type = contentlet.getContentType();

        map.put(IDENTIFIER_KEY, contentlet.getIdentifier());
        map.put(INODE_KEY, contentlet.getInode());
        map.put(TITTLE_KEY, contentlet.getTitle());
        map.put(CONTENT_TYPE_KEY, type != null ? type.variable() : NOT_APPLICABLE);
        map.put(BASE_TYPE_KEY, type != null ? type.baseType().name() : NOT_APPLICABLE);
        map.put(LANGUAGEID_KEY, contentlet.getLanguageId());

        final Optional<Field> titleImage = contentlet.getTitleImage();
        final boolean hasTitleImage = titleImage.isPresent();
        map.put(HAS_TITLE_IMAGE_KEY, hasTitleImage);
        if(hasTitleImage) {
           map.put(TITLE_IMAGE_KEY, titleImage.get().variable());
        } else {
           map.put(TITLE_IMAGE_KEY, TITLE_IMAGE_NOT_FOUND);
        }

        final Host site = toolBox.hostAPI.find(contentlet.getHost(), APILocator.systemUser(), true);
        map.put(HOST_NAME, site != null ? site.getHostname() : NOT_APPLICABLE);
        map.put(HOST_KEY, site != null ? site.getIdentifier() : NOT_APPLICABLE);

        final String urlMap = toolBox.contentletAPI
                .getUrlMapForContentlet(contentlet, toolBox.userAPI.getSystemUser(), true);
        map.put(URL_MAP_FOR_CONTENT_KEY, urlMap);
        map.put(ESMappingConstants.URL_MAP, urlMap);

        //We only calculate the fields if it is not already set
        //However WebAssets (Pages, FileAssets) are forced to calculate it.
        //To prevent any miscalculated urls.
        if (!map.containsKey(URL_FIELD)) {
            final String url = toolBox.contentHelper.getUrl(contentlet);
            if (null != url) {
                map.put(URL_FIELD, url);
            }
        }
        this.addAuditProperties(contentlet, map);

    }

    /**
     * Adds all the required audit attributes to the Contentlet's data map.
     *
     * @param contentlet           The Contentlet to add the audit properties to.
     * @param contentletProperties The map of properties to add the audit properties to.
     *
     * @throws DotDataException     An error occurred when retrieving data from the database.
     * @throws DotSecurityException A User permission error has occurred.
     */
    private void addAuditProperties(final Contentlet contentlet,
                                    final Map<String, Object> contentletProperties) throws DotDataException, DotSecurityException {
        final User modUser = Try.of(() -> toolBox.userAPI.loadUserById(contentlet.getModUser())).getOrNull();
        final User owner = Try.of(() -> toolBox.userAPI.loadUserById(contentlet.getOwner())).getOrNull();
        if (contentletProperties.containsKey(MOD_USER_KEY)) {
            contentletProperties.put(MOD_USER_NAME_KEY, null != modUser ? modUser.getFullName() : NOT_APPLICABLE);
        }
        if (contentletProperties.containsKey(OWNER_KEY)) {
            contentletProperties.put(OWNER_NAME_KEY, null != owner ? owner.getFullName() : NOT_APPLICABLE);
        }
        final Identifier identifier = toolBox.identifierAPI.find(contentlet.getIdentifier());
        if (null != identifier && UtilMethods.isSet(identifier.getId()) && !IdentifierAPI.IDENT404.equals(identifier.getAssetType())) {
            contentletProperties.put(CREATION_DATE_KEY, identifier.getCreateDate());
        }
        if (contentlet.isLive()) {
            final Optional<ContentletVersionInfo> versionInfoOpt =
                    this.toolBox.versionableAPI.getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId(), contentlet.getVariantId());
            versionInfoOpt.ifPresent(contentletVersionInfo -> contentletProperties.put(PUBLISH_DATE_KEY, contentletVersionInfo.getPublishDate()));
            contentletProperties.put(PUBLISH_USER_KEY, null != modUser ? modUser.getUserId() : NOT_APPLICABLE);
            contentletProperties.put(PUBLISH_USER_NAME_KEY, null != modUser ? modUser.getFullName() : NOT_APPLICABLE);
        }
    }

    /**
     * if TransformOptions.USE_LANGUAGE_AS_MAP is set this will load all the language related properties as a single map entry
     * @param contentlet
     * @param map
     * @param options
     */
    private void addLanguage(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options) {
        final Language language = toolBox.languageAPI.getLanguage(contentlet.getLanguageId());
        if (!options.contains(LANGUAGE_PROPS)) {
             return;
        }
        map.putAll(mapLanguage(language, false));
    }

    /**
     * Constant fields are added down here
     * @param contentlet
     * @param map
     * @param options
     */
    private void addConstants(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options){
          if(!options.contains(CONSTANTS)){
             return;
          }
            contentlet.getContentType().fields(ConstantField.class)
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(f-> map.put(f.variable(), f.values()));
    }



    /**
     * return categories as a list of key/values where the key is the categoryKey and the value is
     * the categoryName
     */
    private void addCategories(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) {
        final boolean allCategoriesInfo = options.contains(CATEGORIES_INFO);
        final boolean includeCategoryName = options.contains(CATEGORIES_NAME);
        if (includeCategoryName || allCategoriesInfo) {
            try {
                final List<Category> categories = toolBox.categoryAPI.getParents(contentlet, user, true);
                final List<CategoryField> categoryFields = contentlet.getContentType()
                        .fields(CategoryField.class).stream().filter(Objects::nonNull)
                        .map(CategoryField.class::cast).collect(Collectors.toList());

                for (final CategoryField categoryField : categoryFields) {
                    final Category parentCategory = toolBox.categoryAPI
                            .find(categoryField.values(), user, true);
                    final List<Category> childCategories = new ArrayList<>();
                    if (parentCategory != null) {
                        for (final Category category : categories) {
                            if (toolBox.categoryAPI
                                    .isParent(category, parentCategory, user, true)) {
                                childCategories.add(category);
                            }
                        }
                    }

                    if (!childCategories.isEmpty()) {
                        final List categoriesValue;
                        if (allCategoriesInfo) {
                            categoriesValue = childCategories.stream()
                                    .map(Category::getMap)
                                    .collect(Collectors.toList());
                        } else {
                            categoriesValue = childCategories.stream().map(category -> ImmutableMap
                                    .of(category.getKey(), category.getCategoryName()))
                                    .collect(Collectors.toList());
                        }
                        map.put(categoryField.variable(), categoriesValue);
                    }
                }

            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(DefaultTransformStrategy.class, String.format("An error occurred when adding Categories " +
                        "to Contentlet with ID '%s': %s", contentlet.getIdentifier(), e.getMessage()));
            }
        }
    }

    /**
     * add tags to the given contentlet
     */
    private void addTags(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) {
        final boolean includeTags = options.contains(TAGS);
        if (includeTags) {
            try {
                contentlet.setTags();
            } catch (final DotDataException e) {
                Logger.warn(DefaultTransformStrategy.class, String.format("An error occurred when adding Tags to " +
                        "Contentlet with ID '%s': %s", contentlet.getIdentifier(), e.getMessage()));
            }
        }
    }


    /**
     * Use this method to add any additional property
     * @param contentlet Same contentlet with any additional property added
     */
    private void addVersionProperties(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options)
            throws DotSecurityException, DotDataException {
        if(!options.contains(VERSION_INFO)){
            return;
        }
        final User modUser = toolBox.userAPI.loadUserById(contentlet.getModUser());
        map.put("modUserName", null != modUser ? modUser.getFullName() : NOT_APPLICABLE);
        map.put(WORKING_KEY, contentlet.isWorking());
        map.put(LIVE_KEY, contentlet.isLive());
        map.put(ARCHIVED_KEY, contentlet.isArchived());
        map.put(LOCKED_KEY, contentlet.isLocked());
        if(options.contains(USE_ALIAS)) {
            map.put("isLocked", contentlet.isLocked());
        }
        map.put("hasLiveVersion", toolBox.versionableAPI.hasLiveVersion(contentlet));

        final Optional<ContentletVersionInfo> versionInfo =
                APILocator.getVersionableAPI().getContentletVersionInfo(
                    contentlet.getIdentifier(), contentlet.getLanguageId(), contentlet.getVariantId());

        // If the contentlet is live, get the publish date from the version info
        // unless the contentlet has a publish date property already set
        final Object contentPublishDate = contentlet.get("publishDate");
        final Object versionPublishDate = versionInfo.map(ContentletVersionInfo::getPublishDate)
                .orElse(null);
        if (null != contentPublishDate) {
            map.put("publishDate", contentPublishDate);
        } else if (contentlet.isLive()) {
            map.put("publishDate", versionPublishDate != null ? versionPublishDate :
                    Try.of(contentlet::getModDate).getOrNull());
        }
    }

}
