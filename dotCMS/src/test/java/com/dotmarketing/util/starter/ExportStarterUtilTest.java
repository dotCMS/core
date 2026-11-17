package com.dotmarketing.util.starter;

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
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * Covers how {@link ExportStarterUtil} writes the company into the starter files.
 */
class ExportStarterUtilTest {

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
     * The two mappers the starter export writes its JSON files with.
     *
     * @return the name of each mapper and the mapper
     */
    static Stream<Arguments> starterMappers() {
        return Stream.of(
                Arguments.of("table data", ExportStarterUtil.tableDataMapper()),
                Arguments.of("additional data", ExportStarterUtil.additionalDataMapper()));
    }

    /**
     * Method to test: {@link ExportStarterUtil#tableDataMapper()} and {@link ExportStarterUtil#additionalDataMapper()}.
     * Given scenario: a company that holds its key, written the way the starter writes it (a list of companies).
     * Expected result: the company comes out with its id, name and portal URL and without the key, and the company
     * itself still holds the key.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("starterMappers")
    void starterMapper_company_isWrittenWithoutKey(final String name, final ObjectMapper mapper) throws Exception {
        final Company company = sampleCompany();
        final String storedKey = company.getKey();

        final JsonNode written = mapper.readTree(mapper.writeValueAsString(List.of(company))).get(0);

        assertEquals("sample.company", written.get("companyId").asText());
        assertEquals("Sample Company", written.get("name").asText());
        assertEquals("https://sample.example", written.get("portalURL").asText());
        assertFalse(written.has("key"), "the " + name + " mapper must not write the key");
        assertNotNull(storedKey);
        assertEquals(storedKey, company.getKey());
    }

    /**
     * Method to test: {@link ExportStarterUtil#additionalDataMapper()}.
     * Given scenario: the company JSON the starter writes is read back into a {@link Company}, as the starter import
     * does.
     * Expected result: the company is read with its id and name and without a key, which the import generates.
     */
    @Test
    void additionalDataMapper_writtenCompany_isReadBackWithoutKey() throws Exception {
        final ObjectMapper mapper = ExportStarterUtil.additionalDataMapper();

        final Company readBack = mapper.readValue(mapper.writeValueAsString(sampleCompany()), Company.class);

        assertEquals("sample.company", readBack.getCompanyId());
        assertEquals("Sample Company", readBack.getName());
        assertNull(readBack.getKey());
    }

    /**
     * Method to test: {@link ExportStarterUtil#tableDataMapper()} and {@link ExportStarterUtil#additionalDataMapper()}.
     * Given scenario: the starter mappers have been built.
     * Expected result: the shared mappers they start from still write every property of a company, so the REST API
     * and the contentlet JSON keep their output.
     */
    @Test
    void starterMappers_leaveTheSharedMappersUnchanged() {
        ExportStarterUtil.tableDataMapper();
        ExportStarterUtil.additionalDataMapper();

        assertNull(DotObjectMapperProvider.getInstance().getDefaultObjectMapper().findMixInClassFor(Company.class));
        assertNull(ContentletJsonHelper.INSTANCE.get().objectMapper().findMixInClassFor(Company.class));
    }

    /**
     * Builds a company with an id, a name, a portal URL and a key.
     *
     * @return the company
     */
    private static Company sampleCompany() {
        final Company company = new Company("sample.company");
        company.setName("Sample Company");
        company.setPortalURL("https://sample.example");
        company.setKeyObj(new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));
        return company;
    }
}
