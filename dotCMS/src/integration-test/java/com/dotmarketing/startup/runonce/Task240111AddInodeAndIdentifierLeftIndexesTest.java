package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the {@link Task240111AddInodeAndIdentifierLeftIndexes} Upgrade Task is working as expected.
 *
 * @author Nollymar Longa
 * @since Jan 11st, 2024
 */
public class Task240111AddInodeAndIdentifierLeftIndexesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link Task240111AddInodeAndIdentifierLeftIndexes#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Verifies that new indexes are included in the inode and identifier tables.</li>
     *     <li><b>Expected Result: </b>Each index must be present.</li>
     * </ul>
     */
    @Test
    public void executeTaskUpgrade() throws DotDataException {
        final Task240111AddInodeAndIdentifierLeftIndexes task = new Task240111AddInodeAndIdentifierLeftIndexes();
        task.executeUpgrade();
        final DotDatabaseMetaData dbMetadata = new DotDatabaseMetaData();

        assertTrue("inode_inode_leading_idx index should exist in the inode table", (Try.of(
                () -> dbMetadata.getIndices("inode"))
                .getOrElse(ImmutableList.of())).stream().map(String::toLowerCase).collect(
                Collectors.toList()).contains("inode_inode_leading_idx"));

        assertTrue("identifier_id_leading_idx index should exist in the identifier table", (Try.of(
                        () -> dbMetadata.getIndices("identifier"))
                .getOrElse(ImmutableList.of())).stream().map(String::toLowerCase).collect(
                Collectors.toList()).contains("identifier_id_leading_idx"));

    }

}
