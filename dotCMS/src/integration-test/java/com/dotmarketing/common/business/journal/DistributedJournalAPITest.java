package com.dotmarketing.common.business.journal;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link DistributedJournalAPI}
 */
public class DistributedJournalAPITest extends IntegrationTestBase {

    private static User user;
    private static Host defaultHost;

    private static Language defaultLanguage;

    @BeforeClass
    public static void prepare () throws Exception {
    	//Setting web app environment
        IntegrationTestInitService.getInstance().init();

        HostAPI hostAPI = APILocator.getHostAPI();
        LanguageAPI languageAPI = APILocator.getLanguageAPI();

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        defaultHost = hostAPI.findDefaultHost( user, false );

        //Getting the default language
        defaultLanguage = languageAPI.getDefaultLanguage();
    }

    @Test
    public void test_highestpriority_reindex_vrs_normal_reindex() throws DotDataException {

        final DistributedJournalAPI<String> distributedJournalAPI = APILocator.getDistributedJournalAPI();
        final List<Contentlet>   contentlets = APILocator.getContentletAPI().findAllContent(0, 500);

        if (null != contentlets && contentlets.size() >= 100) {
            return;
        }

        final List<Contentlet>   contentletsHighPriority = contentlets.subList(0, 50);
        final List<Contentlet>   contentletsLowPriority  = contentlets.subList(50, 100);

        assertNotNull(contentletsHighPriority);
        assertTrue(contentletsHighPriority.size() > 0);

        assertNotNull(contentletsLowPriority);
        assertTrue(contentletsLowPriority.size() > 0);

        final Set<String> highIdentifiers = contentletsHighPriority.stream().filter(Objects::nonNull)
                .map(Contentlet::getIdentifier).collect(Collectors.toSet());
        final Set<String> lowIdentifiers  = contentletsHighPriority.stream().filter(Objects::nonNull)
                .map(Contentlet::getIdentifier).collect(Collectors.toSet());

        distributedJournalAPI.addIdentifierReindex(lowIdentifiers);
        distributedJournalAPI.addReindexHighPriority(highIdentifiers);

        // fetch 50
        final List<IndexJournal<String>> indexJournals =
                distributedJournalAPI.findContentReindexEntriesToReindex(false);

        assertNotNull(indexJournals);
        assertTrue(indexJournals.size() > 0);
        assertTrue(highIdentifiers.contains(indexJournals.get(0).getIdentToIndex()));
        assertTrue(indexJournals.size() > 10);
        assertTrue(highIdentifiers.contains(indexJournals.get(9).getIdentToIndex()));
        assertTrue(indexJournals.size() > 20);
        assertTrue(highIdentifiers.contains(indexJournals.get(19).getIdentToIndex()));

        assertTrue(indexJournals.size() > 40);
        assertTrue(highIdentifiers.contains(indexJournals.get(39).getIdentToIndex()));

        final List<IndexJournal<String>> restOfIndexJournals =
                distributedJournalAPI.findContentReindexEntriesToReindex(false);

        assertNotNull(restOfIndexJournals);
        assertTrue(restOfIndexJournals.size() > 10);
        assertTrue(lowIdentifiers.contains(restOfIndexJournals.get(9).getIdentToIndex()));
        assertTrue(restOfIndexJournals.size() > 20);
        assertTrue(highIdentifiers.contains(restOfIndexJournals.get(19).getIdentToIndex()));
        assertTrue(restOfIndexJournals.size() > 30);
        assertTrue(highIdentifiers.contains(restOfIndexJournals.get(39).getIdentToIndex()));
        assertTrue(restOfIndexJournals.size() > 40);
        assertTrue(highIdentifiers.contains(restOfIndexJournals.get(39).getIdentToIndex()));

    }

}
