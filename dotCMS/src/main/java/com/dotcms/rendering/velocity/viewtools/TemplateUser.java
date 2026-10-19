package com.dotcms.rendering.velocity.viewtools;

import com.liferay.portal.model.User;
import java.util.HashMap;

/**
 * A {@link User} as handed to Velocity templates by the {@code $cmsuser} view tool.
 * <p>
 * It is a detached copy of the user loaded through {@code UserAPI}, so whatever a template
 * does to it never reaches the instance held by the user cache. The copy carries the
 * profile data (name, email, locale, dates) but none of the stored credential material:
 * {@link #getPassword()}, {@link #getRememberMeToken()} and {@link #getSkinId()} (the
 * remember-me token shares that column) always return {@code null}.
 */
public class TemplateUser extends User {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the template-facing copy of the given user.
     *
     * @param source the user loaded through the API; it is not modified
     */
    public TemplateUser(final User source) {
        super(source.getUserId(),
                source.getCompanyId() != null ? source.getCompanyId() : "",
                null,
                source.getPasswordEncrypted(),
                source.getPasswordExpirationDate(),
                source.getPasswordReset(),
                source.getFirstName(),
                source.getMiddleName(),
                source.getLastName(),
                source.getNickName(),
                source.getMale(),
                source.getBirthday(),
                source.getEmailAddress(),
                source.getSmsId(),
                source.getAimId(),
                source.getIcqId(),
                source.getMsnId(),
                source.getYmId(),
                source.getFavoriteActivity(),
                source.getFavoriteBibleVerse(),
                source.getFavoriteFood(),
                source.getFavoriteMovie(),
                source.getFavoriteMusic(),
                source.getLanguageId(),
                source.getTimeZoneId(),
                null,
                source.getDottedSkins(),
                source.getRoundedSkins(),
                source.getGreeting(),
                source.getResolution(),
                source.getRefreshRate(),
                source.getLayoutIds(),
                source.getComments(),
                source.getCreateDate(),
                source.getLoginDate(),
                source.getLoginIP(),
                source.getLastLoginDate(),
                source.getLastLoginIP(),
                source.getFailedLoginAttempts(),
                source.getAgreedToTermsOfUse(),
                source.getActive(),
                source.getDeleteInProgress(),
                source.getDeleteDate(),
                source.getAdditionalInfo() != null ? new HashMap<>(source.getAdditionalInfo()) : null);
        setModificationDate(source.getModificationDate());
        setNew(source.isNew());
        setModified(false);
    }

    /**
     * Templates never see the stored password.
     *
     * @return always {@code null}
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Templates never see the remember-me token.
     *
     * @return always {@code null}
     */
    @Override
    public String getRememberMeToken() {
        return null;
    }

    /**
     * The skin id shares storage with the remember-me token, so it is hidden as well.
     *
     * @return always {@code null}
     */
    @Override
    public String getSkinId() {
        return null;
    }

    /**
     * Copies stay template-facing.
     *
     * @return a new {@link TemplateUser} with the same data
     */
    @Override
    public Object clone() {
        return new TemplateUser(this);
    }

}
