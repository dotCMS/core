package com.dotmarketing.util.jwt;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates the Dot CMS Subject for the Json web Token
 * @author jsanca
 */
public class DotCMSSubjectBean implements Serializable {

    private final String userId;

    private final Date lastModified;

    private final String companyId;

    public DotCMSSubjectBean(Date lastModified, String userId, String companyId) {
        this.lastModified = lastModified;
        this.userId = userId;
        this.companyId = companyId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getLastModified() {
        return lastModified;
    }


    public String getCompanyId() {
        return companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DotCMSSubjectBean that = (DotCMSSubjectBean) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (lastModified != null ? !lastModified.equals(that.lastModified) : that.lastModified != null) return false;
        return companyId != null ? companyId.equals(that.companyId) : that.companyId == null;

    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
        result = 31 * result + (companyId != null ? companyId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DotCMSSubjectBean{" +
                "userId='" + userId + '\'' +
                ", lastModified=" + lastModified +
                ", companyId='" + companyId + '\'' +
                '}';
    }
} // E:O:F:DotCMSSubjectBean.
