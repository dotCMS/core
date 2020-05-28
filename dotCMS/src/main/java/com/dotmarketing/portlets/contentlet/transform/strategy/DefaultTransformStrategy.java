package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.BASE_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.CONTENT_TYPE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HAS_TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_NAME;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LIVE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKING_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES_AS_MAP;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.IDENTIFIER_AS_MAP;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_VERSION_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_AS_MAP;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolBox.NA;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolBox.mapIdentifier;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolBox.mapLanguage;
import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DefaultTransformStrategy extends AbstractTransformStrategy<Contentlet> {

    DefaultTransformStrategy(final TransformToolBox toolBox) {
        super(toolBox);
    }

    @Override
    public Contentlet fromContentlet(final Contentlet contentlet) {
        return contentlet;
    }

    @Override
    public Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options)
            throws DotDataException, DotSecurityException {
        addCommonProperties(contentlet, map, options);
        addIdentifier(contentlet, map, options);
        addLanguage(contentlet, map, options);
        addVersionProperties(contentlet, map, options);
        addConstants(contentlet, map, options);
        addBinaries(contentlet, map, options);
        return map;
    }

    private void addCommonProperties(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options)
            throws DotSecurityException, DotDataException {
        if(!options.contains(INC_COMMON_PROPS)){
            return;
        }
        final ContentType type = contentlet.getContentType();

        map.put(IDENTIFIER_KEY, contentlet.getIdentifier());
        map.put(INODE_KEY, contentlet.getInode());
        map.put(TITTLE_KEY, contentlet.getTitle());
        map.put(CONTENT_TYPE_KEY, type != null ? type.variable() : NA);
        map.put(BASE_TYPE_KEY, type != null ? type.baseType().name() : NA);
        map.put("languageId", contentlet.getLanguageId());
        final Optional<Field> titleImage = contentlet.getTitleImage();
        final boolean present = titleImage.isPresent();
        map.put(HAS_TITLE_IMAGE_KEY, present);
        if (present) {
            map.put(TITLE_IMAGE_KEY, titleImage.get().variable());
        }

        final Host host = toolBox.hostAPI.find(contentlet.getHost(), APILocator.systemUser(), true);
        map.put(HOST_NAME, host != null ? host.getHostname() : NA);
        map.put(HOST_KEY, host != null ? host.getIdentifier() : NA);

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
        if (options.contains(LANGUAGE_AS_MAP)) {
            map.putAll(mapLanguage(language, true));
        }
        if (options.contains(LANGUAGE_PROPS)) {
            map.putAll(mapLanguage(language, false));
        }
    }

    /**
     * if TransformOptions.USE_IDENTIFIER_AS_MAP is set this will load all the identifier related properties as a single map entry
     * @param contentlet
     * @param map
     * @param options
     * @throws DotDataException
     */
    private void addIdentifier(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options) throws DotDataException {
        if (!options.contains(IDENTIFIER_AS_MAP)) {
            return;
        }

        final Identifier id = toolBox.identifierAPI.find(contentlet.getIdentifier());
        map.putAll(mapIdentifier(id,true));
    }



    /**
     * Constant fields are added down here
     * @param contentlet
     * @param map
     * @param options
     */
    private void addConstants(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options){
          if(!options.contains(INC_CONSTANTS)){
             return;
          }
            contentlet.getContentType().fields(ConstantField.class)
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(f-> map.put(f.variable(), f.values()));
    }


    /**
     * This method
     * @param contentlet
     * @param map
     * @param options
     */
    private void addBinaries(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options) {

        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);

        if (binaries.isEmpty()) {
            return;
        }
        //This emulates the behavior we had on BinaryToMapTransformer
        if (options.contains(BINARIES_AS_MAP)) {
            for (final Field field : binaries) {

                try {
                    map.put(field.variable() + "Map", TransformToolBox.transform(field, contentlet));

                    final File conBinary = contentlet.getBinary(field.variable());
                    if (conBinary != null) {
                        //This clearly replaces the binary by a string which is the expected output on BinaryToMapTransformer.
                        map.put(field.variable(), conBinary.getName());
                    }
                } catch (IOException e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
            }
        } else {
           // if we want to include binaries as they are (java.io.File) this is the flag you should turn on.
            if (options.contains(INC_BINARIES)) {
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
    }

    /**
     * Use this method to add any additional property
     * @param contentlet Same contentlet with any additional property added
     */
    private void addVersionProperties(final Contentlet contentlet, final Map<String, Object> map, final Set<TransformOptions> options)
            throws DotSecurityException, DotDataException {
        if(!options.contains(INC_VERSION_INFO)){
            return;
        }
        final User modUser = toolBox.userAPI.loadUserById(contentlet.getModUser());
        map.put("modUserName", null != modUser ? modUser.getFullName() : NA);
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
