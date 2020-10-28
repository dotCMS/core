package com.dotcms.company;

import com.dotcms.datagen.TestUserUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/**
 *  A simple class to collect tests company API-related.
 */
public class CompanyAPITest {


    /**
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_key_re_generation_expect_valid_key() throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        final CompanyAPI companyAPI = CompanyAPIFactory.getInstance().getCompanyAPI();
        final Company defaultCompany = companyAPI.getDefaultCompany();
        Assert.assertNotNull(defaultCompany);
        final Company updatedCompany = companyAPI.regenerateKey(defaultCompany, admin);
        Assert.assertNotNull(updatedCompany);
        Assert.assertNotNull(updatedCompany.getKeyObj());
        Assert.assertNotEquals(defaultCompany.getKeyObj(), updatedCompany.getKeyObj());
        final Company freshDefaultCompany = companyAPI.getDefaultCompany();
        Assert.assertNotNull(freshDefaultCompany);
        Assert.assertEquals(updatedCompany.getKeyObj(), freshDefaultCompany.getKeyObj());
        final String test = Base64.objectToString(freshDefaultCompany.getKeyObj());
        final Pattern pattern = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
        final Matcher matcher = pattern.matcher(test);
        Assert.assertTrue(matcher.matches());
    }

}
