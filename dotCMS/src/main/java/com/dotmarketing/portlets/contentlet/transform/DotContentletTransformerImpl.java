package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_NAME;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolver;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolbox;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class intents to be the single point of transformation logic
 * Transformation basically takes place in strategies that are plugged based on the options submitted
 * or the ContentType associated to the the Content.
 */
class DotContentletTransformerImpl implements DotContentletTransformer {

    static final Set<TransformOptions> defaultOptions = EnumSet.of(
            COMMON_PROPS, CONSTANTS, VERSION_INFO, BINARIES, CATEGORIES_NAME
    );

    private final User user;
    private final Set<TransformOptions> options;
    private final List<Contentlet> contentlets;
    private final StrategyResolver strategyResolver;

    /**
     * Main constructor provides access to set the required APIs
     * @param contentlets input
     * @param strategyResolver strategyResolver
     * @param options bit to instruct what needs to be turned on and off
     * @param user user
     */
    @VisibleForTesting
    DotContentletTransformerImpl(final List<Contentlet> contentlets,
            final StrategyResolver strategyResolver,
            final Set<TransformOptions> options,
            final User user) {
        if(!isSet(contentlets)){
           throw new DotRuntimeException("At least 1 contentlet must be set.");
        }
        this.contentlets = contentlets;
        this.strategyResolver = strategyResolver;
        this.options = options;
        this.user = user;
    }

    /**
     * If desired we can do bulk transformation over a collection. So That's why we have a toMaps
     * @return List of transformed Maps
     */
    public List<Map<String, Object>> toMaps() {
        return contentlets.stream().map(source -> Tuple.of(source, copy(source))).map(tuple -> {
            final Contentlet source =  tuple._1; //This is the source contentlet from cache. We're not supposed to modify this.
            final Contentlet copyContentlet = tuple._2; //This is the mutable copy we can work on.
            return transform(source, copyContentlet);
        }).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * This is the main method where individual transformation takes place.
     * @param copyContentlet input contentlet
     * @return Map holding the transformed properties
     */
    private Map<String, Object> transform(final Contentlet sourceContentlet, final Contentlet copyContentlet) {
        final ContentType type = sourceContentlet.getContentType();
        if (null != type) {
            Logger.debug(
                    DotContentletTransformerImpl.class, () -> String
                            .format(" BaseType: `%s` Type: `%s`", type.name(),
                                    type.baseType().name()));
        }
        //We'll work directly on the copy. We dont want to mess with anything that already lives on cache.
        final Map<String, Object> map = copyContentlet.getMap();

        strategyResolver.resolveStrategies(type, options).stream()
                .map(strategy -> {
                    strategy.apply(sourceContentlet, map, options, user);
                    return strategy;
                }).reduce((a, b) -> b).ifPresent(lastStrategy -> lastStrategy.cleanup(map));

        return map;
    }

    /**
     * Adds needed things that are not included by default from the api to the contentlet.
     * If there is anything new to add, returns copy with the new attributes inside, otherwise returns the same instance.
     * @return Contentlet returns a contentlet, if there is something to add will create a new instance based on the current one in the parameter and the new attributes
     */
    public List<Contentlet> hydrate() {
        return contentlets.stream().map(source -> Tuple.of(source, copy(source))).map(tuple -> {
            final Contentlet source =  tuple._1; //This is the source contentlet from cache. We're not supposed to modify this.
            final Contentlet copyContentlet = tuple._2; //This is the mutable copy we can work on.
            transform(source, copyContentlet);
            return copyContentlet;
        }).collect(Collectors.toList());
    }

    /**
     * To avoid caching issues we work on a copy
     * @param contentlet input contentlet
     * @return a copy contentlet
     */
    private Contentlet copy(final Contentlet contentlet) {
       return TransformToolbox.copyContentlet(contentlet);
    }

}