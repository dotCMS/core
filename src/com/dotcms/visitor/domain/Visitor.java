package com.dotcms.visitor.domain;

import com.dotcms.repackage.eu.bitwalker.useragentutils.DeviceType;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;

import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

public class Visitor implements Serializable {

    private static final long serialVersionUID = 1L;

    private InetAddress ipAddress;

    private Language selectedLanguage;

    private Locale locale;

    private IPersona persona;

    private Set<String> accruedTags = new HashSet<>();

    private UserAgent userAgent;

    private UUID dmid;

    private boolean newVisitor = true;

    private URI referrer;

    private LocalDateTime lastRequestDate;

    private final Map<String, Serializable> map = new HashMap<>();

    private final Set<String> pagesViewed = new HashSet<>();

    //private VisitorsJourney journey;

    public static Visitor newInstance(HttpServletRequest request) {
        return new Visitor();
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Language getSelectedLanguage() {
        return selectedLanguage;
    }

    public void setSelectedLanguage(Language selectedLanguage) {
        this.selectedLanguage = selectedLanguage;
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
    public boolean getNewVisitor() {
        return newVisitor;
    }
    public void setNewVisitor(boolean newVisitor) {
        this.newVisitor = newVisitor;
    }

    public URI getReferrer() {
        return referrer;
    }
    
    public String getDevice() {
    	if(userAgent !=null){
    		return userAgent.getOperatingSystem().getDeviceType().toString();
    	}
        return DeviceType.UNKNOWN.toString();
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

    public Serializable put(String key, Serializable value) {
        return map.put(key, value);
    }

    public Serializable get(String key) {
        return map.get(key);
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

    @Override
    public String toString() {
        return "Visitor{" +
        		"id=" + this.hashCode() +
                ", ipAddress=" + ipAddress +
                ", selectedLanguage=" + selectedLanguage +
                ", locale=" + locale +
                ", persona=" + persona +
                ", accruedTags=" + accruedTags +
                ", userAgent=" + userAgent +
                ", device=" + getDevice() +
                ", dmid=" + dmid +
                ", newVisitor=" + newVisitor +
                ", referrer=" + referrer +
                ", lastRequestDate=" + lastRequestDate +
                ", map=" + map +
                '}';
    }

    public void addPagesViewed(String uri){
        pagesViewed.add(uri);
    }

    public int getNumberPagesViewed(){
        return pagesViewed.size();
    }
}
