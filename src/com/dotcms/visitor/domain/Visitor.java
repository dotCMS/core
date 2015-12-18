package com.dotcms.visitor.domain;

import com.dotmarketing.portlets.personas.model.IPersona;
import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Visitor implements Serializable {

    private static final long serialVersionUID = 1L;

    private InetAddress ipAddress;

    private long selectedLanguageId;

    private Locale locale;

    private IPersona persona;

    private Set<String> accruedTags;

    private UserAgent userAgent;

    private UUID dmid;

    private boolean newVisitor = true;

    private URI referrer;

    private LocalDateTime lastRequestDate;

    //private VisitorsJourney journey;

//    public Visitor ipAddress(String ipAddress) {
//        this.ipAddress = ipAddress;
//        return this;
//    }


    public static Visitor newInstance(HttpServletRequest request) {
        return new Visitor();
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getSelectedLanguageId() {
        return selectedLanguageId;
    }

    public void setSelectedLanguageId(long selectedLanguageId) {
        this.selectedLanguageId = selectedLanguageId;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public IPersona getPersona() {
        return persona;
    }

    public void setPersona(IPersona persona) {
        this.persona = persona;
    }

    public Set<String> getAccruedTags() {
        return accruedTags;
    }

    public void setAccruedTags(Set<String> accruedTags) {
        this.accruedTags = accruedTags;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public UUID getDmid() {
        return dmid;
    }

    public void setDmid(UUID dmid) {
        this.dmid = dmid;
    }

    public boolean isNewVisitor() {
        return newVisitor;
    }

    public void setNewVisitor(boolean newVisitor) {
        this.newVisitor = newVisitor;
    }

    public URI getReferrer() {
        return referrer;
    }

    public void setReferrer(URI referrer) {
        this.referrer = referrer;
    }

    public LocalDateTime getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(LocalDateTime lastRequestDate) {
        this.lastRequestDate = lastRequestDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Visitor visitor = (Visitor) o;

        return !(dmid != null ? !dmid.equals(visitor.dmid) : visitor.dmid != null);

    }

    @Override
    public int hashCode() {
        return dmid != null ? dmid.hashCode() : 0;
    }


}
