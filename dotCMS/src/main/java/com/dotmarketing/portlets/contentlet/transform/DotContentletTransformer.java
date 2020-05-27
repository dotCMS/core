package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.*;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.AbstractTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolver;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolBox;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DotContentletTransformer implements DotTransformer {

    static final Set<TransformOptions> defaultOptions = EnumSet.of(
        INC_COMMON_PROPS, INC_CONSTANTS, INC_VERSION_INFO, INC_BINARIES, LOAD_META
    );

    private final Set<TransformOptions> includedOptions;
    private final List<Contentlet> contentlets;
    private final StrategyResolver strategyResolver;

    /**
     * Bulk transform constructor
     * @param contentlets input
     */
    public DotContentletTransformer(final List<Contentlet> contentlets) {
        this(contentlets, new StrategyResolver(), defaultOptions);
    }

    /**
     * Convenience constructor
     * @param contentlets input
     */
    public DotContentletTransformer(final Contentlet... contentlets) {
        this(Arrays.asList(contentlets), new StrategyResolver(), defaultOptions);
    }

    /**
     *
     * @param options
     * @param contentlets
     */
    private DotContentletTransformer(final Set<TransformOptions> options, final List<Contentlet> contentlets) {
        this(contentlets, new StrategyResolver(), options);
    }

    /**
     * Main constructor provides access to set the required APIs
     * @param contentlets input
     * @param strategyResolver strategyResolver
     * @param includeOptions
     */
    @VisibleForTesting
    DotContentletTransformer(final List<Contentlet> contentlets,
            final StrategyResolver strategyResolver,
            final Set<TransformOptions> includeOptions) {
        if(!isSet(contentlets)){
           throw new DotRuntimeException("At least 1 contentlet must be set.");
        }
        this.contentlets = contentlets;
        this.strategyResolver = strategyResolver;
        this.includedOptions = includeOptions;
    }

    /**
     * If desired we can do bulk transformation over a collection. So That's why we have a toMaps
     * @return List of transformed Maps
     */
    public List<Map<String, Object>> toMaps() {
        return contentlets.stream().map(this::copy).map(this::transform).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * This is the main method where individual transformation takes place.
     * @param contentlet input contentlet
     * @return Map holding the transformed properties
     */
    private Map<String, Object> transform(final Contentlet contentlet) {
        final ContentType type = contentlet.getContentType();
        if(null != type) {
            Logger.debug(DotContentletTransformer.class,()->String.format(" BaseType: `%s` Type: `%s`", type.name(), type.baseType().name()));
        }
        final Map<String, Object> map = contentlet.getMap();
        final List<AbstractTransformStrategy> strategies = strategyResolver.resolveStrategies(type);
        for(final AbstractTransformStrategy strategy:strategies){
           strategy.apply(contentlet, map, includedOptions);
        }

        return map;
    }

    /**
     * Adds needed things that are not included by default from the api to the contentlet.
     * If there is anything new to add, returns copy with the new attributes inside, otherwise returns the same instance.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes
     */
    public List<Contentlet> hydrate() {
        return contentlets.stream().map(this::copy).map(newContentlet -> {
            transform(newContentlet);
            return newContentlet;
        }).collect(Collectors.toList());
    }

    /**
     * To avoid caching issues we work on a copy
     * @param contentlet input contentlet
     * @return a copy contentlet
     */
    private Contentlet copy(final Contentlet contentlet) {
       return TransformToolBox.copyContentlet(contentlet);
    }

    public static class Builder {

         private List<Contentlet> contentlets = new ArrayList<>();

         private Set<TransformOptions> optionsHolder = new HashSet<>(defaultOptions);

        public Builder content(final List<Contentlet> contentlets){
            this.contentlets.addAll(contentlets);
            return this;
        }

        public Builder content(final Contentlet... contentlets){
            this.contentlets.addAll(Arrays.asList(contentlets));
            return this;
        }

        Builder binaryToMapTransformer(){
           optionsHolder.clear();
           optionsHolder.add(BINARIES_AS_MAP);
           return this;
        }

        Builder languageToMapTransformer(){
            optionsHolder.clear();
            optionsHolder.add(LANGUAGE_AS_MAP);
            return this;
        }

        Builder identifierToMapTransformer(){
            optionsHolder.clear();
            optionsHolder.add(IDENTIFIER_AS_MAP);
            return this;
        }

        public Builder webAssetOptions(){
            optionsHolder.clear();
            optionsHolder.addAll(EnumSet.of(INC_COMMON_PROPS, INC_VERSION_INFO, LOAD_META, USE_ALIAS, LANGUAGE_PROPS));
            return this;
        }

        public Builder dotAssetOptions(){
            optionsHolder.clear();
            optionsHolder.addAll(EnumSet.of(INC_COMMON_PROPS, INC_VERSION_INFO, USE_ALIAS, LANGUAGE_PROPS));
            return this;
        }

        public DotTransformer build(){
           return new DotContentletTransformer(EnumSet.copyOf(optionsHolder) ,contentlets);
        }

    }

}