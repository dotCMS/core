package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_NAME;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.IDENTIFIER_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.KEY_VALUE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.util.UtilMethods.isNotSet;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    DotTransformerBuilder binaryToMapTransformer(){
       optionsHolder.clear();
       optionsHolder.add(BINARIES_VIEW);
       return this;
    }

    /**
     * Use to replace the LanguageToMapTransformer
     * @return
     */
    DotTransformerBuilder languageToMapTransformer(){
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
     * Fine tuned to be used for FileAssets on BrowserAPI
     * @return
     */
    public DotTransformerBuilder webAssetOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(
                EnumSet.of(COMMON_PROPS, VERSION_INFO, LOAD_META, USE_ALIAS, LANGUAGE_PROPS));
        return this;
    }

    /**
     * Fine tuned to be used for DotAssets on BrowserAPI
     * @return
     */
    public DotTransformerBuilder dotAssetOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, VERSION_INFO, USE_ALIAS, LANGUAGE_PROPS));
        return this;
    }

    /**
     * Fine Tuned to be consumed from ContentResource
     * @return
     */
    public DotTransformerBuilder contentResourceOptions(final boolean allCategoriesInfo){
        optionsHolder.clear();
        optionsHolder.addAll(EnumSet.of(COMMON_PROPS, CONSTANTS, VERSION_INFO, LOAD_META, BINARIES, CATEGORIES_NAME));
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
     * Dont know where? to go land here.
     * @return
     */
    public DotTransformerBuilder defaultOptions(){
        optionsHolder.clear();
        optionsHolder.addAll(DotContentletTransformerImpl.defaultOptions);
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

    /**
     * The strategies resolver is a key part of the mechanism
     * This allows to provide a custom one.
     * @return
     */
    private String getStrategyResolverProvider() {
        return Config
                .getStringProperty("TRANSFORMER_PROVIDER_STRATEGY_CLASS", null);
    }

}
