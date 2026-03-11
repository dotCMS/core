package com.dotcms.business.cdi;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.DotInvalidLicenseException;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.EnterpriseFeature;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link EnterpriseFeature}. Validates that the current dotCMS instance
 * has the required Enterprise License level before allowing method execution.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
 */
@Interceptor
@EnterpriseFeature
@Priority(Interceptor.Priority.APPLICATION - 1)
public class EnterpriseFeatureInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(EnterpriseFeature.class)) {
            return context.proceed();
        }

        try {
            final Method method = context.getMethod();
            final EnterpriseFeature annotation = method.getAnnotation(EnterpriseFeature.class);
            if (annotation != null) {
                final LicenseLevel requiredLevel = annotation.licenseLevel();
                final String errorMsg = annotation.errorMsg();
                final int currentLicenseLevel = LicenseUtil.getLevel();
                if (currentLicenseLevel < requiredLevel.level) {
                    throw new DotInvalidLicenseException(errorMsg);
                }
            }
            return context.proceed();
        } finally {
            InterceptorGuard.release(EnterpriseFeature.class);
        }
    }
}
