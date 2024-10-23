package com.dotcms;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runners.model.InitializationError;

public class DataProviderWeldRunner extends DataProviderRunner {

    private final Weld weld;
    private final WeldContainer container;

    /**
     * Creates a DataProviderRunner to run supplied {@code clazz}.
     *
     * @param clazz the test {@link Class} to run
     * @throws InitializationError if the test {@link Class} is malformed.
     */
    public DataProviderWeldRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        this.weld = new Weld();
        this.container = weld.initialize();

    }

    /**
     * Create the test instance using Weld container.
     * @return the test instance
     * @throws Exception if something goes wrong
     */
    @Override
    protected Object createTest() throws Exception {
        return container.instance().select(getTestClass().getJavaClass()).get();
    }

}
