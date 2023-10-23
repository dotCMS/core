package com.dotcms.dotpubsub;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import io.vavr.control.Try;

/**
 * A Utility Class that converts the default dotCMS postgres DB connection/driver to a url using the
 * pgjdbc-ng driver, which supports Postgres PubSub. We use this driver to listen for incoming
 * events
 *
 * @author will
 *
 */
class PgNgDataSourceUrl {

    private final String finalUrl;


    static final String SSL_MODE = System.getenv("DOT_PUBSUB_SSL_MODE") != null ? System.getenv("DOT_PUBSUB_SSL_MODE") : "prefer";



    /**
     * Use our existing database connection/url and
     * 
     * @param username
     * @param password
     * @param url
     */
    PgNgDataSourceUrl(final String username, final String password, final String url) {
        final String encodedUsername = Try.of(()->URLEncoder.encode(username, StandardCharsets.UTF_8.toString())).getOrNull();
        final String encodedPassword = Try.of(()->URLEncoder.encode(password, StandardCharsets.UTF_8.toString())).getOrNull();
        this.finalUrl = createUrl(encodedUsername, encodedPassword, url);
    }

    PgNgDataSourceUrl(final String providedUrl) {

        this.finalUrl = providedUrl;
    }

    @Override
    public String toString() {
        return "DbUrl Obfuscated";
    }

    String getDbUrl() {
        return finalUrl;
    }

    /**
     * jdbc:postgresql://oboxturbo/dotcms
     * 
     * @param username
     * @param password
     * @param url
     * @return
     */
    private String createUrl(final String username, final String password, final String url) {

        String[] data = url.split("/");
        StringWriter sw = new StringWriter();
        sw.append("jdbc:pgsql://");
        if (url.contains("@")) {
            sw.append(data[data.length - 3]);
        } else {
            sw.append(username).append(":").append(new String(password)).append("@");
        }
        sw.append(data[data.length - 2]);
        sw.append("/");
        sw.append(data[data.length - 1]);

        if(url.contains("ssl.mode=")){
            return sw.toString();
        }
        if(url.contains("?")){
            sw.append( "&");
        }else{
            sw.append( "?");
        }

        sw.append("ssl.mode=" + SSL_MODE);
        return  sw.toString();


    }

}
