package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class StrategyResolver {

   private final Map<BaseContentType, AbstractTransformStrategy> strategyByBaseType;
   private final DefaultTransformStrategy defaultTransformStrategy;
   private final PrivatePropertyRemoveStrategy cleanUpTransformStrategy;

   @VisibleForTesting
   public StrategyResolver(final TransformToolBox toolBox) {
      strategyByBaseType = ImmutableMap.of(
              //These very specific implementations but most cases will be covered by the default strategy.
              BaseContentType.FILEASSET, new FileAssetTransformStrategy(toolBox),
              BaseContentType.HTMLPAGE, new HtmlPageTransformStrategy(toolBox),
              BaseContentType.DOTASSET, new DotAssetTransformStrategy(toolBox)
      );
      defaultTransformStrategy = new DefaultTransformStrategy(toolBox);
      cleanUpTransformStrategy = new PrivatePropertyRemoveStrategy(toolBox);
   }

   public StrategyResolver() {
     this(new TransformToolBox());
   }

   public List<AbstractTransformStrategy> resolveStrategies(final ContentType contentType) {
      final ImmutableList.Builder<AbstractTransformStrategy> builder = new ImmutableList.Builder<>();
      builder.add(defaultTransformStrategy);
      if (null != contentType) {
         final AbstractTransformStrategy strategy = strategyByBaseType.get(contentType.baseType());
         if (null != strategy) {
             builder.add(strategy);
         }
      }
      builder.add(cleanUpTransformStrategy);
      return builder.build();
   }

}
