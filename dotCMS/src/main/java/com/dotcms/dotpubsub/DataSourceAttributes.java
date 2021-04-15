package com.dotcms.dotpubsub;


import java.io.StringWriter;

class DataSourceAttributes {


    private final String username, url;
    private final char[] password;



    DataSourceAttributes(final String username, final String password, final String url) {
        super();
        this.username = username;
        this.password = password.toCharArray();
        this.url = url;


    }


    @Override
    public String toString() {
        return "{username=" + username + ", password=******, url=" + url + "}";
    }


    String getDbUrl() {
        StringWriter sw = new StringWriter();
        sw.append(url.substring(url.indexOf(":") + 1, url.indexOf("://") + 3))
        .append(username)
        .append(":")
        .append(new String(password))
        .append("@")
        .append(url.substring(url.indexOf("://") + 3, url.length()));
        return "jdbc:" + sw.toString().replace("postgresql", "pgsql");



    }

}