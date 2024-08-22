package com.dotmarketing.portlets.personas.business;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PERSONA_KEY_TAG;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.config.DotInitializer;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import io.vavr.Tuple2;
import io.vavr.control.Try;

public class PersonaAPIImpl implements PersonaAPI, DotInitializer {

  private final PersonaFactory personaFactory = FactoryLocator.getPersonaFactory();
  private final List<ContentletListener<Persona>> personaListenerList = new CopyOnWriteArrayList<>();
  private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

  private Persona defaultPersona = null;

  @Override
  public Persona getDefaultPersona() {
    if (defaultPersona == null) {
      final Map<String, Object> map = new HashMap<>();
      map.put("stInode", PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_INODE);
      map.put("contentType", PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME);
      map.put("hostFolder", APILocator.systemHost().getIdentifier());
      map.put("modUser", APILocator.systemUser().getUserId());
      map.put("modDate", new Date(0).getTime());
      map.put("name", PersonaAPI.DEFAULT_PERSONA_NAME_KEY);
      map.put("personalized", Boolean.FALSE);
      map.put("hostName", "SYSTEM_HOST");
      map.put("folder", Folder.SYSTEM_FOLDER);
      map.put("host", "SYSTEM_HOST");
      map.put("contentType", "persona");
      map.put("archived", false);
      map.put("working", true);
      map.put("live", true);
      map.put("baseType", "PERSONA");
      map.put("keyTag", Persona.DOT_PERSONA_PREFIX_SCHEME);
      map.put("hasTitleImage", false);
      map.put("identifier", PersonaAPI.DEFAULT_PERSONA_NAME_KEY);
      defaultPersona = fromContentlet(new Contentlet(map));

    }
    return defaultPersona;

  }

  /// Event stuff
  @Override
  public void addPersonaListener(final ContentletListener<Persona> personaListener) {

    if (null != personaListener) {
      this.personaListenerList.add(personaListener);
    }
  }

  private boolean isPersona(final Contentlet contentlet) {

    if (null != contentlet) {

      final ContentType contentType = contentlet.getContentType();
      return null != contentType && contentType instanceof PersonaContentType;
    }

    return false;
  }

  @Subscriber
  public void onArchiveUnArchive(final ContentletArchiveEvent event) {

    if (null != event && this.isPersona(event.getContentlet())) {

      final ContentletArchiveEvent<Persona> archiveEvent =
          ContentletArchiveEvent.wrapContentlet(this.fromContentlet(event.getContentlet()), event);

      this.personaListenerList.stream().forEach(personaListener -> personaListener.onArchive(archiveEvent));
    }
  } // onArchive.

  @Subscriber
  public void onPublishUnPublish(final ContentletPublishEvent event) {

    if (null != event && this.isPersona(event.getContentlet())) {

      final ContentletPublishEvent<Persona> publishEvent =
          ContentletPublishEvent.wrapContentlet(this.fromContentlet(event.getContentlet()), event);

      this.personaListenerList.stream().forEach(personaListener -> personaListener.onModified(publishEvent));
    }
  } // onPublishUnPublish.

  @Subscriber
  public void onDeleted(final ContentletDeletedEvent event) {

    if (null != event && this.isPersona(event.getContentlet())) {

      final ContentletDeletedEvent<Persona> deleteEvent =
          ContentletDeletedEvent.wrapContentlet(this.fromContentlet(event.getContentlet()), event);

      this.personaListenerList.stream().forEach(personaListener -> personaListener.onDeleted(deleteEvent));
    }
  } // onDeleted.

  @Subscriber
  public void onCheckin(final ContentletCheckinEvent event) {

    if (null != event && this.isPersona(event.getContentlet())) {

      final ContentletCheckinEvent<Persona> checkinEvent =
          ContentletCheckinEvent.wrapContentlet(this.fromContentlet(event.getContentlet()), event);

      this.personaListenerList.stream().forEach(personaListener -> personaListener.onModified(checkinEvent));
    }
  } // onCheckin.

  @Override
  public void init() {

    this.localSystemEventsAPI.subscribe(this);
    this.addPersonaListener(new MultiTreePersonaListener());
  }
  /// Event stuff

  @Override
  public List<Field> getBasePersonaFields(Structure structure) {
    List<Field> fields = new ArrayList<>();
    Field field = null;
    int i = 1;

    field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, i++, "",
        "", "", true, false, true);
    field.setVelocityVarName(HOST_FOLDER_FIELD);
    field.setFieldContentlet("system_field1");
    fields.add(field);

