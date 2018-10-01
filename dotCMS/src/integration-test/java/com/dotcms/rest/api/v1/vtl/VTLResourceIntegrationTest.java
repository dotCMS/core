package com.dotcms.rest.api.v1.vtl;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedHashMap;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class VTLResourceIntegrationTest {

    private final User systemUser = APILocator.systemUser();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] getTestCases() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.put("key1", Collections.singletonList("value1"));
        queryParameters.put("key2", Arrays.asList("value2", "value3"));

        return new VTLResourceTestCase[] {
                new VTLResourceTestCase.Builder().setVtlFile(VALID_GET_VTL_DOTJSON_OUTPUT)
                        .setFolderName("news")
                        .setQueryParameters(queryParameters)
                        .setPathParameters("id/2943b5eb-9105-4dcf-a1c7-87a9d4dc92a6")
                        .setExpectedJSON(VALID_EXPECTED_JSON)
                        .setExpectedOutput(null)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(VALID_GET_VTL_RAW_OUTPUT)
                        .setFolderName("news")
                        .setQueryParameters(queryParameters)
                        .setPathParameters("id/2943b5eb-9105-4dcf-a1c7-87a9d4dc92a6")
                        .setExpectedJSON(null)
                        .setExpectedOutput(VALID_GET_VTL_RAW_OUTPUT)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(INVALID_GET_VTL)
                        .setFolderName("news")
                        .setQueryParameters(queryParameters)
                        .setPathParameters("id/2943b5eb-9105-4dcf-a1c7-87a9d4dc92a6")
                        .setExpectedJSON(null)
                        .setExpectedOutput(null)
                        .setExpectedException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .build(),
        };
    }

    @Test
    @UseDataProvider("getTestCases")
    public void testGet(final VTLResourceTestCase testCase) throws
            DotDataException, DotSecurityException, IOException {

        final Host demoSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final Folder vtlFolder = APILocator.getFolderAPI()
                .createFolders("application/apivtl/" + testCase.getFolderName(),
                        demoSite,
                        APILocator.systemUser(), false);

        try {
            final File getVTLFile = new File(Files.createTempDir(), "get.vtl");
            FileUtil.write(getVTLFile, testCase.getVtlFile());

            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(vtlFolder, getVTLFile);
            final Contentlet getVTLFileAsset = fileAssetDataGen.nextPersisted();
            APILocator.getContentletAPI().publish(getVTLFileAsset, systemUser, false);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            final UriInfo uriInfo = mock(UriInfo.class);
            Mockito.when(uriInfo.getQueryParameters()).thenReturn(testCase.getQueryParameters());

            VTLResource resource = new VTLResource();
            final Response response = resource.get(request, servletResponse, uriInfo, testCase.getFolderName(),
                    testCase.getPathParameters());

            String expectedOutput;
            String getOutput;

            if(testCase.getExpectedException()>0) {
                expectedOutput = Integer.toString(testCase.getExpectedException());
                getOutput = Integer.toString(response.getStatus());
            } else if(UtilMethods.isSet(testCase.getExpectedJSON())) {
                expectedOutput = testCase.getExpectedJSON();
                final ObjectMapper objectMapper = new ObjectMapper();
                getOutput = objectMapper.writeValueAsString(response.getEntity());
            } else {
                expectedOutput = testCase.getExpectedOutput();
                getOutput = (String) response.getEntity();
            }

            assertEquals(expectedOutput, getOutput);

            ;
        } finally {
            APILocator.getFolderAPI().delete(vtlFolder, APILocator.systemUser(), false);
        }


    }

    private static final String VALID_GET_VTL_DOTJSON_OUTPUT = "## GETS NEWS BY ID, LOADS A FEW FIELD\n" +
            "#set($news = $dotcontent.find($urlParams.id))\n" +
            "\n" +
            "##  QUERY PARAMS\n" +
            "\n" +
            "$dotJSON.put(\"queryParam\", $queryParams)\n" +
            "\n" +
            "## NEW ARRAY\n" +
            "#set ($fields = [])   \n" +
            "$fields.add(\"title\")\n" +
            "$fields.add(\"urlTitle\")\n" +
            "$fields.add(\"story\")\n" +
            "\n" +
            "#foreach($field in $fields)\n" +
            "    #set($item = {})\n" +
            "    $item.put($field,${news.get(\"$field\")})\n" +
            "    $dotJSON.put(\"$field\",$item)\n" +
            "#end\n" +
            "\n" +
            "$dotJSON.put(\"cache\", 10)\n";

    private static final String INVALID_GET_VTL =  "#set($news =";

    private static final String VALID_GET_VTL_RAW_OUTPUT = "helloworld";

    private static final String VALID_EXPECTED_JSON = "{\"map\":{\"cache\":10,\"urlTitle\":{\"urlTitle\":\"the-gas-price-rollercoaster\"},\"queryParam\":{\"key1\":[\"value1\"],\"key2\":[\"value2\",\"value3\"]},\"title\":{\"title\":\"The Gas Price Rollercoaster\"},\"story\":{\"story\":\"<p>Houston, Tx.&nbsp;Crude oil and, by default, gasoline prices, are driven by a complex assortment of factors that affect supply and demand, including geopolitical risks, weather, inventories, global economic growth, exchange rates, speculation, hedging and investment activity. From the risk of piracy in the Straits of Malacca and Hormuz, to transit vulnerability in the Caspian and extreme weather in the United States, crude is constantly susceptible to a variety of price-driving forces.<br /><br />The worldwide demand for crude oil, gasoline and petroleum products that are made from crude is expected to increase as the U.S. and global economies strive to recover. This growing demand, coupled with political instability in the Middle East and North Africa (MENA) region and the decline in the U.S. dollar's value, are what pushed gasoline prices to $4 per gallon earlier this year. While gas prices climbed, the average cost for crude oil rose from about $90 per barrel in mid-February, 2011, to about $127 per barrel two months later.</p>\\n<p>Figure 1 shows average regular gasoline prices in the U.S. over the past three years. Crude oil and gasoline reached record highs during 2008, following surges in worldwide demand due in part to increased demand from emerging markets like China and India. During the second half of 2008, demand and prices both tumbled in response to deteriorating economic conditions. From June to December of 2008, gasoline prices fell from just over $4 per gallon to $1.59. Prices rose steadily through the middle of 2009, bounced up and down for the next year, and then by September 2010, gas prices took off with unrelenting daily price increases. Fears of $6 gas, a slowing economy and decreased demand put the brakes on the uptrend, and price increases finally slowed by mid-May, 2011.</p>\\n<p><br /><strong>Where Do Our Dollars Go At The Pump?</strong><br />According to the EIA data from 2010, 68% of every dollar at the gas pump goes to crude oil - the raw material used to produce gasoline. Refining the crude oil into gasoline and retailing, which includes distribution and marketing, adds 18% to the price that consumers pay for gasoline. The remaining 14% of every dollar goes towards excise taxes.<br /><br />In the United States, the federal government excise tax is currently 18.4 cents per gallon. The average fuel tax paid by consumers, however, is 49.5 cents per gallon. The difference comes from state and local government taxes, which vary widely across the U.S. The tax in Alaska, for example, is about 26 cents per gallon; in Connecticut, the rate is more than 70 cents per gallon.<br /><br />Perhaps surprisingly, the United States produces 51% of the oil and petroleum products that it consumes. The rest is imported from other countries, including Canada at the top of the list (25%); Saudi Arabia (12%); Venezuela (10%) and Mexico (9%).<br /><br /><strong>What's Ahead?</strong><br />The Energy Information Administration (EIA) expects that the annual price of WTI (West Texas Intermediate) crude will average $103 per barrel in 2011 and $107 per barrel in 2012; the 2010 average was $79. The EIA projects rising crude costs will add an average of 85 cents more per gallon of gasoline during 2011, with an additional three cents per gallon during 2012. The 2010 average regular pump price was $2.78 per gallon; EIA's forecast for 2011 and 2012 is $3.63 and $3.66, respectively.<br /><br />Despite these carefully crafted forecasts, any one of a vast number of factors could trigger strong changes in crude and gasoline prices - both to the upside and downside.<br /><br /><strong>Up and Down (and Up and Down)</strong><br />The weak U.S. dollar only exaggerates rising global oil prices for American consumers. Countries with strong currencies, including those using the euro and the yen, are generally exposed to smaller price increases. Though high gas prices are never welcome, they have come at a particularly tricky time as unemployment and foreclosure rates remain high and food prices soar. While gas prices in the U.S. are still a dollar higher than this time last year, they continue to drop from the early May highs.</p>\"}}}";

}
