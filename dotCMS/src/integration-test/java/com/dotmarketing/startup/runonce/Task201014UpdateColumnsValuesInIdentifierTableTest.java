package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;


public class Task201014UpdateColumnsValuesInIdentifierTableTest {

    private static IdentifierAPI identifierAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        identifierAPI = APILocator.getIdentifierAPI();
    }

    private void cleanUpColumns(final List<Identifier> identifiers) throws SQLException {
        final DotConnect dotConnect = new DotConnect();
        for(Identifier identifier:identifiers){
            dotConnect.executeStatement("UPDATE identifier set owner=null, create_date=null,"
                    + " asset_subtype=null where id='" + identifier.getId() + "'");
        }
    }

    private List<Identifier> createIdentifiers() throws DotDataException {
        final List<Identifier> identifiers = new ArrayList<>();

        final Link link = new LinkDataGen().nextPersisted();
        identifiers.add(identifierAPI.find(link.getIdentifier()));

        final Folder folder = new FolderDataGen().nextPersisted();
        identifiers.add(identifierAPI.find(folder.getIdentifier()));

        final Template template = new TemplateDataGen().nextPersisted();
        identifiers.add(identifierAPI.find(template.getIdentifier()));

        final Container container = new ContainerDataGen().nextPersisted();
        identifiers.add(identifierAPI.find(container.getIdentifier()));

        final Contentlet contentlet = TestDataUtils
                .getPageContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId());
        identifiers.add(identifierAPI.find(contentlet.getIdentifier()));

        return identifiers;
    }

    private boolean areColumnsPopulated(final List<Identifier> identifiers)
            throws DotDataException {
        List<Map<String, Object>> results;
        for (Identifier identifier: identifiers){
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("select owner, create_date, asset_subtype from identifier where id=?");
            dotConnect.addParam(identifier.getId());
            results = dotConnect.loadObjectResults();

            if (!areValuesEqual(identifier, results.get(0))){
                return false;
            }
        }
        return true;
    }

    private boolean areValuesEqual(final Identifier expectedResult, final Map<String, Object> result){
        final LocalDateTime expectedDate = new Timestamp(
                expectedResult.getCreateDate().getTime()).toLocalDateTime().truncatedTo(
                ChronoUnit.SECONDS);
        final LocalDateTime resultDate = new Timestamp(
                ((Date)result.get("create_date")).getTime()).toLocalDateTime().truncatedTo(
                ChronoUnit.SECONDS);

        return doesOwnerMatch(expectedResult, result) &&
                resultDate.equals(expectedDate) &&
                doesAssetSubtypeMatch(expectedResult, result);
    }

    private boolean doesOwnerMatch(final Identifier expectedResult, final Map<String, Object> result) {
        return (expectedResult.getOwner() == null && result.get("owner") == null) || result
                .get("owner").equals(expectedResult.getOwner());
    }

    private boolean doesAssetSubtypeMatch(final Identifier expectedResult, final Map<String, Object> result) {
        return (!expectedResult.getAssetType().equals("contentlet") && !UtilMethods
                .isSet(result.get("asset_subtype")))
                || (expectedResult.getAssetType().equals("contentlet") && result
                .get("asset_subtype").equals(expectedResult.getAssetSubType()));
    }

    /**
     * Method to Test: {@link Task201014UpdateColumnsValuesInIdentifierTable#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Populate columns owner, create_date and asset_subtype of the identifier table
     * @throws SQLException
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask_success() throws SQLException, DotDataException {
        final List<Identifier> identifiers = createIdentifiers();
        cleanUpColumns(identifiers);
        final Task201014UpdateColumnsValuesInIdentifierTable task = new Task201014UpdateColumnsValuesInIdentifierTable();
        task.executeUpgrade();
        assertTrue(areColumnsPopulated(identifiers));
    }

}
