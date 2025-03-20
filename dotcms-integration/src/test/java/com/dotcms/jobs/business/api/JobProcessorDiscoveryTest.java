package com.dotcms.jobs.business.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for verifying the CDI-based job processor discovery functionality in
 * JobProcessorDiscovery.
 * <p>
 * This class tests the automatic discovery of JobProcessor implementations through CDI, ensuring:
 * <ul>
 *   <li>Only JobProcessor implementations with @Dependent scope are discovered</li>
 *   <li>Invalid scope processors (non-@Dependent) are filtered out</li>
 *   <li>Non-JobProcessor classes are ignored</li>
 *   <li>Empty results are handled correctly</li>
 * </ul>
 */
public class JobProcessorDiscoveryTest {

    private BeanManager beanManager;
    private JobProcessorDiscovery discovery;

    @BeforeEach
    void setUp() {
        beanManager = mock(BeanManager.class);
        discovery = new JobProcessorDiscovery(beanManager);
    }

    /**
     * Method to test: discoverJobProcessors
     * Given Scenario: Multiple classes are available, including valid JobProcessors,
     * invalid scope processors, and non-processor classes
     * ExpectedResult: Only valid JobProcessor implementations with @Dependent scope are discovered
     */
    @Test
    void test_discover_valid_job_processors() {

        // Create mock beans
        Bean<?> validBean1 = createMockBean(ValidJobProcessor1.class, Dependent.class);
        Bean<?> validBean2 = createMockBean(ValidJobProcessor2.class, Dependent.class);
        Bean<?> invalidScopeBean = createMockBean(
                InvalidScopeProcessor.class, ApplicationScoped.class);
        Bean<?> nonProcessorBean = createMockBean(NonProcessor.class, Dependent.class);

        // Set up bean manager to return our mock beans
        Set<Bean<?>> beans = new HashSet<>();
        beans.add(validBean1);
        beans.add(validBean2);
        beans.add(invalidScopeBean);
        beans.add(nonProcessorBean);
        when(beanManager.getBeans(JobProcessor.class, Any.Literal.INSTANCE)).thenReturn(beans);

        // Test discovery
        List<Class<? extends JobProcessor>> discovered = discovery.discoverJobProcessors();

        // Verify results
        assertEquals(2, discovered.size(),
                "Should discover only valid JobProcessor implementations");
        assertTrue(discovered.contains(ValidJobProcessor1.class),
                "Should discover ValidJobProcessor1");
        assertTrue(discovered.contains(ValidJobProcessor2.class),
                "Should discover ValidJobProcessor2");
    }

    /**
     * Method to test: discoverJobProcessors
     * Given Scenario: No JobProcessor implementations are available
     * ExpectedResult: An empty list is returned
     */
    @Test
    void test_discover_no_processors() {

        // Set up bean manager to return empty set
        when(beanManager.getBeans(JobProcessor.class, Any.Literal.INSTANCE))
                .thenReturn(new HashSet<>());

        // Test discovery
        List<Class<? extends JobProcessor>> discovered = discovery.discoverJobProcessors();

        // Verify results
        assertTrue(discovered.isEmpty(), "Should return empty list when no processors found");
    }

    /**
     * Helper method to create mock beans for testing.
     *
     * @param beanClass The class of the bean to mock
     * @param scope     The scope annotation class to apply to the bean
     * @return A mock Bean instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Bean<?> createMockBean(Class<?> beanClass, Class<?> scope) {
        Bean bean = mock(Bean.class);
        when(bean.getBeanClass()).thenReturn(beanClass);
        when(bean.getScope()).thenReturn(scope);
        return bean;
    }

    /**
     * Valid test JobProcessor implementation with correct @Dependent scope.
     */
    @Dependent
    static class ValidJobProcessor1 implements JobProcessor {

        @Override
        public void process(Job job) {
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return null;
        }
    }

    /**
     * Second valid test JobProcessor implementation with correct @Dependent scope.
     */
    @Dependent
    static class ValidJobProcessor2 implements JobProcessor {

        @Override
        public void process(Job job) {
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return null;
        }
    }

    /**
     * Invalid test JobProcessor implementation with incorrect @ApplicationScoped scope.
     */
    @ApplicationScoped
    static class InvalidScopeProcessor implements JobProcessor {

        @Override
        public void process(Job job) {
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return null;
        }
    }

    /**
     * Test class that doesn't implement JobProcessor interface.
     */
    @Dependent
    static class NonProcessor {
        // Not a JobProcessor implementation
    }
}