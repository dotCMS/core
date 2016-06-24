package com.dotmarketing.util.jwt;

import java.io.Serializable;

/**
 * Encapsulates the Json Web Token data
 * @author jsanca
 */
public class JWTBean implements Serializable {

    private final String id;
    private final String subject;
    private final String issuer;
    private final long ttlMillis;

    public JWTBean(String id, String subject, String issuer, long ttlMillis) {
        this.id = id;
        this.subject = subject;
        this.issuer = issuer;
        this.ttlMillis = ttlMillis;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JWTBean jwtBean = (JWTBean) o;

        if (ttlMillis != jwtBean.ttlMillis) return false;
        if (id != null ? !id.equals(jwtBean.id) : jwtBean.id != null) return false;
        if (subject != null ? !subject.equals(jwtBean.subject) : jwtBean.subject != null) return false;
        return issuer != null ? issuer.equals(jwtBean.issuer) : jwtBean.issuer == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (issuer != null ? issuer.hashCode() : 0);
        result = 31 * result + (int) (ttlMillis ^ (ttlMillis >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "JWTBean{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", issuer='" + issuer + '\'' +
                ", ttlMillis=" + ttlMillis +
                '}';
    }
} // E:O:F:JWTBean.
