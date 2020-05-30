package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_CONSTANTS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.INC_VERSION_INFO;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.AbstractTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolver;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolbox;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
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

class DotTransformerImpl implements DotTransformer {

    static final Set<TransformOptions> defaultOptions = EnumSet.of(
        INC_COMMON_PROPS, INC_CONSTANTS, INC_VERSION_INFO, INC_BINARIES
    );

    private User user;
    private final Set<TransformOptions> includedOptions;
    private final List<Contentlet> contentlets;
    private final StrategyResolver strategyResolver;

    /**
     * Main constructor provides access to set the required APIs
     * @param contentlets input
     * @param strategyResolver strategyResolver
     * @param includeOptions
     * @param user
     */
    @VisibleForTesting
    DotTransformerImpl(final List<Contentlet> contentlets,
            final StrategyResolver strategyResolver,
            final Set<TransformOptions> includeOptions,
            final User user) {
        if(!isSet(contentlets)){
           throw new DotRuntimeException("At least 1 contentlet must be set.");
        }
        this.contentlets = contentlets;
        this.strategyResolver = strategyResolver;
        this.includedOptions = includeOptions;
        this.user = user;
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
            Logger.debug(
                    DotTransformerImpl.class,()->String.format(" BaseType: `%s` Type: `%s`", type.name(), type.baseType().name()));
        }
        final Map<String, Object> map = contentlet.getMap();
        final List<AbstractTransformStrategy> strategies = strategyResolver.resolveStrategies(type, includedOptions);
        for(final AbstractTransformStrategy strategy:strategies){
           strategy.apply(contentlet, map, includedOptions, user);
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
       return TransformToolbox.copyContentlet(contentlet);
    }

}