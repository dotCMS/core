package com.dotcms.visitor.domain;

import com.dotcms.repackage.com.google.common.collect.HashMultiset;
import com.dotcms.repackage.com.google.common.collect.Multiset;
import com.dotcms.repackage.com.google.common.collect.Multisets;
import eu.bitwalker.useragentutils.DeviceType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TagUtil;
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

    private Multiset<String> _accruedTags = HashMultiset.create();
    		
    		
    		
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

        //Validate if we must accrue the Tags for this "new" Persona
        if ( persona != null &&
                (this.persona == null || !this.persona.getIdentifier().equals(persona.getIdentifier())) ) {

            try {
                //The Persona changed for this Visitor, we must accrue the tags associated to this new Persona
                List<Tag> personaTags = APILocator.getTagAPI().getTagsByInode(persona.getInode());

                String foundTags = TagUtil.tagListToString(personaTags);
                //Accrue these found tags to this visitor object
                TagUtil.accrueTagsToVisitor(this, foundTags);
            } catch (DotDataException e) {
                Logger.error(this, "Unable to retrieve Tags associated to Persona [" + persona.getInode() + "].", e);
            }

        }

        this.persona = persona;
    }

    
   public class AccruedTag implements Serializable {

	   private static final long serialVersionUID = 1L;
	   final String tag;
	   final int count;
	   public AccruedTag ( String tag,  int count){
		   	this.tag = tag;
	   		this.count=count;
	   }
	   public String getTag() {
		   return tag;
	   }
	   public int getCount() {
		   return count;
	   }
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof AccruedTag){
				AccruedTag tag2=(AccruedTag)obj;
				if(tag2.getTag().equals(this.tag) && this.count== tag2.count){
					return true;
				}
			}
			return false;
		}
		@Override
		public String toString() {
			return "{\"tag\":\"" + tag + "\", \"count\":" + count + "}";
			
		}
		
    }
   
    public List<AccruedTag> getAccruedTags() {
    	List<AccruedTag> tags = new ArrayList<>();
		for (String key : Multisets.copyHighestCountFirst(_accruedTags).elementSet()) {
			AccruedTag tag = new AccruedTag(key,_accruedTags.count(key) );
		    tags.add(tag);
		}
		return tags;
    }
    
    public List<AccruedTag> getTags() {

		return getAccruedTags();
    }
    
    public void addAccruedTags(Set<String> tags){
    	for(String tag : tags){
    		addTag(tag);
    	}
    	//_accruedTags.addAll(tags);
    }
    public void addTag(String tag){
    	if(tag==null) return;
    	_accruedTags.add(tag);
    }
    
    public void addTag(String tag, int count){
    	if(tag==null) return;
    	_accruedTags.add(tag, count);
    }
    public void removeTag(String tag){
    	_accruedTags.remove(tag);
    }

    public void clearTags(){
    	_accruedTags = HashMultiset.create();
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
                ", accruedTags=" + _accruedTags +
                ", userAgent=" + userAgent +
                ", device=" + getDevice() +
                ", dmid=" + dmid +
                ", newVisitor=" + newVisitor +
                ", referrer=" + referrer +
                ", lastRequestDate=" + lastRequestDate +
                ", map=" + map +
                '}';
    }


    /**
      * Add uri as a visited page.
      *
      * @param uri
     */
    public void addPagesViewed(String uri){
        pagesViewed.add(uri);
    }

    /**
     * Return the number og page visited by the current user.
     *
     * @return
    */
    public int getNumberPagesViewed(){
        return pagesViewed.size();
    }
}
