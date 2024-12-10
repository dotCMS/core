package com.dotcms;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Annotate your JUnit4 test class with {@code @RunWith(JUnit4WeldRunner.class)} to run it with Weld container.
 */
public class JUnit4WeldRunner extends BlockJUnit4ClassRunner {

    private static final Weld WELD;
    private static final WeldContainer CONTAINER;

    static {
        WELD = new Weld("JUnit4WeldRunner");
        CONTAINER = WELD.initialize();
    }

    /**
     * Creates a DataProviderRunner to run supplied {@code clazz}.
     *
     * @param clazz the test {@link Class} to run
     * @throws InitializationError if the test {@link Class} is malformed.
     */
    public JUnit4WeldRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    /**
     * Create the test instance using Weld container.
     * @return the test instance
     * @throws Exception if something goes wrong
     */
    @Override
    protected Object createTest() throws Exception {
        return CONTAINER.select(getTestClass().getJavaClass()).get();
    }
}
