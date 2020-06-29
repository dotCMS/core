package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.BASE_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.CONTENT_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HAS_TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_NAME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LANGUAGEID_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LIVE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_NOT_FOUND;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKING_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.LanguageViewStrategy.mapLanguage;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_NAME;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.FILTER_BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

import com.dotcms.api.APIProvider;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * If any Options marked as property is found this class gets instantiated since props are most likely to be resolved here
 */
public class DefaultTransformStrategy extends AbstractTransformStrategy<Contentlet> {

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
        addBinaries(contentlet, map, options);
        addCategories(contentlet, map, options, user);
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
        final Host host = toolBox.hostAPI.find(contentlet.getHost(), APILocator.systemUser(), true);
        map.put(HOST_NAME, host != null ? host.getHostname() : NOT_APPLICABLE);
        map.put(HOST_KEY, host != null ? host.getIdentifier() : NOT_APPLICABLE);

        final String urlMap = toolBox.contentletAPI
                .getUrlMapForContentlet(contentlet, toolBox.userAPI.getSystemUser(), true);
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
     * This method includes binaries in the resulting view if so is indicated
     * After the execution of this method if the BINARIES flag is turned on
     * all the binary fields will be replaced and transformed by a /dA/.. path.
     */
    private void addBinaries(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options) {

        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);

        if (binaries.isEmpty()) {
            return;
        }

        //If we dont want any binaries making it into the final map
        if(options.contains(FILTER_BINARIES)){
            binaries.forEach(field -> {
                map.remove(field.variable());
            });
            Logger.info(DefaultTransformStrategy.class, ()->"Transformer was instructed to exclude binaries.");
            return;
        }

        // if we want to include binaries as they are (java.io.File) this is the flag you should turn on.
        if (options.contains(BINARIES)) {
            for (final Field field : binaries) {
                try {
                    final String velocityVarName = field.variable();
                    //Extra precaution in case we are attempting to process a contentlet that has already been transformed.
                    if (map.get(velocityVarName) instanceof File) {
                        final File conBinary = (File) map.get(velocityVarName); //contentlet.getBinary(field.variable());
                        if (null != conBinary) {
                            //The binary-field per se. Must be replaced by file-name. We dont want to disclose any file specifics.
                            //TODO: in a near future this must be read from a pre-cached metadata.
                            final String dAPath = "/dA/%s/%s/%s";
                            map.put(field.variable() + "Version",
                                    String.format(dAPath, contentlet.getInode(),
                                            field.variable(), conBinary.getName()));
                            map.put(field.variable(),
                                    String.format(dAPath, contentlet.getIdentifier(),
                                            field.variable(), conBinary.getName()));
                            map.put(field.variable() + "ContentAsset",
                                    contentlet.getIdentifier() + "/" + field.variable());
                        } else {
                            Logger.warn(FileAssetViewStrategy.class,"We're missing a binary");
                        }
                    }
                } catch (Exception e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
            }
        }
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

            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(DefaultTransformStrategy.class,
                        "Unable to get categories from content with id ");
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

        map.put("publishDate", Try.of(contentlet::getModDate).getOrNull());

    }

}
