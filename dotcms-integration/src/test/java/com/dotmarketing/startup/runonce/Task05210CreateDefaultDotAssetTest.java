package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class Task05210CreateDefaultDotAssetTest {

    private static final String dotAssetInode = Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_INODE;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private List<Map<String, Object>> removeConstraintIfAny() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        //Certain db engines store unique constraints as indices
        final String [] fieldInodes = Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_FIELD_INODES;
        for (final String fieldInode : fieldInodes) {
            try {
                new DotConnect().setSQL("delete from field where inode = ?")
                        .addParam(fieldInode).loadResult();
            } catch (DotDataException e) {
                //Nah.
            }
        }

        for (final String fieldInode : fieldInodes) {
            try {
                new DotConnect().setSQL("delete from inode where inode = ?")
                        .addParam(fieldInode).loadResult();
            } catch (DotDataException e) {
                //Nah.
            }
        }

        final List<Map<String, Object>> workflowSchemeXStructure = new DotConnect().setSQL("select * from workflow_scheme_x_structure where structure_id = ?")
                .addParam(dotAssetInode)
                .loadObjectResults();

        new DotConnect().setSQL("delete from workflow_scheme_x_structure where structure_id = ?")
                .addParam(dotAssetInode).loadResult();

        new DotConnect().setSQL("delete from structure where inode = ?")
                .addParam(dotAssetInode).loadResult();

        new DotConnect().setSQL("delete from inode where inode = ?")
                .addParam(dotAssetInode).loadResult();

        return workflowSchemeXStructure;
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException, DotSecurityException {
        deleteAlldotAssetContent();
        final List<Map<String, Object>> constraint = removeConstraintIfAny();

        final Task05210CreateDefaultDotAsset task =  new Task05210CreateDefaultDotAsset();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        addConstraintIfAny(constraint);
    }

    private void addConstraintIfAny(final  List<Map<String, Object>> constraint) throws DotDataException {
        FactoryLocator.getWorkFlowFactory().saveSchemeIdsForContentType(
                dotAssetInode,
                constraint.stream().map(register -> (String) register.get("scheme_id")).collect(Collectors.toSet()),
                null
        );
    }

    private void deleteAlldotAssetContent() throws DotSecurityException, DotDataException {
        final List<Contentlet> contentletList = APILocator.getContentletAPI()
                .findByStructure(dotAssetInode,APILocator.systemUser(),false,-1,0);
        APILocator.getContentletAPI().delete(contentletList,APILocator.systemUser(),false,true);
    }


}
