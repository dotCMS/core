package com.liferay.portal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * Tests that a {@link Company} keeps its key to itself: the shared JSON mappers leave the key out, and the protected
 * copy carries no key while keeping every other value.
 */
class CompanyTest {

    private MockedStatic<APILocator> apiLocator;

    /**
     * Serving the default user and its role lets a {@link Company} be written to JSON without a running dotCMS.
     */
    @BeforeEach
    void setUp() throws Exception {
        final User defaultUser = new User();
        defaultUser.setUserId("default-user");
        defaultUser.setCompanyId("dotcms.org");
        defaultUser.setLanguageId("en_US");
        defaultUser.setTimeZoneId("UTC");
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.getDefaultUser()).thenReturn(defaultUser);
        // A user written as JSON reads its own role through the locator
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(anyString())).thenReturn(new Role());
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
    }

    /**
     * Releases the static mock of {@link APILocator}.
     */
    @AfterEach
    void tearDown() {
        apiLocator.close();
    }

    /**
     * The shared mappers that write dotCMS objects as JSON.
     *
     * @return the name of each mapper and the mapper
     */
    static Stream<Arguments> sharedMappers() {
        return Stream.of(
                Arguments.of("REST", DotObjectMapperProvider.getInstance().getDefaultObjectMapper()),
                Arguments.of("contentlet JSON", ContentletJsonHelper.INSTANCE.get().objectMapper()));
    }

    /**
     * Method to test: the JSON form of {@link Company}.
     * Given scenario: a company that holds its key is written with one of the shared mappers, with no mix-in.
     * Expected result: the JSON has the company's id, name and portal URL and no key, and the company still holds
     * its key.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sharedMappers")
    void sharedMapper_company_isWrittenWithoutKey(final String name, final ObjectMapper mapper) throws Exception {
        final Company company = sampleCompany();
        final String storedKey = company.getKey();

        final JsonNode written = mapper.readTree(mapper.writeValueAsString(company));

        assertEquals("sample.company", written.get("companyId").asText());
        assertEquals("Sample Company", written.get("name").asText());
        assertEquals("https://sample.example", written.get("portalURL").asText());
        assertFalse(written.has("key"), "the " + name + " mapper must not write the key");
        assertNotNull(storedKey);
        assertEquals(storedKey, company.getKey());
    }

    /**
     * Method to test: {@link Company#getProtected()}.
     * Given scenario: the protected copy of a company that holds its key.
     * Expected result: the copy has no key in any form, keeps the company's other values, including the colors read
     * from the size, type and street fields, and the company itself still holds its key.
     */
    @Test
    void getProtected_companyWithKey_copiesEverythingButTheKey() {
        final Company company = sampleCompany();

        final Company copy = (Company) company.getProtected();

        assertNull(copy.getKey());
        assertNull(copy.getKeyObj());
        assertNull(copy.getKeyDigest());
        assertEquals("sample.company", copy.getCompanyId());
        assertEquals("Sample Company", copy.getName());
        assertEquals("https://sample.example", copy.getPortalURL());
        assertEquals("dotcms@sample.example", copy.getEmailAddress());
        assertEquals("#111111", copy.getBackgroundColor());
        assertEquals("#222222", copy.getPrimaryColor());
        assertEquals("#333333", copy.getSecondaryColor());
        assertNotNull(company.getKey());
        assertNotNull(company.getKeyObj());
    }

    /**
     * Method to test: the serialized form of {@link Company} and {@link CompanyModel}.
     * Given scenario: a company is cached with Java serialization, for example in Redis, and read back by a node
     * running another version of dotCMS.
     * Expected result: both classes keep the serial version they had before, so the cached company stays readable
     * across versions. Neither class declares one, so it is computed from their members: changing a non-private
     * member changes it.
     */
    @Test
    void serialVersion_isUnchanged() {
        assertEquals(-4397169985178733761L, ObjectStreamClass.lookup(Company.class).getSerialVersionUID());
        assertEquals(-8744738597834842569L, ObjectStreamClass.lookup(CompanyModel.class).getSerialVersionUID());
    }

    /**
     * Builds a company with an id, a name, a portal URL, an email address, the three colors and a key.
     *
     * @return the company
     */
    private static Company sampleCompany() {
        final Company company = new Company("sample.company");
        company.setName("Sample Company");
        company.setPortalURL("https://sample.example");
        company.setEmailAddress("dotcms@sample.example");
        company.setSize("#111111");
        company.setType("#222222");
        company.setStreet("#333333");
        company.setKeyObj(new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));
        return company;
    }
}
