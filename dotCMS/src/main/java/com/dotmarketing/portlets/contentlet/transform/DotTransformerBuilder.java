package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolver;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolverImpl;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.AVOID_MAP_SUFFIX_FOR_VIEWS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_NAME;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CLEAR_EXISTING_DATA;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.DATETIME_FIELDS_TO_TIMESTAMP;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.FILEASSET_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.HISTORY_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.IDENTIFIER_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.KEY_VALUE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.SITE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.TAGS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.util.UtilMethods.isNotSet;

/**
 * This Class is the entry point to realize any transformation
 * it facilitates instantiating the Transformer
 */
public class DotTransformerBuilder {

    private final List<Contentlet> contentlets = new ArrayList<>();

    private final Set<TransformOptions> optionsHolder = new HashSet<>();

    private User user;

    /**
     * Use to submit all contentlets
     * @param contentlets
     * @return
     */
    public DotTransformerBuilder content(final List<Contentlet> contentlets){
        this.contentlets.addAll(contentlets);
        return this;
    }

    /**
     * Use to submit all contentlets. One single entry can me set conveniently
     * @param contentlets
     * @return
     */
    public DotTransformerBuilder content(final Contentlet... contentlets){
        this.contentlets.addAll(Arrays.asList(contentlets));
        return this;
    }

    /***
     * If a user is required this must be used to set it
     * @param user
     * @return
     */
    public DotTransformerBuilder forUser(final User user) {
        this.user = user;
        return this;
    }

    /**
     * Use to replace the BinaryToMapTransformer
     * @return
     */
    public DotTransformerBuilder binaryToMapTransformer(){
       optionsHolder.clear();
       optionsHolder.add(BINARIES_VIEW);
       return this;
    }

    /**
     * Use to replace the LanguageToMapTransformer
     * @return
     */
    public DotTransformerBuilder languageToMapTransformer(){
        optionsHolder.clear();
        optionsHolder.add(LANGUAGE_VIEW);
        return this;
    }

    /**
     * Use it to replace IdentifierToMapTransformer
     * @return
     */
    DotTransformerBuilder identifierToMapTransformer(){
        optionsHolder.clear();
        optionsHolder.add(IDENTIFIER_VIEW);
        return this;
    }

    /**
     * Use to hydrated Site
     * @param clear boolean true if want to clear the previous options, otherwise false if just want to aggregate to the current config.
     * @return
     */
    public DotTransformerBuilder siteToMapTransformer(final boolean clear){
        if (clear) {
            optionsHolder.clear();
        }
        optionsHolder.addAll(EnumSet.of(SITE_VIEW));
        return this;
    }

