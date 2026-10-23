package com.dotmarketing.cms.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.model.Company;
import java.lang.reflect.Field;
import javax.servlet.ServletContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Guards the default-company cache added to {@link PublicCompanyFactory}. Localizing an error
 * message through {@code LanguageUtil#get(String)} while a transaction is already aborted must
 * not re-read the company row: the read fails with \"current transaction is aborted\" and turns
 * friendly 400s into 500s for session-less API-token callers. A successful load is cached for the
 * JVM's life (the row is per-install static configuration); a failed load is retried.
 */
public class PublicCompanyFactoryTest {

    private ServletContext originalContext;

    @Before
    public void setUp() throws Exception {
        originalContext = Config.CONTEXT;
        resetCachedCompany();
    }

    @After
    public void tearDown() throws Exception {
        Config.CONTEXT = originalContext;
        resetCachedCompany();
    }

    /** Clears the static cache so each test starts from the uncached state. */
    private static void resetCachedCompany() throws Exception {
        final Field cache = PublicCompanyFactory.class.getDeclaredField("defaultCompany");
        cache.setAccessible(true);
        cache.set(null, null);
    }

    /**
     * The company row must be loaded once and then served from the cache, so callers inside an
     * aborted transaction never issue a database read for it.
     */
    @Test
    public void cachesTheDefaultCompanyAfterTheFirstSuccessfulLoad() {

        final Company first = mock(Company.class);
        final ServletContext context = mock(ServletContext.class);
        when(context.getInitParameter("company_id")).thenReturn("company-id");
        Config.CONTEXT = context;

        try (MockedStatic<CompanyUtil> companyUtil = mockStatic(CompanyUtil.class)) {
            companyUtil.when(() -> CompanyUtil.findByPrimaryKey("company-id")).thenReturn(first);

            assertSame(first, PublicCompanyFactory.getDefaultCompany());
            assertSame(first, PublicCompanyFactory.getDefaultCompany());

            companyUtil.verify(() -> CompanyUtil.findByPrimaryKey("company-id"), times(1));
        }
    }

    /**
     * A failed load must not be cached: the next call retries the read, preserving the
     * before-the-database-is-ready behavior of the uncached lookup.
     */
    @Test
    public void retriesAfterAFailedLoad() {

        final Company company = mock(Company.class);
        final ServletContext context = mock(ServletContext.class);
        when(context.getInitParameter("company_id")).thenReturn("company-id");
        Config.CONTEXT = context;

        try (MockedStatic<CompanyUtil> companyUtil = mockStatic(CompanyUtil.class)) {
            companyUtil.when(() -> CompanyUtil.findByPrimaryKey("company-id"))
                    .thenThrow(new RuntimeException("not ready"))
                    .thenReturn(company);

            assertThrows(DotRuntimeException.class, PublicCompanyFactory::getDefaultCompany);
            assertSame(company, PublicCompanyFactory.getDefaultCompany());
        }
    }

    /**
     * The error message is unchanged from the uncached lookup, so existing callers and tests
     * that match on it keep working.
     */
    @Test
    public void keepsTheNoCompanyMessageOnAFailure() {

        final ServletContext context = mock(ServletContext.class);
        when(context.getInitParameter("company_id")).thenReturn("company-id");
        Config.CONTEXT = context;

        try (MockedStatic<CompanyUtil> companyUtil = mockStatic(CompanyUtil.class)) {
            companyUtil.when(() -> CompanyUtil.findByPrimaryKey("company-id"))
                    .thenThrow(new RuntimeException("boom"));

            final DotRuntimeException error =
                    assertThrows(DotRuntimeException.class, PublicCompanyFactory::getDefaultCompany);
            assertEquals("No Company!", error.getMessage());
        }
    }
}