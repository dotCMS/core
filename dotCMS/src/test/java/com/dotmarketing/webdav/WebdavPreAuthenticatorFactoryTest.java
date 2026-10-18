package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import org.junit.Test;

/**
 * Covers how the WebDAV pre-authenticator configured in {@code auth.pipeline.pre} is created.
 */
public class WebdavPreAuthenticatorFactoryTest {

    /**
     * A configured {@link Authenticator} implementation is instantiated and usable.
     */
    @Test
    public void createsConfiguredAuthenticator() throws Exception {
        final Authenticator authenticator =
                WebdavPreAuthenticatorFactory.newInstance(TestAuthenticator.class.getName());

        assertTrue(authenticator instanceof TestAuthenticator);
        assertEquals(Authenticator.SUCCESS, authenticator.authenticateByEmailAddress("c", "e", "p"));
        assertEquals(Authenticator.FAILURE, authenticator.authenticateByUserId("c", "u", "p"));
    }

    /**
     * Each call returns a new instance, as before.
     */
    @Test
    public void createsNewInstanceOnEachCall() throws Exception {
        final String className = TestAuthenticator.class.getName();

        assertNotSame(WebdavPreAuthenticatorFactory.newInstance(className),
                WebdavPreAuthenticatorFactory.newInstance(className));
    }

    /**
     * Surrounding whitespace in the configured value is ignored.
     */
    @Test
    public void ignoresSurroundingWhitespace() throws Exception {
        final Authenticator authenticator =
                WebdavPreAuthenticatorFactory.newInstance("  " + TestAuthenticator.class.getName() + " ");

        assertTrue(authenticator instanceof TestAuthenticator);
    }

    /**
     * A class that is not an {@link Authenticator} is rejected without being instantiated.
     */
    @Test
    public void rejectsClassThatIsNotAnAuthenticator() {
        NotAnAuthenticator.created = false;

        assertThrows(IllegalArgumentException.class,
                () -> WebdavPreAuthenticatorFactory.newInstance(NotAnAuthenticator.class.getName()));
        assertFalse("constructor must not run", NotAnAuthenticator.created);
    }

    /**
     * The configured value is treated as a class name only.
     */
    @Test
    public void rejectsValueThatIsNotAClassName() {
        assertThrows(ClassNotFoundException.class,
                () -> WebdavPreAuthenticatorFactory.newInstance(
                        "new " + TestAuthenticator.class.getName() + "()"));
    }

    /**
     * An unknown class name fails.
     */
    @Test
    public void rejectsUnknownClass() {
        assertThrows(ClassNotFoundException.class,
                () -> WebdavPreAuthenticatorFactory.newInstance("com.example.DoesNotExist"));
    }

    /**
     * An implementation without a no-argument constructor fails.
     */
    @Test
    public void rejectsAuthenticatorWithoutNoArgConstructor() {
        assertThrows(NoSuchMethodException.class,
                () -> WebdavPreAuthenticatorFactory.newInstance(NoDefaultConstructorAuthenticator.class.getName()));
    }

    public static class TestAuthenticator implements Authenticator {

        @Override
        public int authenticateByEmailAddress(final String companyId, final String emailAddress,
                final String password) throws AuthException {
            return SUCCESS;
        }

        @Override
        public int authenticateByUserId(final String companyId, final String userId, final String password)
                throws AuthException {
            return FAILURE;
        }
    }

    public static class NoDefaultConstructorAuthenticator extends TestAuthenticator {

        public NoDefaultConstructorAuthenticator(final String ignored) {
        }
    }

    public static class NotAnAuthenticator {

        static boolean created;

        public NotAnAuthenticator() {
            created = true;
        }
    }

}
