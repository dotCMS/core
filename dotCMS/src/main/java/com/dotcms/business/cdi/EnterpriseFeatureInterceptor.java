package com.dotcms.business.cdi;

import com.dotcms.business.interceptor.EnterpriseFeatureHandler;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.EnterpriseFeature;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link EnterpriseFeature}. Delegates to
 * {@link EnterpriseFeatureHandler} for the actual logic, keeping the implementation DRY
 * with the ByteBuddy advice.
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
                EnterpriseFeatureHandler.checkLicense(requiredLevel.level, annotation.errorMsg());
            }
            return context.proceed();
        } finally {
            InterceptorGuard.release(EnterpriseFeature.class);
        }
    }
}
