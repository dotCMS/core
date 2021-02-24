package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210218MigrateUserProxyTableTest {

    private static UserAPI userAPI;

    private static final String USER_PROXY_CREATE_SCRIPT_MSSQL = "if not exists (select * from sysobjects where name='USER_PROXY' and xtype='U')"
            + " CREATE TABLE USER_PROXY (\n"
            + "   inode NVARCHAR(36) not null,\n"
            + "   user_id NVARCHAR(255) null,\n"
            + "   prefix NVARCHAR(255) null,\n"
            + "   suffix NVARCHAR(255) null,\n"
            + "   title NVARCHAR(255) null,\n"
            + "   school NVARCHAR(255) null,\n"
            + "   how_heard NVARCHAR(255) null,\n"
            + "   company NVARCHAR(255) null,\n"
            + "   long_lived_cookie NVARCHAR(255) null,\n"
            + "   website NVARCHAR(255) null,\n"
            + "   graduation_year int null,\n"
            + "   organization NVARCHAR(255) null,\n"
            + "   mail_subscription tinyint null,\n"
            + "   var1 NVARCHAR(255) null,\n"
            + "   var2 NVARCHAR(255) null,\n"
            + "   var3 NVARCHAR(255) null,\n"
            + "   var4 NVARCHAR(255) null,\n"
            + "   var5 NVARCHAR(255) null,\n"
            + "   var6 NVARCHAR(255) null,\n"
            + "   var7 NVARCHAR(255) null,\n"
            + "   var8 NVARCHAR(255) null,\n"
            + "   var9 NVARCHAR(255) null,\n"
            + "   var10 NVARCHAR(255) null,\n"
            + "   var11 NVARCHAR(255) null,\n"
            + "   var12 NVARCHAR(255) null,\n"
            + "   var13 NVARCHAR(255) null,\n"
            + "   var14 NVARCHAR(255) null,\n"
            + "   var15 NVARCHAR(255) null,\n"
            + "   var16 NVARCHAR(255) null,\n"
            + "   var17 NVARCHAR(255) null,\n"
            + "   var18 NVARCHAR(255) null,\n"
            + "   var19 NVARCHAR(255) null,\n"
            + "   var20 NVARCHAR(255) null,\n"
            + "   var21 NVARCHAR(255) null,\n"
            + "   var22 NVARCHAR(255) null,\n"
            + "   var23 NVARCHAR(255) null,\n"
            + "   var24 NVARCHAR(255) null,\n"
            + "   var25 NVARCHAR(255) null,\n"
            + "   last_result int null,\n"
            + "   last_message NVARCHAR(255) null,\n"
            + "   no_click_tracking tinyint null,\n"
            + "   cquestionid NVARCHAR(255) null,\n"
            + "   cqanswer NVARCHAR(255) null,\n"
            + "   chapter_officer NVARCHAR(255) null,\n"
            + "   primary key (inode),\n"
            + "   unique (user_id)\n"
            + ");\n";

    private static final String USER_PROXY_CREATE_SCRIPT_MYSQL = "CREATE TABLE IF NOT EXISTS USER_PROXY (\n"
            + "   inode varchar(36) not null,\n"
            + "   user_id varchar(255),\n"
            + "   prefix varchar(255),\n"
            + "   suffix varchar(255),\n"
            + "   title varchar(255),\n"
            + "   school varchar(255),\n"
            + "   how_heard varchar(255),\n"
            + "   company varchar(255),\n"
            + "   long_lived_cookie varchar(255),\n"
            + "   website varchar(255),\n"
            + "   graduation_year integer,\n"
            + "   organization varchar(255),\n"
            + "   mail_subscription tinyint(1),\n"
            + "   var1 varchar(255),\n"
            + "   var2 varchar(255),\n"
            + "   var3 varchar(255),\n"
            + "   var4 varchar(255),\n"
            + "   var5 varchar(255),\n"
            + "   var6 varchar(255),\n"
            + "   var7 varchar(255),\n"
            + "   var8 varchar(255),\n"
            + "   var9 varchar(255),\n"
            + "   var10 varchar(255),\n"
            + "   var11 varchar(255),\n"
            + "   var12 varchar(255),\n"
            + "   var13 varchar(255),\n"
            + "   var14 varchar(255),\n"
            + "   var15 varchar(255),\n"
            + "   var16 varchar(255),\n"
            + "   var17 varchar(255),\n"
            + "   var18 varchar(255),\n"
            + "   var19 varchar(255),\n"
            + "   var20 varchar(255),\n"
            + "   var21 varchar(255),\n"
            + "   var22 varchar(255),\n"
            + "   var23 varchar(255),\n"
            + "   var24 varchar(255),\n"
            + "   var25 varchar(255),\n"
            + "   last_result integer,\n"
            + "   last_message varchar(255),\n"
            + "   no_click_tracking tinyint(1),\n"
            + "   cquestionid varchar(255),\n"
            + "   cqanswer varchar(255),\n"
            + "   chapter_officer varchar(255),\n"
            + "   primary key (inode),\n"
            + "   unique (user_id)\n"
            + ");";

    private static final String USER_PROXY_CREATE_SCRIPT_POSTGRES = "CREATE TABLE IF NOT EXISTS USER_PROXY (\n"
            + "   inode varchar(36) not null,\n"
            + "   user_id varchar(255),\n"
            + "   prefix varchar(255),\n"
            + "   suffix varchar(255),\n"
            + "   title varchar(255),\n"
            + "   school varchar(255),\n"
            + "   how_heard varchar(255),\n"
            + "   company varchar(255),\n"
            + "   long_lived_cookie varchar(255),\n"
            + "   website varchar(255),\n"
            + "   graduation_year int4,\n"
            + "   organization varchar(255),\n"
            + "   mail_subscription bool,\n"
            + "   var1 varchar(255),\n"
            + "   var2 varchar(255),\n"
            + "   var3 varchar(255),\n"
            + "   var4 varchar(255),\n"
            + "   var5 varchar(255),\n"
            + "   var6 varchar(255),\n"
            + "   var7 varchar(255),\n"
            + "   var8 varchar(255),\n"
            + "   var9 varchar(255),\n"
            + "   var10 varchar(255),\n"
            + "   var11 varchar(255),\n"
            + "   var12 varchar(255),\n"
            + "   var13 varchar(255),\n"
            + "   var14 varchar(255),\n"
            + "   var15 varchar(255),\n"
            + "   var16 varchar(255),\n"
            + "   var17 varchar(255),\n"
            + "   var18 varchar(255),\n"
            + "   var19 varchar(255),\n"
            + "   var20 varchar(255),\n"
            + "   var21 varchar(255),\n"
            + "   var22 varchar(255),\n"
            + "   var23 varchar(255),\n"
            + "   var24 varchar(255),\n"
            + "   var25 varchar(255),\n"
            + "   last_result int4,\n"
            + "   last_message varchar(255),\n"
            + "   no_click_tracking bool,\n"
            + "   cquestionid varchar(255),\n"
            + "   cqanswer varchar(255),\n"
            + "   chapter_officer varchar(255),\n"
            + "   primary key (inode),\n"
            + "   unique (user_id)\n"
            + ");";

    private static final String USER_PROXY_CREATE_SCRIPT_ORACLE = "CREATE TABLE USER_PROXY (\n"
            + "   inode varchar2(36) not null,\n"
            + "   user_id varchar2(255),\n"
            + "   prefix varchar2(255),\n"
            + "   suffix varchar2(255),\n"
            + "   title varchar2(255),\n"
            + "   school varchar2(255),\n"
            + "   how_heard varchar2(255),\n"
            + "   company varchar2(255),\n"
            + "   long_lived_cookie varchar2(255),\n"
            + "   website varchar2(255),\n"
            + "   graduation_year number(10,0),\n"
            + "   organization varchar2(255),\n"
            + "   mail_subscription number(1,0),\n"
            + "   var1 varchar2(255),\n"
            + "   var2 varchar2(255),\n"
            + "   var3 varchar2(255),\n"
            + "   var4 varchar2(255),\n"
            + "   var5 varchar2(255),\n"
            + "   var6 varchar2(255),\n"
            + "   var7 varchar2(255),\n"
            + "   var8 varchar2(255),\n"
            + "   var9 varchar2(255),\n"
            + "   var10 varchar2(255),\n"
            + "   var11 varchar2(255),\n"
            + "   var12 varchar2(255),\n"
            + "   var13 varchar2(255),\n"
            + "   var14 varchar2(255),\n"
            + "   var15 varchar2(255),\n"
            + "   var16 varchar2(255),\n"
            + "   var17 varchar2(255),\n"
            + "   var18 varchar2(255),\n"
            + "   var19 varchar2(255),\n"
            + "   var20 varchar2(255),\n"
            + "   var21 varchar2(255),\n"
            + "   var22 varchar2(255),\n"
            + "   var23 varchar2(255),\n"
            + "   var24 varchar2(255),\n"
            + "   var25 varchar2(255),\n"
            + "   last_result number(10,0),\n"
            + "   last_message varchar2(255),\n"
            + "   no_click_tracking number(1,0),\n"
            + "   cquestionid varchar2(255),\n"
            + "   cqanswer varchar2(255),\n"
            + "   chapter_officer varchar2(255),\n"
            + "   primary key (inode),\n"
            + "   unique (user_id)\n"
            + ")";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
        createUserProxyTable();
    }

    private static void createUserProxyTable() throws SQLException, DotDataException {
        DotConnect dotConnect = new DotConnect();

        if (DbConnectionFactory.isPostgres()) {
            dotConnect.executeStatement(USER_PROXY_CREATE_SCRIPT_POSTGRES);
        } else if (DbConnectionFactory.isMySql()) {
            dotConnect.executeStatement(USER_PROXY_CREATE_SCRIPT_MYSQL);
        } else if (DbConnectionFactory.isMsSql()) {
            dotConnect.executeStatement(USER_PROXY_CREATE_SCRIPT_MSSQL);
        } else {

            dotConnect.setSQL("SELECT COUNT(*) as exist FROM user_tables WHERE table_name='USER_PROXY'");
            BigDecimal existTable = (BigDecimal)dotConnect.loadObjectResults().get(0).get("exist");
            if(existTable.longValue() == 0) {
                dotConnect = new DotConnect();
                dotConnect.executeStatement(USER_PROXY_CREATE_SCRIPT_ORACLE);
            }
        }
    }

    /**
     * Method to Test: {@link Task210218MigrateUserProxyTable#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Migrate user's additional info from user_proxy to user_ table
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testUpgradeTask() throws DotDataException, DotSecurityException {

        //Create user with additional info
        final User userWithAdditionalInfo = new UserDataGen()
                .firstName("backendUser" + System.currentTimeMillis()).nextPersisted();

        new DotConnect()
                .setSQL("insert into USER_PROXY (inode, user_id, prefix, suffix, title, var1, var2) values (?, ?, ?, ?, ?, ?, ?)")
                .addParam(UUIDGenerator.generateUuid())
                .addParam(userWithAdditionalInfo.getUserId())
                .addParam("MyPrefix")
                .addParam("MySuffix")
                .addParam("MyTitle")
                .addParam("DummyVar1")
                .addParam("DummyVar2")
                .loadResult();

        //Create user without additional info
        final User userWithoutAdditionalInfo = new UserDataGen()
                .firstName("backendUser" + System.currentTimeMillis()).nextPersisted();

        final Task210218MigrateUserProxyTable task = new Task210218MigrateUserProxyTable();

        assertTrue(task.forceRun());
        task.executeUpgrade();

        User userResult = userAPI.loadUserById(userWithoutAdditionalInfo.getUserId());
        assertFalse(UtilMethods.isSet(userResult.getAdditionalInfo()));

        userResult = userAPI.loadUserById(userWithAdditionalInfo.getUserId());
        assertTrue(UtilMethods.isSet(userResult.getAdditionalInfo()));
        assertEquals("MyPrefix", userResult.getAdditionalInfo().get("prefix"));
        assertEquals("MySuffix", userResult.getAdditionalInfo().get("suffix"));
        assertEquals("MyTitle", userResult.getAdditionalInfo().get("title"));
        assertEquals("DummyVar1", userResult.getAdditionalInfo().get("var1"));
        assertEquals("DummyVar2", userResult.getAdditionalInfo().get("var2"));

    }

}