    field = new Field(NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, i++, "", "", "", true, false,
        true);
    field.setVelocityVarName(NAME_FIELD);
    field.setFieldContentlet("text1");
    fields.add(field);

    field = new Field(KEY_TAG_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, i++,
        "$velutil.mergeTemplate('/static/personas/keytag_custom_field.vtl')", "", "[a-zA-Z0-9]+", true, false, true);
    field.setVelocityVarName(KEY_TAG_FIELD);
    field.setFieldContentlet("text2");
    fields.add(field);

    field = new Field(PHOTO_FIELD_NAME, Field.FieldType.BINARY, Field.DataType.BINARY, structure, false, false, false, i++, "", "", "",
        true, false, false);
    field.setVelocityVarName(PHOTO_FIELD);
    field.setFieldContentlet("binary1");
    fields.add(field);

    field = new Field(TAGS_FIELD_NAME, Field.FieldType.TAG, Field.DataType.LONG_TEXT, structure, false, true, true, i++, "", "", "", true,
        false, true);
    field.setVelocityVarName(TAGS_FIELD);
    field.setFieldContentlet("text_area1");
    field.setListed(false);;
    fields.add(field);

    field = new Field(DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, false, i++, "",
        "", "", true, false, true);
    field.setVelocityVarName(DESCRIPTION_FIELD);
    field.setFieldContentlet("text_area2");
    fields.add(field);

