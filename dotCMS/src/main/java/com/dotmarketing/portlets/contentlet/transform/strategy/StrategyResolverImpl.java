package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_VIEW;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Default Resolver impl
 */
public class StrategyResolverImpl implements StrategyResolver {

    //Strategies Triggered by CT
    private final Map<BaseContentType, Supplier<AbstractTransformStrategy>> strategyTriggeredByBaseType;

    //Strategies Triggered by an Option
    private final Map<TransformOptions, Supplier<AbstractTransformStrategy>> strategyTriggeredByOption;

    //Fixed strategies that are most like to be applied all the time.
    //Anyways we keep them on a Supplier to instantiate them only if they're really needed.
    private final DefaultTransformStrategy defaultTransformStrategy;
    private final CleanupStrategy cleanUpTransformStrategy;

    StrategyResolverImpl(
            final Map<BaseContentType, Supplier<AbstractTransformStrategy>> strategyTriggeredByBaseType,
            final Map<TransformOptions, Supplier<AbstractTransformStrategy>> strategyTriggeredByOption,
            final DefaultTransformStrategy defaultTransformStrategy,
            final CleanupStrategy cleanUpTransformStrategy) {
        this.strategyTriggeredByBaseType = strategyTriggeredByBaseType;
        this.strategyTriggeredByOption = strategyTriggeredByOption;
        this.defaultTransformStrategy = defaultTransformStrategy;
        this.cleanUpTransformStrategy = cleanUpTransformStrategy;
    }

    /**
     * Main constructor
     */
    @VisibleForTesting
    public StrategyResolverImpl(final TransformToolbox toolBox) {
        this(
            //These are very specific implementations but most cases will be covered by the default strategy.
            ImmutableMap.of(
                BaseContentType.FILEASSET, () -> new FileAssetViewStrategy(toolBox),
                BaseContentType.HTMLPAGE, () -> new PageViewStrategy(toolBox),
                BaseContentType.DOTASSET, () -> new DotAssetViewStrategy(toolBox)
                ),
             ImmutableMap.of(
                 CATEGORIES_VIEW, () -> new CategoryViewStrategy(toolBox)
             ),
            //This are the ones applied at all times
             new DefaultTransformStrategy(toolBox), new CleanupStrategy(toolBox)
        );
    }

    /**
     * Default constructor
     */
    public StrategyResolverImpl() {
        this(new TransformToolbox());
    }

    /**
     * This decides what strategies must be applied to transform the contentlet based of on the CT and the Options
     * @param contentType
     * @param options
     * @return
     */
    @Override
    public List<AbstractTransformStrategy> resolveStrategies(final ContentType contentType,
            final Set<TransformOptions> options) {
        final ImmutableList.Builder<AbstractTransformStrategy> builder = new ImmutableList.Builder<>();
        //The order on which things are applied is important
        builder.add(defaultTransformStrategy);

        if (null != contentType) {
            final Supplier<AbstractTransformStrategy> supplier = strategyTriggeredByBaseType
                    .get(contentType.baseType());
            if (null != supplier) {
                builder.add(supplier.get());
            }
        }

        for(final TransformOptions option:options){
            final Supplier<AbstractTransformStrategy> supplier = strategyTriggeredByOption.get(option);
            if(null != supplier){
                builder.add(supplier.get());
            }
        }

        builder.add(cleanUpTransformStrategy);

        return builder.build();
    }

}
