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
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKING_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;
import static com.liferay.portal.language.LanguageUtil.getLiteralLocale;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * If any Options marked as property is found this class gets instantiated since props are most likely to be resolved here
 */
public class DefaultTransformStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Typical constructor
     * @param toolBox
     */
    @VisibleForTesting
    public DefaultTransformStrategy(final TransformToolbox toolBox) {
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
        final boolean present = titleImage.isPresent();
        map.put(HAS_TITLE_IMAGE_KEY, present);
        if (present) {
            map.put(TITLE_IMAGE_KEY, titleImage.get().variable());
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
        if (options.contains(LANGUAGE_VIEW)) {
            map.putAll(mapLanguage(language, true));
        }
        if (options.contains(LANGUAGE_PROPS)) {
            map.putAll(mapLanguage(language, false));
        }
    }

    /**
     * Lang functions now relocated here.
     * @param language
     * @param wrapAsMap
     * @return
     */
    private Map<String, Object> mapLanguage(final Language language, final boolean wrapAsMap) {

        final Builder<String, Object> builder = new Builder<>();

        builder
                .put("languageId", language.getId())
                .put("language", language.getLanguage())
                .put("languageCode", language.getLanguageCode())
                .put("country", language.getCountry())
                .put("countryCode", language.getCountryCode())
                .put("languageFlag", getLiteralLocale(language.getLanguageCode(), language.getCountryCode()));

        final String iso = UtilMethods.isSet(language.getCountryCode())
                ? language.getLanguageCode() + StringPool.DASH + language.getCountryCode()
                : language.getLanguageCode();
        builder.put("isoCode", iso.toLowerCase());

        if(wrapAsMap){
            builder.put("id", language.getId());
            return ImmutableMap.of("languageMap", builder.build(), "language",language.getLanguage());
        }
        return builder.build();
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
     */
    private void addBinaries(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options) {

        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);

        if (binaries.isEmpty()) {
            return;
        }

        // if we want to include binaries as they are (java.io.File) this is the flag you should turn on.
        if (options.contains(BINARIES)) {
            for (final Field field : binaries) {
                try {
                    final File conBinary = contentlet.getBinary(field.variable());
                    if (conBinary != null) {
                        map.put(field.variable(), conBinary);
                    }
                } catch (IOException e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
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
