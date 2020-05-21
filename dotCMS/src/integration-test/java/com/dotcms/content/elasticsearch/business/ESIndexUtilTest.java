package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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

public class ESIndexUtilTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link ESIndexUtil#isReadOnly(String...)}
     * When: The Index is not read only
     * Should:return false
     *
     * @throws DotDataException
     */
    @Test
    public void shouldReturnFalseIfTheCurrentIndicesAreReadOnly() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getWorking(), false);

            final boolean readOnly = ESIndexUtil.isReadOnly(indiciesInfo.getWorking());

            assertEquals(false, readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isReadOnly(String...)}
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

            final boolean readOnly = ESIndexUtil.isReadOnly(indiciesInfo.getWorking());

            assertEquals(true, readOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isReadOnly(String...)}
     * When: The Index not exists
     * Should: throw a {@link org.elasticsearch.ElasticsearchStatusException}
     *
     * @throws DotDataException
     */
    @Test (expected = ElasticsearchStatusException.class)
    public void whenTheIndexNotExistsShouldThrowException() {
        final boolean readOnly = ESIndexUtil.isReadOnly("not_Exists");
        assertEquals(true, readOnly);
    }

    /**
     * Method to Test: {@link ESIndexUtil#putReadOnlyToFalse(String...)}
     * When: The index exists
     * Should: should set it to read only
     *
     * @throws DotDataException
     */
    @Test
    public void shouldPutReadonlyInTrue() throws DotDataException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            ESIndexUtil.putReadOnlyToFalse(indiciesInfo.getWorking());

            final boolean readOnly = ESIndexUtil.isReadOnly(indiciesInfo.getWorking());

            assertEquals(true, !readOnly);
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
     * Method to Test: {@link ESIndexUtil#isAnyCurrentIndicesReadOnly()}
     * When: If at least one of the current working index if read only
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

            final boolean anyCurrentIndicesReadOnly = ESIndexUtil.isAnyCurrentIndicesReadOnly();
            assertEquals(true, anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getWorking(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#isAnyCurrentIndicesReadOnly()}
     * When: If at least one of the current live index if read only
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

            final boolean anyCurrentIndicesReadOnly = ESIndexUtil.isAnyCurrentIndicesReadOnly();
            assertEquals(true, anyCurrentIndicesReadOnly);
        } finally {
            putReadOnly(indiciesInfo.getLive(), false);
        }
    }

    /**
     * Method to Test: {@link ESIndexUtil#putCurrentIndicesToWriteMode()}
     * When: If at least one of the current live index if read only
     * Should: return true
     *
     * @throws ESResponseException
     * @throws DotDataException
     */
    @Test()
    public void shouldSetWorkingAndLiveIndexToReadOnly() throws DotDataException, ESResponseException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        try {
            putReadOnly(indiciesInfo.getLive(), false);
            putReadOnly(indiciesInfo.getWorking(), false);

            ESIndexUtil.putCurrentIndicesToWriteMode();
            assertEquals(false, ESIndexUtil.isReadOnly(indiciesInfo.getLive()));
            assertEquals(false, ESIndexUtil.isReadOnly(indiciesInfo.getWorking()));
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
