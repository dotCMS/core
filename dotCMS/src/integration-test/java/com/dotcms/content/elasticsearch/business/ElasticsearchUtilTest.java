package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.BeforeClass;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Test;

import java.io.IOException;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElasticsearchUtilTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isAnyReadOnly(String...)}
     * When: The Index is not read only
     * Should:return false
     *
     * @throws DotDataException
     */
    @Test
    public void shouldReturnFalseIfTheCurrentIndicesAreNotReadOnly() throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), false);

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
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
    public void shouldReturnTrueIfTheCurrentIndicesAreReadOnly() throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), true);

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertTrue(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
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
    public void shouldPutReadonlyInFalse() throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), true);
            ElasticsearchUtil.putReadOnlyToFalse(indiciesInfo.getWorking());

            final boolean readOnly = ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
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
    public void shouldReturnTrueWhenWorkingIndexIsReadOnly() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), false);
            putReadOnly(indiciesInfo.getWorking(), true);

            final boolean anyCurrentIndicesReadOnly = ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
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
    public void shouldReturnTrueWhenLiveIndexIsReadOnly() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), true);
            putReadOnly(indiciesInfo.getWorking(), false);

            final boolean anyCurrentIndicesReadOnly = ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getLive(), false);
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
    public void shouldSetWorkingAndLiveIndexToReadOnly() throws DotDataException, ElasticsearchResponseException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), false);
            putReadOnly(indiciesInfo.getWorking(), false);

            ElasticsearchUtil.setLiveAndWorkingIndicesToWriteMode();
            assertFalse(ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getLive()));
            assertFalse( ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getWorking()));
        } finally {
            putReadOnly(indiciesInfo.getLive(), false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#putReadOnlyToFalse(String...)}
     * When: If the cluster is set as Read Only
     * Should: set read only property to false
     */
    @Test
    public void setClusterReadOnlyModeToFalse(){
        try {
            setClusterAsReadOnly(true);

            final boolean clusterInReadOnlyMode = ElasticsearchUtil.isClusterInReadOnlyMode();
            assertTrue(clusterInReadOnlyMode);

            ElasticsearchUtil.setClusterToWriteMode();
            assertFalse(ElasticsearchUtil.isClusterInReadOnlyMode());
        }finally {
            setClusterAsReadOnly(false);
        }
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isClusterInReadOnlyMode()}
     * When: The cluster is not read only
     * Should: return false
     */
    @Test
    public void shouldReturnFalseWhenTheClusterIsNotInReadOnlyMode(){
        setClusterAsReadOnly(false);
        assertFalse(ElasticsearchUtil.isClusterInReadOnlyMode());
    }

    /**
     * Method to Test: {@link ElasticsearchUtil#isClusterInReadOnlyMode()}
     * When: The cluster is in read only
     * Should: return true
     */
    @Test
    public void shouldReturnFalseWhenTheClusterIsInReadOnlyMode(){
        try {
            setClusterAsReadOnly(true);
            assertTrue(ElasticsearchUtil.isClusterInReadOnlyMode());
        }finally {
            setClusterAsReadOnly(false);
        }
    }

    private static AcknowledgedResponse putReadOnly(final String indexName, final boolean value) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);

        final Settings.Builder settingBuilder = Settings.builder()
                .put("index.blocks.read_only_allow_delete", value)
                .put("index.blocks.read_only", value);

        request.settings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }

    private static AcknowledgedResponse setClusterAsReadOnly(final boolean value) {
        final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        final Settings.Builder settingBuilder = Settings.builder()
                .put("cluster.blocks.read_only", value);

        request.persistentSettings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }
}