    return fields;

  }

  @WrapInTransaction
  @Override
  public void createPersonaBaseFields(Structure structure) throws DotDataException, DotStateException {
    if (structure == null || !InodeUtils.isSet(structure.getInode())) {
      throw new DotStateException("Can not create base Persona fields on a structure that does not exist");
    }
    if (structure.getStructureType() != Structure.STRUCTURE_TYPE_PERSONA) {
      throw new DotStateException("Can not create base Persona fields on a structure that is not Persona type");
    }

    List<Field> fields = getBasePersonaFields(structure);
    for (Field f : fields) {
      FieldFactory.saveField(f);
    }
  }

  @Override
  public Host getParentHost(Persona persona) throws DotDataException, DotStateException, DotSecurityException {
    return APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(persona).getHostId(), APILocator.getUserAPI().getSystemUser(),
        false);
  }

  @Override
  public List<Persona> getLiveHTMLPages(Host parent, User user, boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {
    return getPersonas(parent, true, false, user, respectFrontEndRoles);
  }

  @Override
  public List<Persona> getWorkingHTMLPages(Host parent, User user, boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {
    return getPersonas(parent, false, false, user, respectFrontEndRoles);
  }

  @Override
  public List<Persona> getDeletedPersonas(Host parent, User user, boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {
    return getPersonas(parent, false, true, user, respectFrontEndRoles);
  }

  @Override
  public List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {
    return getPersonas(parent, live, deleted, -1, 0, "", user, respectFrontEndRoles);
  }

  @Override
  public List<Persona> getPersonas(Treeable parent, boolean live, boolean deleted, int limit, int offset, String sortBy, User user,
      boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
    List<Persona> personas = new ArrayList<>();
    StringBuilder query = new StringBuilder();
    String liveWorkingDeleted = (live) ? " +live:true " : (deleted) ? " +working:true +deleted:true " : " +working:true -deleted:true ";
    query.append(liveWorkingDeleted);
    if (parent instanceof Host) {
      query.append(" +conFolder:SYSTEM_FOLDER");
      query.append(" +conHost:(").append(parent.getIdentifier()).append(" OR ").append(Host.SYSTEM_HOST).append(")");
    } else if (parent instanceof Folder) {
      query.append(" +conFolder:").append(parent.getIdentifier()).append(" ");
    }

    query.append(" +structureType:" + Structure.STRUCTURE_TYPE_PERSONA);
    if (!UtilMethods.isSet(sortBy)) {
      sortBy = "title desc";
    }
    List<Contentlet> contentlets =
        APILocator.getContentletAPI().search(query.toString(), limit, offset, sortBy, user, respectFrontEndRoles);
    for (Contentlet cont : contentlets) {
      personas.add(fromContentlet(cont));
    }
    return personas;
  }

  @Override
  public Tuple2<List<Persona>, Integer> getPersonasIncludingDefaultPersona(Treeable parent, String filter, boolean live, final int limit,
       int offset, String sortBy, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {

    List<Persona> personas = new ArrayList<>();
    StringBuilder query = new StringBuilder();
    boolean includeDefaultInList = (offset <= 0);
    boolean includeDefaultInCount = true;
    if (UtilMethods.isSet(filter)) {

      Language foundLanguage =
          Try.of(() -> WebAPILocator.getLanguageWebAPI().getLanguage(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
              .getOrElse(APILocator.getLanguageAPI().getDefaultLanguage());
      String displayName = APILocator.getLanguageAPI().getStringKey(foundLanguage, PersonaAPI.DEFAULT_PERSONA_NAME_KEY) + "";
      includeDefaultInList = (displayName.toLowerCase().contains(filter.toLowerCase()));
      includeDefaultInCount=includeDefaultInList;
      List<ContentType> personaTypes= APILocator.getContentTypeAPI(APILocator.systemUser()).findByBaseType(BaseContentType.PERSONA, "mod_date", 100, 0);
      final Iterator<ContentType> personaTypeIterator = personaTypes.iterator();
      if (!personaTypes.isEmpty()) {
        query.append(StringPool.PLUS + StringPool.OPEN_PARENTHESES);
        while (personaTypeIterator.hasNext()) {
          final ContentType personaType = personaTypeIterator.next();
          query.append(String.format("(%s.name:*%s*  %s.keytag:*%s*)",personaType.variable(),filter,personaType.variable(),filter));
          if(personaTypeIterator.hasNext()){
             query.append(" OR ");
          }
        }
        query.append(StringPool.CLOSE_PARENTHESES);
      }
    }

    if (includeDefaultInList) {
      personas.add(getDefaultPersona());
    }
    if(includeDefaultInCount) {
      offset=(offset<1) ? 0:--offset;
    }

    String liveWorkingDeleted = (live) ? " +live:true " : " +working:true -deleted:true ";
    query.append(liveWorkingDeleted);
    if (parent instanceof Host) {
      query.append(" +conHost:(").append(parent.getIdentifier()).append(" OR ").append(Host.SYSTEM_HOST).append(")");
    } else if (parent instanceof Folder) {
      query.append(" +conFolder:").append(parent.getIdentifier()).append(" ");
    }

    query.append(" +basetype:6 ");
    if (!UtilMethods.isSet(sortBy)) {
      sortBy = "title desc";
    }

    List<Contentlet> contentlets =
        APILocator.getContentletAPI().search(query.toString(), limit, offset, sortBy, user, respectFrontEndRoles);
    for (Contentlet cont : contentlets) {
      personas.add(fromContentlet(cont));
    }
    while(includeDefaultInList && personas.size()>limit) {
      personas.remove(personas.size()-1);
    }

    Long count = APILocator.getContentletAPI().indexCount(query.toString(), user, respectFrontEndRoles) + (includeDefaultInCount ? 1 : 0);

    return new Tuple2(personas, count.intValue());
  }

  /**
   * This method checks to insure that we are not persisting a persona contentlet that duplicates the
   * keytag of any other persona on the same host (across structures), Key tag fields are intended to
   * be unique on Hosts).
   */
  @Override
  public void validatePersona(Contentlet c) throws DotContentletValidationException {
    Persona persona = fromContentlet(c);
    User user = null;
    try {
      user = APILocator.getUserAPI().loadUserById(c.getModUser());
    } catch (Exception e) {
      try {
        user = APILocator.getUserAPI().getSystemUser();
      } catch (DotDataException e1) {
        throw new DotContentletValidationException("User Not Found");
      }
    }

    if (c.getLanguageId() != APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
      throw new DotContentletValidationException("Can't create Persona in a Language different than default language");
    }

    String keyTag = persona.getKeyTag();

    // we need to make sure no other persona has the same keyfield
    List<Structure> personaStructs = StructureFactory.getStructures("structuretype=6", "mod_date", -1, 0, "asc");

    StringWriter sw = new StringWriter();
    if (UtilMethods.isSet(persona.getIdentifier())) {
      sw.append(" -identifier:").append(persona.getIdentifier());
    }
    if (UtilMethods.isSet(persona.getInode())) {
      sw.append(" -inode:").append(persona.getInode());
    }

    sw.append(" +conhost:").append(persona.getHost());
    sw.append(" +basetype:6 +languageid:* +(");

    for (Structure s : personaStructs) {
      sw.append(s.getVelocityVarName()).append(".").append(KEY_TAG_FIELD).append(":").append(keyTag).append(" ");

    }
    sw.append(") ");

    try {

      if (APILocator.getContentletAPI().indexCount(sw.toString(), APILocator.getUserAPI().getSystemUser(), false) > 0) {
        Language l = APILocator.getLanguageAPI().getLanguage(user.getLanguageId());
        String message = APILocator.getLanguageAPI().getStringKey(l, "message.persona.error.invalidKeyTagField");
        message = message.replace("{0}", persona.getKeyTag());

        throw new DotContentletValidationException(message);
      }

    } catch (DotDataException | DotSecurityException e) {
      throw new DotContentletValidationException(e.getMessage(), e);
    }

  }

  /**
   * A Persona key tag should be persist as a Tag. When the <code>enable</code> parameter is
   * <code>true</code>, the tag will be created if does not already exist. Saving a Persona tag in the
   * <i>Tag</i> table will set the value of the "persona" column to <code>true</code>.
   *
   * @param personaContentlet - The Persona contentlet that is being saved.
   * @param enable - If <code>true</code> the tag to be saved will be handled as a Persona tag.
   *        Otherwise, the tag will be handled as a regular tag.
   * @throws DotDataException An error occurred when saving the data.
   * @throws DotSecurityException The current user does not have permissions to perform the requested
   *         action.
   */
  @WrapInTransaction
  public void enableDisablePersonaTag(Contentlet personaContentlet, boolean enable) throws DotDataException, DotSecurityException {

    Persona persona = fromContentlet(personaContentlet);
    String keyTag = persona.getKeyTag();

    if(null == keyTag || keyTag.isEmpty()) {
      return;
    }
    
    // Search for the tag related to this key tag, either in current host or
    // system host
    Tag foundPersonaKeyTag = APILocator.getTagAPI().getTagByNameAndHost(keyTag, persona.getHost());
    if (foundPersonaKeyTag == null || !UtilMethods.isSet(foundPersonaKeyTag.getTagId())) {
      foundPersonaKeyTag = APILocator.getTagAPI().getTagByNameAndHost(keyTag, Host.SYSTEM_HOST);
    }

    // Make sure the tag exist for this key tag, if not we need to create it
    if (enable && (foundPersonaKeyTag == null || !UtilMethods.isSet(foundPersonaKeyTag.getTagId()))) {

      // Persist the key tag as a Tag
      APILocator.getTagAPI().getTagAndCreate(keyTag, persona.getHost(), true);
      return;
    }

    if (foundPersonaKeyTag != null && UtilMethods.isSet(foundPersonaKeyTag.getTagId())) {
      // Disable/enable this tag as Persona tag
      APILocator.getTagAPI().enableDisablePersonaTag(foundPersonaKeyTag.getTagId(), enable);
    }
  }

  @Override
  public Persona find(final String identifier, final User user, final boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {

    return this.find(identifier, user, respectFrontEndRoles, false);
  }

  @Override
  public Persona findLive(final String identifier, final User user, final boolean respectFrontEndRoles)
      throws DotDataException, DotSecurityException {

    return this.find(identifier, user, respectFrontEndRoles, true);
  }

  private Persona find(final String identifier, final User user, final boolean respectFrontEndRoles, final boolean live)
      throws DotDataException, DotSecurityException {

    final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier, live,
        APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, respectFrontEndRoles);


    if(contentlet != null) {
      return fromContentlet(contentlet);
    }


    return findPersonaByTag(identifier, user, respectFrontEndRoles).orElseGet(()->null);
  }

  @Override
  public Persona fromContentlet(final Contentlet con) throws DotStateException {

    return new Persona(con);
  }

  @WrapInTransaction
  @Override
  public void createDefaultPersonaStructure() throws DotDataException {

    personaFactory.createDefaultPersonaStructure();
  }

  @Override
  public Structure getDefaultPersonaStructure() throws DotSecurityException, DotDataException {

    Structure defaultStr =
        APILocator.getStructureAPI().findByVarName(PersonaAPI.DEFAULT_PERSONAS_STRUCTURE_VARNAME, APILocator.getUserAPI().getSystemUser());
    return defaultStr;
  }

  @CloseDBIfOpened
  @Override
  public Optional<Persona> findPersonaByTag(final String personaTag, final User user, final boolean respectFrontEndRoles)
      throws DotSecurityException, DotDataException {

    if(UtilMethods.isEmpty(personaTag)){
      return Optional.empty();
    }

    final StringBuilder query = new StringBuilder(" +baseType:").append(BaseContentType.PERSONA.getType())
        .append(" +").append(PERSONA_KEY_TAG).append(":").append(personaTag);

    final List<Contentlet> contentlets =
        APILocator.getContentletAPI().search(query.toString(), 1, 0, StringPool.BLANK, user, respectFrontEndRoles);
    final Optional<Contentlet> persona = null != contentlets ? contentlets.stream().findFirst() : Optional.empty();
    return persona.isPresent() ? Optional.ofNullable(fromContentlet(persona.get())) : Optional.empty();
  }

}
