package com.dotcms.datagen;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.UUIDGenerator;

/**
 * Class used to create {@link Contentlet} objects of type Persona for test purposes
 *
 * @author Nollymar Longa
 */
public class PersonaDataGen extends ContentletDataGen {


  private String hostFolder;
  private String name;
  private String keyTag;
  private String description;
  private String tags;
  private String photo;
  public String unique;
  private Host host;
  public PersonaDataGen() {
    super(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
    this.unique=UUIDGenerator.shorty();
    this.host=null;
    this.hostFolder = null;
    this.name = "name" + unique;
    this.keyTag = "keyTag" + unique;
    this.description = "description" + unique;
    this.tags = "tagging, tag" + unique;
    this.languageId=APILocator.getLanguageAPI().getDefaultLanguage().getId();
    
  }

  public PersonaDataGen unique(String unique) {
    this.unique = unique;
    return this;
  }

  
  public PersonaDataGen hostFolder(String hostFolder) {
    this.hostFolder = hostFolder;
    return this;
  }

  
  public PersonaDataGen name(String name) {
    this.name = name;
    return this;
  }

  public PersonaDataGen host(Host host) {
    this.host = host;
    return this;
  }
  

  
  public PersonaDataGen keyTag(String keyTag) {
    this.keyTag = keyTag;
    return this;
  }

  
  public PersonaDataGen description(String description) {
    this.description = description;
    return this;
  }

  
  public PersonaDataGen tags(String tags) {
    this.tags = tags;
    return this;
  }

  
  public PersonaDataGen photo(String photo) {
    this.photo = photo;
    return this;
  }

  @Override
  public Persona next() {
    final Map<String, Object> map = new HashMap<>();
    map.put("stInode", PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
    map.put("contentType", PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME);
    map.put("modUser", APILocator.systemUser().getUserId());
    map.put("modDate", new Date(0).getTime());
    map.put("name", this.name);
    map.put("photo", this.photo);
    map.put("description", this.description);
    map.put("tags", this.tags);
    map.put("keyTag", this.keyTag);
    map.put("languageId", this.languageId);
    String hostId = (host!=null) ? host.getIdentifier() : (hostFolder!=null) ? hostFolder : APILocator.systemHost().getIdentifier();
    map.put("host", hostId);
    Persona persona = new Persona(new Contentlet(map));

    return persona;
  }

  @Override
  public Persona nextPersisted() {
    Persona persona = next();

    persona.setIndexPolicy(IndexPolicy.WAIT_FOR);
    persona.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
    persona.setBoolProperty(Contentlet.IS_TEST_MODE, true);
    Contentlet contentlet = persist(persona);
    try {
      return APILocator.getPersonaAPI().fromContentlet(contentlet);
    } catch (Exception e) {
      throw new DotRuntimeException(e);

    }
  }

}
