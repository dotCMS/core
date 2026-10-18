package com.dotmarketing.webdav;

import com.liferay.portal.auth.Authenticator;

/**
 * Creates the pre-authenticator configured through the {@code auth.pipeline.pre} property for WebDAV logins.
 *
 * <p>The configured value must be the fully qualified name of a concrete class that implements
 * {@link Authenticator} and has a public no-argument constructor.</p>
 */
final class WebdavPreAuthenticatorFactory {

    private WebdavPreAuthenticatorFactory() {
    }

    /**
     * Instantiates the given {@link Authenticator} implementation.
     *
     * @param className Fully qualified name of the {@link Authenticator} implementation.
     *
     * @return A new instance of the configured {@link Authenticator}.
     *
     * @throws ReflectiveOperationException If the class cannot be found or instantiated.
     * @throws IllegalArgumentException     If the class does not implement {@link Authenticator}.
     */
    static Authenticator newInstance(final String className) throws ReflectiveOperationException {
        final Class<?> clazz = Class.forName(className.trim(), false,
                WebdavPreAuthenticatorFactory.class.getClassLoader());
        if (!Authenticator.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(String.format(
                    "Class '%s' configured as pre-authenticator does not implement %s",
                    className, Authenticator.class.getName()));
        }
        return clazz.asSubclass(Authenticator.class).getDeclaredConstructor().newInstance();
    }

}
