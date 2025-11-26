/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.BoostingQueryBuilder;
import org.elasticsearch.index.query.CommonTermsQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.FieldMaskingSpanQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchNoneQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SpanContainingQueryBuilder;
import org.elasticsearch.index.query.SpanFirstQueryBuilder;
import org.elasticsearch.index.query.SpanMultiTermQueryBuilder;
import org.elasticsearch.index.query.SpanNearQueryBuilder;
import org.elasticsearch.index.query.SpanNotQueryBuilder;
import org.elasticsearch.index.query.SpanOrQueryBuilder;
import org.elasticsearch.index.query.SpanTermQueryBuilder;
import org.elasticsearch.index.query.SpanWithinQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.TermsSetQueryBuilder;
import org.elasticsearch.index.query.TypeQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.index.query.functionscore.ExponentialDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.LinearDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.plugins.SearchPlugin.AggregationSpec;
import org.elasticsearch.plugins.SearchPlugin.QuerySpec;
import org.elasticsearch.plugins.SearchPlugin.ScoreFunctionSpec;
import org.elasticsearch.plugins.SearchPlugin.SuggesterSpec;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.aggregations.BaseAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;

/**
 * Utility class used to build an ES Source based on a JSON query
 * @author nollymar
 */
public class SearchSourceBuilderUtil {

    private static List<QuerySpec<?>> querySpecs = new ArrayList<>();
    private static List<ScoreFunctionSpec<?>> scoreSpecs = new ArrayList<>();
    private static List<AggregationSpec> aggregationSpecs = new ArrayList<>();
    private static List<SuggesterSpec> suggestionSpecs = new ArrayList<>();

    private static List<NamedXContentRegistry.Entry> namedXContents = new ArrayList<>();

