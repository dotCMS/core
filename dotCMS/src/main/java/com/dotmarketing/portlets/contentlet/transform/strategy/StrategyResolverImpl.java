package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.api.APIProvider.Builder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CATEGORIES_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.DATETIME_FIELDS_TO_TIMESTAMP;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.FILEASSET_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.HISTORY_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.IDENTIFIER_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.JSON_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.KEY_VALUE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LANGUAGE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.RENDER_FIELDS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.SITE_VIEW;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.STORY_BLOCK_VIEW;
import static com.google.common.collect.ImmutableMap.of;

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
    public StrategyResolverImpl(final APIProvider toolBox) {
        this(
            //These are very specific implementations but most cases will be covered by the default strategy.
            of(
                BaseContentType.FILEASSET, () -> new FileAssetViewStrategy(toolBox),
                BaseContentType.HTMLPAGE, () -> new PageViewStrategy(toolBox),
                BaseContentType.DOTASSET, () -> new DotAssetViewStrategy(toolBox),
                BaseContentType.WIDGET, () -> new WidgetViewStrategy(toolBox)
                ),
                getStrategyTriggeredByOptionMap(toolBox),
             ()-> new DefaultTransformStrategy(toolBox)
        );
    }

    /**
     * Loads a {@link Map} of {@link TransformOptions} to
     * {@link Supplier<AbstractTransformStrategy>} which associates a specific Transformation
     * Strategy to a specific Transform Options. This way, the Strategies listed in the method can
     * be exposed and used for returning a JSON representation of a Contentlet in a specific way.
     *
     * @param toolBox The {@link APIProvider} object used to access dotCMS APIs.
     *
     * @return A {@link Map} of {@link TransformOptions} to
     * {@link Supplier<AbstractTransformStrategy>}.
     */
    private static Map<TransformOptions, Supplier<AbstractTransformStrategy>> getStrategyTriggeredByOptionMap(final APIProvider toolBox) {

        final Map<TransformOptions, Supplier<AbstractTransformStrategy>> strategyTriggeredByOptionMap = new HashMap<>();

        strategyTriggeredByOptionMap.put(CATEGORIES_VIEW, () -> new CategoryViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(BINARIES_VIEW, () -> new BinaryViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(IDENTIFIER_VIEW, () -> new IdentifierViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(LANGUAGE_VIEW, () -> new LanguageViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(KEY_VALUE_VIEW, () -> new KeyValueViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(FILEASSET_VIEW, () -> new FileViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(SITE_VIEW, () -> new SiteViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(STORY_BLOCK_VIEW, () -> new StoryBlockViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(RENDER_FIELDS, () -> new RenderFieldStrategy(toolBox));
        strategyTriggeredByOptionMap.put(JSON_VIEW, () -> new JSONViewStrategy(toolBox));
        strategyTriggeredByOptionMap.put(DATETIME_FIELDS_TO_TIMESTAMP, () -> new DateTimeFieldsToTimeStampStrategy(toolBox));
        strategyTriggeredByOptionMap.put(HISTORY_VIEW, () -> new HistoryViewStrategy(toolBox));
        return strategyTriggeredByOptionMap;
    }

    /**
     * Default constructor
     */
    public StrategyResolverImpl() {
        this(new Builder().build());
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

        if(options.stream().anyMatch(TransformOptions::isDefaultProperty)){
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
