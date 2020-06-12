package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.BeforeClass;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class ElasticsearchUtilTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] clusterReadOnlyProperties() {
        return new String[]{"cluster.blocks.read_only", "cluster.blocks.read_only_allow_delete"};
    }

    @DataProvider
    public static Object[] indexReadOnlyProperties() {
        return new String[]{"index.blocks.read_only", "index.blocks.read_only_allow_delete"};
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isAnyReadOnly(String...)}
     * When: The Index is not read only
     * Should:return false
     *
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldReturnFalseIfTheCurrentIndicesAreNotReadOnly(final String propertyName) throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isAnyReadOnly(String...)}
     * When: The Index is read only
     * Should:return true
     *
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldReturnTrueIfTheCurrentIndicesAreReadOnly(final String propertyName) throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), propertyName, true);

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertTrue(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isAnyReadOnly(String...)}
     * When: The Index not exists
     * Should: throw a {@link org.elasticsearch.ElasticsearchStatusException}
     *
     * @throws DotDataException
     */
    @Test (expected = ElasticsearchStatusException.class)
    public void whenTheIndexDoesNotExistsShouldThrowException() throws IOException {
        ElasticsearchUtil.isAnyReadOnly("not_Exists");
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#putReadOnlyToFalse(String...)}
     * When: The index exists
     * Should: should set it to not read only
     *
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldPutReadonlyInFalse(final String propertyName) throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), propertyName, true);
            ElasticsearchUtil.putReadOnlyToFalse(indiciesInfo.getWorking());

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#putReadOnlyToFalse(String...)}
     * When: The index not exists
     * Should: throw a {@link org.elasticsearch.ElasticsearchStatusException}
     *
     */
    @Test(expected = ElasticsearchStatusException.class)
    public void shouldTryPutReadonlyInTrueAndTheIndexDoesNotExists() throws IOException {
        ElasticsearchUtil.putReadOnlyToFalse("index_not_exists");
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isEitherLiveOrWorkingIndicesReadOnly()}
     * When: If at least one of the current working index is read only
     * Should: return true
     *
     * @throws DotDataException
     */
    @Test()
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldReturnTrueWhenWorkingIndexIsReadOnly(final String propertyName) throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), propertyName, false);
            putReadOnly(indiciesInfo.getWorking(), propertyName, true);

            final boolean anyCurrentIndicesReadOnly = ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isEitherLiveOrWorkingIndicesReadOnly()}
     * When: If at least one of the current live index is read only
     * Should: return true
     *
     * @throws DotDataException
     */
    @Test()
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldReturnTrueWhenLiveIndexIsReadOnly(final String propertyName) throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), propertyName, true);
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);

            final boolean anyCurrentIndicesReadOnly = ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getLive(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#setLiveAndWorkingIndicesToWriteMode()}
     * When: If at least one of the current live index if read only
     * Should: return true
     *
     * @throws ElasticsearchResponseException
     * @throws DotDataException
     */
    @Test()
    @UseDataProvider("indexReadOnlyProperties")
    public void shouldSetWorkingAndLiveIndexToReadOnly(final String propertyName) throws DotDataException, ElasticsearchResponseException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), propertyName, false);
            putReadOnly(indiciesInfo.getWorking(), propertyName, false);

            ElasticsearchUtil.setLiveAndWorkingIndicesToWriteMode();
            assertFalse(ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getLive()));
            assertFalse( ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking()));
        } finally {
            putReadOnly(indiciesInfo.getLive(), propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#putReadOnlyToFalse(String...)}
     * When: If the cluster is set as Read Only
     * Should: set read only property to false
     */
    @Test
    @UseDataProvider("clusterReadOnlyProperties")
    public void setClusterReadOnlyModeToFalse(final String propertyName) throws ElasticsearchResponseException {
        try {
            setClusterAsReadOnly(propertyName, true);

            final boolean clusterInReadOnlyMode = ElasticsearchUtil.isClusterInReadOnlyMode();
            assertTrue(clusterInReadOnlyMode);

            ElasticsearchUtil.setClusterToWriteMode();
            assertFalse(ElasticsearchUtil.isClusterInReadOnlyMode());
        }finally {
            setClusterAsReadOnly(propertyName, false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isClusterInReadOnlyMode()}
     * When: The cluster is not read only
     * Should: return false
     */
    @Test
    @UseDataProvider("clusterReadOnlyProperties")
    public void shouldReturnFalseWhenTheClusterIsNotInReadOnlyMode(final String propertyName){
        setClusterAsReadOnly(propertyName, false);
        assertFalse(ElasticsearchUtil.isClusterInReadOnlyMode());
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isClusterInReadOnlyMode()}
     * When: The cluster is in read only
     * Should: return true
     */
    @Test
    @UseDataProvider("clusterReadOnlyProperties")
    public void shouldReturnFalseWhenTheClusterIsInReadOnlyMode(final String propertyName){
        try {
            setClusterAsReadOnly(propertyName, true);
            assertTrue(ElasticsearchUtil.isClusterInReadOnlyMode());
        }finally {
            setClusterAsReadOnly(propertyName, false);
        }
    }

    private static AcknowledgedResponse putReadOnly(final String indexName, final String propertyName, final boolean value) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);

        final Settings.Builder settingBuilder = Settings.builder()
                .put(propertyName, value);

        request.settings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }

    private static AcknowledgedResponse setClusterAsReadOnly(final String propertyName, final boolean value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        final Settings.Builder settingBuilder = Settings.builder()
                .put(propertyName, value);

        request.persistentSettings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
