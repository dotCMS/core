package com.dotcms.rendering.js.proxy;

import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class is used to expose the User object to the javascript engine.
 * @author jsanca
 */
public class JsUser implements Serializable, JsProxyObject<User> {

    private final User user;

    public JsUser(final User user) {
        this.user = user;
    }

    /**
     * Returns the actual user, but can not be retrieved on the JS context.
     * @return User
     */
    public User getUser () {
        return user;
    }

    @Override
    public User  getWrappedObject() {
        return this.getUser();
    }

    @HostAccess.Export
    public String toString() {
        return this.getFullName() + " [ID: " + user.getUserId() + "][email:" + user.getEmailAddress() + "]";
    }

    @HostAccess.Export
    public boolean isDefaultUser() {
        return user.isDefaultUser();
    }

    @HostAccess.Export
    public String getActualCompanyId() {
        return user.getActualCompanyId();
    }

    @HostAccess.Export
    public boolean isPasswordExpired() {
        return user.isPasswordExpired();
    }

    @HostAccess.Export
    public String getFullName() {
        return user.getFullName();
    }

    @HostAccess.Export
    public boolean getFemale() {
        return user.getFemale();
    }

    @HostAccess.Export
    public boolean isMale() {
        return user.isMale();
    }

    @HostAccess.Export
    public boolean isFemale() {
        return user.isFemale();
    }

    @HostAccess.Export
    public Locale getLocale() {
        return user.getLocale();
    }

    @HostAccess.Export
    public TimeZone getTimeZone() {
        return user.getTimeZone();
    }

    @HostAccess.Export
    public String getTimeZoneId() {
        return user.getTimeZoneId();
    }

    @HostAccess.Export
    public String getRecipientId() {
        return user.getRecipientId();
    }

    @HostAccess.Export
    public String getRecipientName() {
        return user.getRecipientName();
    }

    @HostAccess.Export
    public String getRecipientAddress() {
        return user.getRecipientAddress();
    }

    @HostAccess.Export
    public String getRecipientInternetAddress() {
        return user.getRecipientInternetAddress();
    }

    @HostAccess.Export
    public boolean isMultipleRecipients() {
        return user.isMultipleRecipients();
    }

    @HostAccess.Export
    public boolean isAnonymousUser(){
        return user.isAnonymousUser();
    }

    @HostAccess.Export
    public boolean isAdmin() {

       return user.isAdmin();
    }

    @HostAccess.Export
    public boolean isBackendUser() {

        return user.isBackendUser();
    }

    @HostAccess.Export
    public boolean isFrontendUser() {
        return user.isFrontendUser();
    }

    @HostAccess.Export
    public Role getUserRole() {

        return user.getUserRole();

    }

    @HostAccess.Export
    public boolean hasConsoleAccess() {

        return user.hasConsoleAccess();
    }

    @HostAccess.Export
    public String getUserId() {
        return user.getUserId();
    }

    @HostAccess.Export
    public String getCompanyId() {
        return user.getCompanyId();
    }

    @HostAccess.Export
    public String getFirstName() {
        return user.getFirstName();
    }

    @HostAccess.Export
    public String getMiddleName() {
        return user.getMiddleName();
    }

    @HostAccess.Export
    public String getLastName() {
        return user.getLastName();
    }

    @HostAccess.Export
    public String getNickName() {
        return user.getNickName();
    }

    @HostAccess.Export
    public Date getBirthday() {
        return user.getBirthday();
    }

    @HostAccess.Export
    public String getEmailAddress() {
        return user.getEmailAddress();
    }

    @HostAccess.Export
    public String getLanguageId() {
        return user.getLanguageId();
    }

    @HostAccess.Export
    public String getLayoutIds() {
        return user.getLayoutIds();
    }

    @HostAccess.Export
    public String getComments() {
        return user.getComments();
    }

    @HostAccess.Export
    public Date getCreateDate() {
        return user.getCreateDate();
    }

    @HostAccess.Export
    public Date getLoginDate() {
        return user.getLoginDate();
    }

    @HostAccess.Export
    public boolean isActive() {
        return user.isActive();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public ProxyHashMap getAdditionalInfo(){
        final Map additionalInfo = user.getAdditionalInfo();
        return ProxyHashMap.from(additionalInfo);
    }


}
