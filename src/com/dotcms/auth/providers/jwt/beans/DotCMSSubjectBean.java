package com.dotcms.auth.providers.jwt.beans;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates the dotCMS Subject for the JSON Web Token (JWT). The
 * {@code subject} of the JWT used by the system is composed of:
 * <ul>
 * <li>The date of last modification of the user that is trying to log in and
 * generate this token.</li>
 * <li>The user ID.</li>
 * <li>The ID of the company associated to the user.</li>
 * </ul>
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class DotCMSSubjectBean implements Serializable {

    private final String userId;

    private final Date lastModified;

    private final String companyId;

	/**
	 * Creates a Subject bean with the required parameters gathered from the
	 * user that is trying to log into the system.
	 * 
	 * @param lastModified
	 *            - The date of last modification of the user.
	 * @param userId
	 *            - The user ID.
	 * @param companyId
	 *            - The ID of the company associated to the user.
	 */
    public DotCMSSubjectBean(final Date lastModified, final String userId, final String companyId) {
        this.lastModified = lastModified;
        this.userId = userId;
        this.companyId = companyId;
    }

    /**
     * Returns the ID of the user that generated the JWT.
     * 
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the last modification date of the user that generated the JWT.
     * 
     * @return The date of last modification.
     */
    public Date getLastModified() {
        return lastModified;
    }

	/**
	 * Returns the ID of the company associated to the user that generated the
	 * JWT.
	 * 
	 * @return The company ID.
	 */
    public String getCompanyId() {
        return companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DotCMSSubjectBean that = (DotCMSSubjectBean) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (lastModified != null ? lastModified.getTime() != that.lastModified.getTime() : that.lastModified != null) return false;
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