    static{
        querySpecs.add(new QuerySpec<>(MatchQueryBuilder.NAME, MatchQueryBuilder::new,
                MatchQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(MatchPhraseQueryBuilder.NAME,
                MatchPhraseQueryBuilder::new, MatchPhraseQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(MatchPhrasePrefixQueryBuilder.NAME,
                MatchPhrasePrefixQueryBuilder::new,
                MatchPhrasePrefixQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(MultiMatchQueryBuilder.NAME,
                MultiMatchQueryBuilder::new, MultiMatchQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(NestedQueryBuilder.NAME, NestedQueryBuilder::new,
                NestedQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(DisMaxQueryBuilder.NAME, DisMaxQueryBuilder::new,
                DisMaxQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(IdsQueryBuilder.NAME, IdsQueryBuilder::new,
                IdsQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(MatchAllQueryBuilder.NAME, MatchAllQueryBuilder::new,
                MatchAllQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(QueryStringQueryBuilder.NAME,
                QueryStringQueryBuilder::new, QueryStringQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(BoostingQueryBuilder.NAME, BoostingQueryBuilder::new,
                BoostingQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(BoolQueryBuilder.NAME, BoolQueryBuilder::new,
                BoolQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(TermQueryBuilder.NAME, TermQueryBuilder::new,
                TermQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(TermsQueryBuilder.NAME, TermsQueryBuilder::new,
                TermsQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(FuzzyQueryBuilder.NAME, FuzzyQueryBuilder::new,
                FuzzyQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(RegexpQueryBuilder.NAME, RegexpQueryBuilder::new,
                RegexpQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(RangeQueryBuilder.NAME, RangeQueryBuilder::new,
                RangeQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(PrefixQueryBuilder.NAME, PrefixQueryBuilder::new,
                PrefixQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(WildcardQueryBuilder.NAME, WildcardQueryBuilder::new,
                WildcardQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(ConstantScoreQueryBuilder.NAME,
                ConstantScoreQueryBuilder::new, ConstantScoreQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanTermQueryBuilder.NAME, SpanTermQueryBuilder::new,
                SpanTermQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanNotQueryBuilder.NAME, SpanNotQueryBuilder::new,
                SpanNotQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanWithinQueryBuilder.NAME,
                SpanWithinQueryBuilder::new, SpanWithinQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanContainingQueryBuilder.NAME,
                SpanContainingQueryBuilder::new,
                SpanContainingQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(FieldMaskingSpanQueryBuilder.NAME,
                FieldMaskingSpanQueryBuilder::new,
                FieldMaskingSpanQueryBuilder::fromXContent));
        querySpecs
                .add(new QuerySpec<>(SpanFirstQueryBuilder.NAME, SpanFirstQueryBuilder::new,
                        SpanFirstQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanNearQueryBuilder.NAME, SpanNearQueryBuilder::new,
                SpanNearQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanOrQueryBuilder.NAME, SpanOrQueryBuilder::new,
                SpanOrQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(MoreLikeThisQueryBuilder.NAME,
                MoreLikeThisQueryBuilder::new,
                MoreLikeThisQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(WrapperQueryBuilder.NAME, WrapperQueryBuilder::new,
                WrapperQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(CommonTermsQueryBuilder.NAME,
                CommonTermsQueryBuilder::new, CommonTermsQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SpanMultiTermQueryBuilder.NAME,
                SpanMultiTermQueryBuilder::new, SpanMultiTermQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(FunctionScoreQueryBuilder.NAME,
                FunctionScoreQueryBuilder::new,
                FunctionScoreQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(SimpleQueryStringBuilder.NAME,
                SimpleQueryStringBuilder::new, SimpleQueryStringBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(TypeQueryBuilder.NAME, TypeQueryBuilder::new,
                TypeQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(ScriptQueryBuilder.NAME, ScriptQueryBuilder::new,
                ScriptQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(GeoDistanceQueryBuilder.NAME,
                GeoDistanceQueryBuilder::new, GeoDistanceQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(GeoBoundingBoxQueryBuilder.NAME,
                GeoBoundingBoxQueryBuilder::new,
                GeoBoundingBoxQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(GeoPolygonQueryBuilder.NAME,
                GeoPolygonQueryBuilder::new, GeoPolygonQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(ExistsQueryBuilder.NAME, ExistsQueryBuilder::new,
                ExistsQueryBuilder::fromXContent));
        querySpecs
                .add(new QuerySpec<>(MatchNoneQueryBuilder.NAME, MatchNoneQueryBuilder::new,
                        MatchNoneQueryBuilder::fromXContent));
        querySpecs.add(new QuerySpec<>(TermsSetQueryBuilder.NAME, TermsSetQueryBuilder::new,
                TermsSetQueryBuilder::fromXContent));

        aggregationSpecs.add(new AggregationSpec(TermsAggregationBuilder.NAME,
                TermsAggregationBuilder::new, TermsAggregationBuilder.PARSER));
        aggregationSpecs.add(new AggregationSpec(TopHitsAggregationBuilder.NAME,
                TopHitsAggregationBuilder::new, TopHitsAggregationBuilder::parse));
        aggregationSpecs.add(new AggregationSpec(RangeAggregationBuilder.NAME,
                RangeAggregationBuilder::new, RangeAggregationBuilder.PARSER));
        aggregationSpecs.add(new AggregationSpec(HistogramAggregationBuilder.NAME,
                HistogramAggregationBuilder::new, HistogramAggregationBuilder.PARSER));
        aggregationSpecs.add(new AggregationSpec(DateHistogramAggregationBuilder.NAME,
                DateHistogramAggregationBuilder::new, DateHistogramAggregationBuilder.PARSER));
        aggregationSpecs.add(new AggregationSpec(FilterAggregationBuilder.NAME,
                FilterAggregationBuilder::new, FilterAggregationBuilder::parse));
        aggregationSpecs.add(new AggregationSpec(FiltersAggregationBuilder.NAME,
                FiltersAggregationBuilder::new, FiltersAggregationBuilder::parse));

        suggestionSpecs.add(new SuggesterSpec<>(TermSuggestionBuilder.SUGGESTION_NAME, TermSuggestionBuilder::new,
                TermSuggestionBuilder::fromXContent, TermSuggestion::new));
        suggestionSpecs.add(new SuggesterSpec<>(CompletionSuggestionBuilder.SUGGESTION_NAME,
                CompletionSuggestionBuilder::new, CompletionSuggestionBuilder::fromXContent, CompletionSuggestion::new));
        suggestionSpecs.add(new SuggesterSpec<>(PhraseSuggestionBuilder.SUGGESTION_NAME, PhraseSuggestionBuilder::new,
                PhraseSuggestionBuilder::fromXContent, PhraseSuggestion::new));

        scoreSpecs.add(new ScoreFunctionSpec<>(ScriptScoreFunctionBuilder.NAME, ScriptScoreFunctionBuilder::new,
                ScriptScoreFunctionBuilder::fromXContent));

        scoreSpecs.add(new ScoreFunctionSpec<>(GaussDecayFunctionBuilder.NAME, GaussDecayFunctionBuilder::new, GaussDecayFunctionBuilder.PARSER));

        scoreSpecs.add(new ScoreFunctionSpec<>(LinearDecayFunctionBuilder.NAME, LinearDecayFunctionBuilder::new,
                LinearDecayFunctionBuilder.PARSER));

        scoreSpecs.add(new ScoreFunctionSpec<>(ExponentialDecayFunctionBuilder.NAME, ExponentialDecayFunctionBuilder::new,
                ExponentialDecayFunctionBuilder.PARSER));

        scoreSpecs.add(new ScoreFunctionSpec<>(RandomScoreFunctionBuilder.NAME, RandomScoreFunctionBuilder::new,
                RandomScoreFunctionBuilder::fromXContent));

        scoreSpecs.add(new ScoreFunctionSpec<>(FieldValueFactorFunctionBuilder.NAME, FieldValueFactorFunctionBuilder::new,
                FieldValueFactorFunctionBuilder::fromXContent));



        querySpecs.forEach(spec -> registerQuery(spec));
        aggregationSpecs.forEach(spec -> registerAggregation(spec));
        suggestionSpecs.forEach(spec -> registerSuggestions(spec));
        scoreSpecs.forEach(spec -> registerScoreFunction(spec));

    }

    public static SearchSourceBuilder getSearchSourceBuilder(String query) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SearchModule searchModule = new SearchModule(Settings.EMPTY, false,
                Collections.emptyList());
        try (XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                .createParser(new NamedXContentRegistry(searchModule
                                .getNamedXContents()), DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                        query)) {
            searchSourceBuilder.parseXContent(parser);
        }

        return searchSourceBuilder;
    }

    private static void registerQuery(QuerySpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(QueryBuilder.class, spec.getName(),
                (p, c) -> spec.getParser().fromXContent(p)));
    }

    private static void registerAggregation(AggregationSpec spec) {
        namedXContents
                .add(new NamedXContentRegistry.Entry(BaseAggregationBuilder.class, spec.getName(),
                        (p, c) -> spec.getParser().parse(p,(String)c)));
    }

    private static void registerSuggestions(SuggesterSpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(SuggestionBuilder.class,
                new ParseField(spec.getName().getPreferredName()),
                (parser, context) -> spec.getParser().apply(parser)));
    }

    private static void registerScoreFunction(ScoreFunctionSpec<?> scoreFunction) {
        namedXContents.add(new NamedXContentRegistry.Entry(
                ScoreFunctionBuilder.class, scoreFunction.getName(),
                (XContentParser p, Object c) -> scoreFunction.getParser().fromXContent(p)));
    }


}
