package com.dotcms.dotpubsub;

import org.junit.BeforeClass;
import org.junit.Test;

public class PgNgDataSourceUrlTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    

    final String PgNgUrl = "jdbc:pgsql://dotcmsUserName:dotcmsPassword@dbServer.com/dotcms";

    
    
    @Test
    public void test_PgNgDataSourceUrl_does_not_mess_with_url() {


        PgNgDataSourceUrl testDataSource = new PgNgDataSourceUrl(PgNgUrl);
        assert testDataSource.getDbUrl().equals(PgNgUrl);
        
        
        
    }
    
    @Test
    public void test_PgNgDataSourceUrl_builts_correctly() {

        
        String url = "jdbc:postgresql://dbServer.com/dotcms";
        String username = "dotcmsUserName";
        String password = "dotcmsPassword";

        final PgNgDataSourceUrl testDataSource = new PgNgDataSourceUrl(username,password,url);
        
        final String finalUrl=testDataSource.getDbUrl();
        
        
        assert finalUrl.equals(PgNgUrl);
        
        
        
    }
    
    
    

}

