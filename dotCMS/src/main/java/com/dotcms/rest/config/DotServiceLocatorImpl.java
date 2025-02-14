package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.Utilities;

/**
 * This class provides a workaround for a known issue in Jersey where a service locator,
 * once marked as inactive, throws an exception that renders Jersey unusable.
 *
 * <p>When this exception is thrown directly from the service locator during dependency
 * injection, Jersey enters a non-functional state. The solution implemented here involves
 * overriding the service locator using JavaServiceProviderLocator. By leveraging the
 * class loader, we replace the problematic service instances with safe-to-dispose null
 * classes.</p>
 *
 * <p>The replacement is configured in the <code>META-INF/services</code> folder via Service Provider Interface (SPI),
 * where the necessary class overrides are specified. This ensures that when an exception
 * is encountered during disposal, it is intercepted, and a safe null instance is returned,
 * preventing additional failures.</p>
 *
 * <p>It is important to note that the root cause of this issue is a known Github issue,
 * and this workaround should be removed once the migration to Tomcat 10 is complete.</p>
 * See <a href="https://github.com/dotCMS/core/issues/31185">31185</a> for more information.
 */
public class DotServiceLocatorImpl extends ServiceLocatorImpl {

    private static final String PATTERN = "DotServiceLocatorImpl\\(__HK2_Generated_\\d+,\\d+,\\d+\\) has been shut down";
    private static final Pattern REGEX = Pattern.compile(PATTERN);

    /**
     * To Narrow down the exception to the one we are looking for
     * @param e The exception to check
     * @return True if the exception is the one we are looking for
     */
    public static boolean isServiceShutDownException(IllegalStateException e) {
        String exceptionMessage = e.getMessage();
        Matcher matcher = REGEX.matcher(exceptionMessage);
        return matcher.matches();
    }

    private final String name;
    /**
     * Called by the Generator, and hence must be a public method
     *
     * @param name   The name of this locator
     * @param parent The parent of this locator (maybe null)
     */
    public DotServiceLocatorImpl(String name, ServiceLocatorImpl parent) {
        super(name, parent);
        this.name = name;
    }

    /**
     * This Method is overridden to ignore IllegalStateException during reload
     * Injects the given object using the given strategy
     * @param injectMe The object to be analyzed and injected into
     * @param strategy The name of the {@link ClassAnalyzer} that should be used. If
     */
    @Override
    public void inject(Object injectMe, String strategy) {
        //there's a bug in jersey that causes a IllegalStateException to be thrown when the container is reloading
        //This Bug Kills the container leaving it useless
        //And the reason if the checkState Method in the super Class (which is private)
        Utilities.justInject(injectMe, this, strategy);
    }

    /**
     * This Method is overridden to ignore IllegalStateException during reload
     * @param activeDescriptor The descriptor for which to create a {@link ServiceHandle}.
     * May not be null
     * @return
     * @param <T>
     * @throws MultiException
     */
    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException{
        //Check state is also called here during reload. During disposal of the service
        try {
            return super.getServiceHandle(activeDescriptor);
        } catch (IllegalStateException e) {
            if(isServiceShutDownException(e)) {
                Logger.warn(this,
                        String.format("The Following Exception: \"%s\" was caught and ignored. %n This Exception is expected during a reload! ",
                                e.getMessage())
                );
            }
            //The exception still needs to be thrown so the container can restore itself
            throw e;
        }
    }

    /**
     * Super useful for debugging
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return "DotServiceLocatorImpl(" + name + "," + super.getLocatorId() + "," + System.identityHashCode(this) + ")";
    }

}
