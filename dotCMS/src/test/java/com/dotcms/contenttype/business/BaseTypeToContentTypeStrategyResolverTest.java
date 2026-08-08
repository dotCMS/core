package com.dotcms.contenttype.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit test for {@link BaseTypeToContentTypeStrategyResolver}.
 *
 * Covers the {@code subscribe(...)} regression (it used to ignore its {@code baseContentType}
 * argument and always register under {@link BaseContentType#DOTASSET}) and the registration of
 * the default {@code FILEASSET} strategy.
 */
public class BaseTypeToContentTypeStrategyResolverTest extends UnitTestBase {

    /**
     * Method to test: {@link BaseTypeToContentTypeStrategyResolver#subscribe(BaseContentType, BaseTypeToContentTypeStrategy)}
     * Given Scenario: A strategy is subscribed under {@link BaseContentType#FILEASSET}.
     * Expected Result: {@code get(FILEASSET)} returns exactly that strategy, and the DOTASSET
     * default strategy is left untouched.
     */
    @Test
    public void test_subscribe_registers_strategy_under_the_passed_base_type() {

        final BaseTypeToContentTypeStrategyResolver resolver = new BaseTypeToContentTypeStrategyResolver();
        final BaseTypeToContentTypeStrategy dotAssetDefault =
                resolver.get(BaseContentType.DOTASSET).orElse(null);
        final BaseTypeToContentTypeStrategy customStrategy =
                (baseContentType, contextMap) -> Optional.empty();

        resolver.subscribe(BaseContentType.FILEASSET, customStrategy);

        final Optional<BaseTypeToContentTypeStrategy> result = resolver.get(BaseContentType.FILEASSET);
        assertTrue("FILEASSET strategy should be registered under FILEASSET", result.isPresent());
        assertSame("subscribe must register under the passed base type", customStrategy, result.get());
        assertSame("DOTASSET strategy must not be overwritten by subscribing FILEASSET",
                dotAssetDefault, resolver.get(BaseContentType.DOTASSET).orElse(null));
    }

    /**
     * Method to test: {@link BaseTypeToContentTypeStrategyResolver#get(BaseContentType)}
     * Given Scenario: A freshly built resolver with its default strategies.
     * Expected Result: Both DOTASSET and FILEASSET resolution strategies are registered by default.
     */
    @Test
    public void test_default_strategies_include_dotasset_and_fileasset() {

        final BaseTypeToContentTypeStrategyResolver resolver = new BaseTypeToContentTypeStrategyResolver();

        assertTrue("DOTASSET strategy must be registered by default",
                resolver.get(BaseContentType.DOTASSET).isPresent());
        assertTrue("FILEASSET strategy must be registered by default",
                resolver.get(BaseContentType.FILEASSET).isPresent());
    }

    /**
     * Method to test: {@link BaseTypeToContentTypeStrategyResolver#get(BaseContentType)}
     * Given Scenario: A base type with no registered strategy.
     * Expected Result: An empty Optional is returned.
     */
    @Test
    public void test_get_returns_empty_for_unregistered_base_type() {

        final BaseTypeToContentTypeStrategyResolver resolver = new BaseTypeToContentTypeStrategyResolver();

        assertFalse(resolver.get(BaseContentType.WIDGET).isPresent());
    }

}
