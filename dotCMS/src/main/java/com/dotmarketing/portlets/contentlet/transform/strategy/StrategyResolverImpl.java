package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.IDENTIFIER_VIEW;
import static com.google.common.collect.ImmutableMap.of;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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
    private final Supplier<DefaultTransformStrategy> defaultTransformStrategy;

    /**
     * Test friendly constructor version
     * @param strategyTriggeredByBaseType
     * @param strategyTriggeredByOption
     * @param defaultTransformStrategy
     */
    @VisibleForTesting
    public StrategyResolverImpl(
            final Map<BaseContentType, Supplier<AbstractTransformStrategy>> strategyTriggeredByBaseType,
            final Map<TransformOptions, Supplier<AbstractTransformStrategy>> strategyTriggeredByOption,
            final Supplier<DefaultTransformStrategy> defaultTransformStrategy) {
        this.strategyTriggeredByBaseType = strategyTriggeredByBaseType;
        this.strategyTriggeredByOption = strategyTriggeredByOption;
        this.defaultTransformStrategy = defaultTransformStrategy;
    }

    /**
     * Main constructor
     */
    public StrategyResolverImpl(final TransformToolbox toolBox) {
        this(
            //These are very specific implementations but most cases will be covered by the default strategy.
            of(
                BaseContentType.FILEASSET, () -> new FileAssetViewStrategy(toolBox),
                BaseContentType.HTMLPAGE, () -> new PageViewStrategy(toolBox),
                BaseContentType.DOTASSET, () -> new DotAssetViewStrategy(toolBox)
                ),
             of(
                 CATEGORIES_VIEW, () -> new CategoryViewStrategy(toolBox),
                 BINARIES_VIEW, () -> new BinaryViewStrategy(toolBox),
                 IDENTIFIER_VIEW, () -> new IdentifierViewStrategy(toolBox)
             ),
             ()-> new DefaultTransformStrategy(toolBox)
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

        if(options.stream().anyMatch(TransformOptions::isProperty)){
           builder.add(defaultTransformStrategy.get());
        }

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

        return builder.build();
    }

}
