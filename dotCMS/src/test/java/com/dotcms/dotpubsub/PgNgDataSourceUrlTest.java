package com.dotcms.dotpubsub;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.BeforeClass;
import org.junit.Test;
import io.vavr.control.Try;

public class PgNgDataSourceUrlTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    

    final String PgNgUrl = "jdbc:pgsql://dotcmsUserName:dotcmsPassword@dbServer.com/dotcms?ssl.mode=" + PgNgDataSourceUrl.SSL_MODE;

    
    
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
    
    
    @Test
    public void test_PgNgDataSourceUrl_URL_escapes_special_chars() {

        
        final String url = "jdbc:postgresql://dbServer.com/dotcms";
        final String username = "dotcmsUserName@";
        final String password = ")mvd99/iyH!_=ag=Por/W}%%aKY^ygt+,sC7%%P?APOU$!@$+*";
        final String encodedUsername = Try.of(()->URLEncoder.encode(username, StandardCharsets.UTF_8.toString())).getOrNull();
        final String encodedPassword = Try.of(()->URLEncoder.encode(password, StandardCharsets.UTF_8.toString())).getOrNull();
        assert (encodedPassword!=null);
        assert (!encodedPassword.contains("!"));

        
        final PgNgDataSourceUrl testDataSource = new PgNgDataSourceUrl(username, password, url);

        final String finalUrl = testDataSource.getDbUrl();

        assert (!encodedPassword.equals(password));

        assert (finalUrl.contains(encodedUsername));
        assert (finalUrl.contains(encodedPassword));
        
        
    }

    @Test
    public void test_ssl_mode_is_added() throws MalformedURLException {

        String url = "jdbc:postgresql://dbServer.com/dotcms";
        String username = "username";
        String password = "password";


        PgNgDataSourceUrl testDataSource = new PgNgDataSourceUrl(username, password, url);

        assert testDataSource.getDbUrl().contains("?ssl.mode=" + PgNgDataSourceUrl.SSL_MODE);

        url = "jdbc:postgresql://dbServer.com/dotcms?test=here";


        testDataSource = new PgNgDataSourceUrl(username, password, url);

        assert testDataSource.getDbUrl().contains("&ssl.mode=" + PgNgDataSourceUrl.SSL_MODE);

    }
}
