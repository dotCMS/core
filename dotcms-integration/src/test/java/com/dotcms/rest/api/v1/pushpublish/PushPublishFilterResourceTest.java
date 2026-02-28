package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableMap;
import java.util.Base64;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;

/**
 * This test suite verifies the correct and expected behavior of the {@link PushPublishFilterResource} REST Endpoint.
 * This endpoint allows dotCMS Users to perform CRUD operations on Push Publishing Filters.
 *
 * @author Erick Gonzalez
 * @since Jun 3rd, 2020
 * @see <a href="https://www.dotcms.com/docs/latest/push-publishing-filters#push-publishing-filters">Push Publishing Filters</a>
 */
public class PushPublishFilterResourceTest {

    static HttpServletResponse response;
    static PushPublishFilterResource resource;
    static String filterKey = "filterTestAPI.yml";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        resource = new PushPublishFilterResource();
        response = new MockHttpResponse();
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
    }

    private static void createFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"excludeClasses","Host,Workflow");
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor(filterKey,"Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");

        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }

    private HttpServletRequest getHttpRequest(final boolean authorization) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        if(authorization) {
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

        }

        return request;
    }

    /**
     * Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Get the filters that the user has access to, passing a user
     * ExpectedResult: filters that the user has access to, 200 Code
     *
     */
    @Test
    public void test_getFilter_withUser_success_returnFilter() throws DotDataException {
        createFilter();
        final Response responseResource = resource.getFilters(getHttpRequest(true),response);
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        Assert.assertTrue(responseEntityView.getEntity().toString().contains(filterKey));
    }

    /**
     * Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Get the filters that the user has access to, but no user is set
     * ExpectedResult: 401 Code, Invalid User, SecurityException
     *
     */
    @Test(expected = SecurityException.class)
    public void test_getFilter_noUser_return401() throws DotDataException {
        resource.getFilters(getHttpRequest(false),response);
    }

    /**
     * <ul>
     *     <li>Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}</li>
     *     <li>Given Scenario: Get the filters for the Admin User, sorted by its {@code sort} value.</li>
     *     <li>Expected Result: Filters sorted by sort value, NOT alphabetically.</li>
     * </ul>
     */
    @Test
    public void testFilterDescriptorOrderedBySortValue() throws DotDataException {
        // Initialization
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        final String TEST_FILTER_ONE = "Test Filter One";
        final String TEST_FILTER_TWO = "Test Filter Two";
        final String TEST_FILTER_THREE = "Test Filter Three";

        // Test data generation
        new FilterDescriptorDataGen().key("filterOne.yml").title(TEST_FILTER_ONE).sort("4").clearFilterList(Boolean.FALSE).nextPersisted();
        new FilterDescriptorDataGen().key("filterTwo.yml").title(TEST_FILTER_TWO).sort("1").clearFilterList(Boolean.FALSE).nextPersisted();
        new FilterDescriptorDataGen().key("filterThree.yml").title(TEST_FILTER_THREE).sort("9").clearFilterList(Boolean.FALSE).nextPersisted();

        try {
            final List<FilterDescriptor> filterList = getFilterList();

            // Assertions
            Assert.assertEquals("First filter must be Filter Two", TEST_FILTER_TWO, filterList.get(0).getTitle());
            Assert.assertEquals("Second filter must be Filter One", TEST_FILTER_ONE, filterList.get(1).getTitle());
            Assert.assertEquals("Third filter must be Filter Three", TEST_FILTER_THREE, filterList.get(2).getTitle());
        } catch (final DotDataException e) {
            // An error occurred when retrieving the filters
            throw e;
        } finally {
            APILocator.getPublisherAPI().clearFilterDescriptorList();
        }
    }

    /**
     * <ul>
     *     <li>Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}</li>
     *     <li>Given Scenario: Get the filters for the Admin User, sorted alphabetically by default.</li>
     *     <li>Expected Result: Filters sorted alphabetically, NOT by sort value.</li>
     * </ul>
     */
    @Test
    public void testFilterDescriptorOrderedAlphabetically() throws DotDataException {
        // Initialization
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        final String TEST_FILTER_ONE = "Workflows and Users";
        final String TEST_FILTER_TWO = "Content Only";
        final String TEST_FILTER_THREE = "Audit Content";

        // Test data generation
        new FilterDescriptorDataGen().key("filterOne.yml").title(TEST_FILTER_ONE).clearFilterList(Boolean.FALSE).nextPersisted();
        new FilterDescriptorDataGen().key("filterTwo.yml").title(TEST_FILTER_TWO).clearFilterList(Boolean.FALSE).nextPersisted();
        new FilterDescriptorDataGen().key("filterThree.yml").title(TEST_FILTER_THREE).clearFilterList(Boolean.FALSE).nextPersisted();

        try {
            final List<FilterDescriptor> filterList = getFilterList();

            // Assertions
            Assert.assertEquals("First filter must be Filter Three", TEST_FILTER_THREE, filterList.get(0).getTitle());
            Assert.assertEquals("Second filter must be Filter Two", TEST_FILTER_TWO, filterList.get(1).getTitle());
            Assert.assertEquals("Third filter must be Filter One", TEST_FILTER_ONE, filterList.get(2).getTitle());
        } catch (final DotDataException e) {
            // An error occurred when retrieving the filters
            throw e;
        } finally {
            APILocator.getPublisherAPI().clearFilterDescriptorList();
        }
    }

    /**
     * <ul>
     *     <li>Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}</li>
     *     <li>Given Scenario: Get the filters for the Admin User, sorted by both sort order and alphabetically. In
     *     this case, Filters #3 and #5 must show up in the list in first and second place respectively as they're using
     *     the sort value. The rest of the filters will be ordered alphabetically, by default.</li>
     *     <li>Expected Result: Filters sorted by both sort order and alphabetically.</li>
     * </ul>
     */
    @Test
    public void testFilterDescriptorOrderedBySortOrderAndAlphabetically() throws DotDataException {
        // Initialization
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
        final FilterDescriptorDataGen filterDescriptorDataGen = new FilterDescriptorDataGen();
        final String TEST_FILTER_ONE = "Force Push Everything";
        final String TEST_FILTER_TWO = "Content, Assets and Pages";
        final String TEST_FILTER_THREE = "Push Without Workflows";
        final String TEST_FILTER_FOUR = "Content And Relationships";
        final String TEST_FILTER_FIVE = "Only Selected Items";
        final String TEST_FILTER_SIX = "Everything And Dependencies";

        // Test data generation
        filterDescriptorDataGen.key("filterOne.yml").title(TEST_FILTER_ONE).sort(null).clearFilterList(Boolean.FALSE).nextPersisted();
        filterDescriptorDataGen.key("filterTwo.yml").title(TEST_FILTER_TWO).sort(null).clearFilterList(Boolean.FALSE).nextPersisted();
        filterDescriptorDataGen.key("filterThree.yml").title(TEST_FILTER_THREE).sort("1").clearFilterList(Boolean.FALSE).nextPersisted();
        filterDescriptorDataGen.key("filterFour.yml").title(TEST_FILTER_FOUR).sort(null).clearFilterList(Boolean.FALSE).nextPersisted();
        filterDescriptorDataGen.key("filterFive.yml").title(TEST_FILTER_FIVE).sort("2").clearFilterList(Boolean.FALSE).nextPersisted();
        filterDescriptorDataGen.key("filterSix.yml").title(TEST_FILTER_SIX).sort(null).clearFilterList(Boolean.FALSE).nextPersisted();

        try {
            final List<FilterDescriptor> filterList = getFilterList();

            // Assertions
            // The first two use the sort value
            Assert.assertEquals("First filter must be Filter Three", TEST_FILTER_THREE, filterList.get(0).getTitle());
            Assert.assertEquals("Second filter must be Filter Five", TEST_FILTER_FIVE, filterList.get(1).getTitle());
            // The rest are sorted alphabetically
            Assert.assertEquals("Third filter must be Filter Four", TEST_FILTER_FOUR, filterList.get(2).getTitle());
            Assert.assertEquals("Fourth filter must be Filter Two", TEST_FILTER_TWO, filterList.get(3).getTitle());
            Assert.assertEquals("Fifth filter must be Filter Six", TEST_FILTER_SIX, filterList.get(4).getTitle());
            Assert.assertEquals("Sixth filter must be Filter One", TEST_FILTER_ONE, filterList.get(5).getTitle());
        } catch (final DotDataException e) {
            // An error occurred when retrieving the filters
            throw e;
        } finally {
            APILocator.getPublisherAPI().clearFilterDescriptorList();
        }
    }

    /**
     * Retrieves the current list of {@link FilterDescriptor} objects, based on the ones added via the
     * {@link  FilterDescriptorDataGen} class.
     *
     * @return The list of {@link FilterDescriptor} objects.
     *
     * @throws DotDataException The list of descriptors could not be retrieved.
     */
    private List<FilterDescriptor> getFilterList() throws DotDataException {
        final Response responseResource = resource.getFilters(getHttpRequest(true), response);
        if (Status.OK.getStatusCode() != responseResource.getStatus()) {
            throw new DotDataException("Status code is NOT 200. Something failed!");
        }
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        return (List<FilterDescriptor>) responseEntityView.getEntity();
    }

}