    /**
     * Use to replace CategoryToMapTransformer
     * @return
     */
    public DotTransformerBuilder categoryToMapTransformer(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(CATEGORIES_VIEW));
        return this;
    }

    /**
     * KeyValue to Map Transformer
     * @return
     */
    public DotTransformerBuilder keyValueToMapTransformer(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(KEY_VALUE_VIEW));
        return this;
    }

    /**
     * Url Content Map to Map Transformer
     * @return
     */
    public DotTransformerBuilder urlContentMapTransformer(){
        optionsHolder.clear();
        optionsHolder.addAll(DotContentletTransformerImpl.defaultOptions);
        optionsHolder.add(KEY_VALUE_VIEW);
        return this;
    }

    /**
     * Fine tuned to be used for FileAssets on BrowserAPI
     * @return
     */
    public DotTransformerBuilder webAssetOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(
                EnumSet.of(COMMON_PROPS, VERSION_INFO, LOAD_META, USE_ALIAS, LANGUAGE_PROPS, BINARIES));
        return this;
    }

    public DotTransformerBuilder hydratedContentMapTransformer(final TransformOptions...transformOptions){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, CONSTANTS, VERSION_INFO, TAGS));
        optionsHolder.add(KEY_VALUE_VIEW);
        optionsHolder.add(LANGUAGE_VIEW);
        optionsHolder.add(CATEGORIES_VIEW);
        optionsHolder.add(BINARIES_VIEW);
        optionsHolder.add(FILEASSET_VIEW);
        optionsHolder.add(LOAD_META);
        optionsHolder.add(AVOID_MAP_SUFFIX_FOR_VIEWS);
        if(transformOptions.length>0) {
            optionsHolder.addAll(Arrays.asList(transformOptions));
        }

        return this;
    }

    /**
     * Fine tuned to be used for DotAssets on BrowserAPI
     * @return
     */
    public DotTransformerBuilder dotAssetOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, VERSION_INFO, USE_ALIAS, LANGUAGE_PROPS, BINARIES));
        return this;
    }

    /**
     * Fine-Tuned to be consumed from ContentResource
     * @return
     */
    public DotTransformerBuilder contentResourceOptions(final boolean allCategoriesInfo){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, CONSTANTS, VERSION_INFO, LOAD_META, BINARIES, CATEGORIES_NAME,
                DATETIME_FIELDS_TO_TIMESTAMP));
        if(allCategoriesInfo){
          optionsHolder.remove(CATEGORIES_NAME);
          optionsHolder.add(CATEGORIES_INFO);
        }
        return this;
    }

    /**
     * Fine-Tuned to be consumed from ContentResource
     * @return
     */
    public DotTransformerBuilder allOptions(final boolean allCategoriesInfo){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, CONSTANTS, VERSION_INFO, LOAD_META, BINARIES, CATEGORIES_NAME, LANGUAGE_PROPS));
        if(allCategoriesInfo){
            optionsHolder.remove(CATEGORIES_NAME);
            optionsHolder.add(CATEGORIES_INFO);
        }
        return this;
    }


    /**
     * This one does not transform binaries. But still leaves them for use. Meaning the resulting map will still have binaries
     * @return
     */
    public DotTransformerBuilder graphQLDataFetchOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, CONSTANTS, VERSION_INFO, CATEGORIES_NAME));
        return this;
    }

    /**
     * Don't know where? to go land here.
     * @return
     */
    public DotTransformerBuilder defaultOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(DotContentletTransformerImpl.defaultOptions);
        return this;
    }

    /**
     * This transformer provides a view for the History of a Contentlet. It exposes a minified map
     * of properties, just like the data you can see in the History tab in the Content Editor page.
     * As such, all the other Contentlet properties are removed by default.
     *
     * @return The {@link DotTransformerBuilder} instance.
     */
    public DotTransformerBuilder historyToMapTransformer(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(CLEAR_EXISTING_DATA, HISTORY_VIEW));
        return this;
    }

    /**
     * This opens the door to say we want to skip rendering code
     * @param renderFields boolean true if we want to render the fields, otherwise false
     * @return the builder
     */
    public DotTransformerBuilder renderFields(final boolean renderFields){
        if (renderFields) {
            optionsHolder.add(TransformOptions.RENDER_FIELDS);
        } else {
            optionsHolder.remove(TransformOptions.RENDER_FIELDS);
            //By default, Widget Strategy render widget code. If we want to skip it, we need to add this option
            optionsHolder.add(TransformOptions.SKIP_WIDGET_CODE_RENDERING);
        }
        return this;
    }

    /**
     * Once every option has been selected get the instance here
     * @return
     */
    public DotContentletTransformer build() {
        final StrategyResolver resolver;
        final String providerClassName = getStrategyResolverProvider();
        if (isNotSet(providerClassName)) {
            resolver = new StrategyResolverImpl();
        } else {
            try {
                resolver = ((Class<StrategyResolver>) Class.forName(providerClassName))
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                final String message = String
                        .format("Failure instantiating custom StrategyResolver for the given className `%s`",
                                providerClassName);
                Logger.error(DotTransformerBuilder.class, message, e);
                throw new DotRuntimeException(message, e);
            }
        }
        if (null == user) {
            try {
                user = APILocator.getUserAPI().getAnonymousUser();
            } catch (DotDataException e) {
                throw new DotRuntimeException(e);
            }
        }
        return new DotContentletTransformerImpl(contentlets, resolver, EnumSet.copyOf(optionsHolder), user);
    }

    final static Lazy<String> strategyResolver = Lazy.of(() -> {
        return Config.getStringProperty("TRANSFORMER_PROVIDER_STRATEGY_CLASS", null);
    });
    
    /**
     * The strategies resolver is a key part of the mechanism
     * This allows to provide a custom one.
     * @return
     */
    private String getStrategyResolverProvider() {
        return strategyResolver.get();
    }

}
