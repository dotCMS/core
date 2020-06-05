package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;
import org.junit.BeforeClass;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Test;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ESIndexUtilTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link ESIndexUtil#isAnyReadOnly(String...)}
     * When: The Index is not read only
     * Should:return false
     *
     * @throws DotDataException
     */
    @Test
    public void shouldReturnFalseIfTheCurrentIndicesAreNotReadOnly() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), false);

            final boolean readOnly = ESIndexUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isAnyReadOnly(String...)}
     * When: The Index is read only
     * Should:return true
     *
     * @throws DotDataException
     */
    @Test
    public void shouldReturnTrueIfTheCurrentIndicesAreReadOnly() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), true);

            final boolean readOnly = ESIndexUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertTrue(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isAnyReadOnly(String...)}
     * When: The Index not exists
     * Should: throw a {@link org.elasticsearch.ElasticsearchStatusException}
     *
     * @throws DotDataException
     */
    @Test (expected = ElasticsearchStatusException.class)
    public void whenTheIndexDoesNotExistsShouldThrowException() {
        ESIndexUtil.isAnyReadOnly("not_Exists");
    }

    /**
     * Method to Test: {@link ESIndexUtil#putReadOnlyToFalse(String...)}
     * When: The index exists
     * Should: should set it to not read only
     *
     * @throws DotDataException
     */
    @Test
    public void shouldPutReadonlyInFalse() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), true);
            ESIndexUtil.putReadOnlyToFalse(indiciesInfo.getWorking());

            final boolean readOnly = ESIndexUtil.isAnyReadOnly(indiciesInfo.getWorking());

            assertFalse(readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#putReadOnlyToFalse(String...)}
     * When: The index not exists
     * Should: throw a {@link org.elasticsearch.ElasticsearchStatusException}
     *
     */
    @Test(expected = ElasticsearchStatusException.class)
    public void shouldTryPutReadonlyInTrueAndTheIndexDoesNotExists()  {
        ESIndexUtil.putReadOnlyToFalse("index_not_exists");
    }

    /**
     * Method to Test: {@link ESIndexUtil#isEitherLiveOrWokingIndicesReadOnly()}
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

            final boolean anyCurrentIndicesReadOnly = ESIndexUtil.isEitherLiveOrWokingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isEitherLiveOrWokingIndicesReadOnly()}
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

            final boolean anyCurrentIndicesReadOnly = ESIndexUtil.isEitherLiveOrWokingIndicesReadOnly();
            assertTrue(anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getLive(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#setLiveAndWorkingIndicesToWriteMode()}
     * When: If at least one of the current live index if read only
     * Should: return true
     *
     * @throws ElasticsearchResponseException
     * @throws DotDataException
     */
    @Test()
    public void shouldSetWorkingAndLiveIndexToReadOnly() throws DotDataException, ElasticsearchResponseException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), false);
            putReadOnly(indiciesInfo.getWorking(), false);

            ESIndexUtil.setLiveAndWorkingIndicesToWriteMode();
            assertFalse(ESIndexUtil.isAnyReadOnly(indiciesInfo.getLive()));
            assertFalse( ESIndexUtil.isAnyReadOnly(indiciesInfo.getWorking()));
        } finally {
            putReadOnly(indiciesInfo.getLive(), false);
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
}
