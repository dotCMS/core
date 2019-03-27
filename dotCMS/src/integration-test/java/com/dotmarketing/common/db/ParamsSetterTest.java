package com.dotmarketing.common.db;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.map;

public class ParamsSetterTest extends BaseWorkflowIntegrationTest {



    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();

    }

    @Test
    public void setParamsTest() throws DotDataException, DotSecurityException {

        final List<Params>        paramsInsert                         = new ArrayList<>();
        paramsInsert.add(new Params(UUIDGenerator.generateUuid(),
                "Test",
                DbConnectionFactory.now(),
                DbConnectionFactory.now(),
                this.getSystemUserId(),
                "",
                "",
                "",
                1
        ));

        Assert.assertNotNull(new DotConnect().executeBatch(
                        "insert into workflow_task(id, title, creation_date, mod_date, created_by, assigned_to, status, webasset, language_id) values (?,?,?,?,?,?,?,?,?)",
                        paramsInsert,
                        this::setParams));
    }

    private String getSystemUserId() throws DotDataException {

        return (String)new DotConnect()
                .setSQL("select id,role_name,role_key from cms_role where role_key = ?")
                .addParam(UserAPI.SYSTEM_USER_ID)
                .loadObjectResults()
                .stream().findFirst().orElse(map("id", "2af7cde3-459a-47e1-8041-22a18aa5ed3c"))
                .get("id");
    }

    private void setParams(final PreparedStatement preparedStatement,
                           final Params params) throws SQLException {

        int i = 0;
        for (; i < 5; ++i) {

            preparedStatement.setObject(i+1, params.get(i));
        }

        // AssignedTo
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // Status
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // webasset
        DotConnect.setStringOrNull(preparedStatement, i+1, (String)params.get(i++));
        // lang id
        preparedStatement.setObject(i+1, params.get(i));

    }
}