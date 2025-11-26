package com.dotmarketing.portlets.personas.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import java.util.List;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Config;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;

import io.vavr.Tuple2;

public class PersonaAPITest {

  private static PersonaAPI personaAPI;
  private static Host host;
  private static Persona persona1, persona2, persona3, persona4;
  private static Tuple2<List<Persona>, Integer> allPersonasOnHost;
  
  @BeforeClass
  public static void initData() throws Exception {
      IntegrationTestInitService.getInstance().init();
      personaAPI = APILocator.getPersonaAPI();
      // create a host and add 4 personas to it
      host = new SiteDataGen().nextPersisted();
      when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                      .thenReturn(new MultiMessageResources(MultiMessageResourcesFactory.createFactory(), ""));

      deleteAllPersonas();
    
    
    persona1 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona2 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona3 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona4 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    
    allPersonasOnHost  =  personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 100, 0 , null, APILocator.systemUser(), false);
    assertTrue("total allPersonas should be 5, got:" + allPersonasOnHost._2, allPersonasOnHost._2 == 5);
  }

  private static void deleteAllPersonas() throws Exception{
      final ContentletAPI capi = APILocator.getContentletAPI();
      List<String> inodes = new DotConnect().setSQL(
                       "select working_inode from "
                      + "contentlet_version_info cvi, "
                      + "identifier "
                      + "where "
                      + "identifier.id = cvi.identifier and "
                      + "identifier.asset_subtype='persona' ")
                      .loadObjectResults().stream().map(m -> (String) m.get("working_inode")).collect(Collectors.toList());


    
    // delete system host personas
    List<Contentlet> cons = capi.findContentlets(inodes);
    capi.destroy(cons, APILocator.systemUser(), false);
    APILocator.getCacheProviderAPI().removeAll(false);
    
      
      
  }
  
  
  
  
  @AfterClass
  public static void nullOutData() throws Exception {

    personaAPI = null;
    // create a host and add 4 personas to it
    host = null;

    persona1 = null;
    persona2 = null;
    persona3 = null;
    persona4 = null;

  }

  @Test
  public void test_pulling_personas_including_default_persona() throws Exception {

    // we should get five personas back, because we are including the default persona
    for (int i = 1; i < 5; i++) {
      Tuple2<List<Persona>, Integer> personas =
          personaAPI.getPersonasIncludingDefaultPersona(host, "", false, i, 0, null, APILocator.systemUser(), false);
      assertTrue("looking for:" + i + " personas back, got:" + personas._1.size(), personas._1.size() == i);
      assertTrue("total personas should be 5, got:" + personas._2, personas._2 == 5);
    }
    Tuple2<List<Persona>, Integer> personas =
        personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 5, 0, null, APILocator.systemUser(), false);
    // the first result should be the default persaon
    assert(personas._1.get(0) == personaAPI.getDefaultPersona());

  }

  @Test
  public void test_pagination_of_pulling_personas_including_default_persona() throws Exception {

    // we should get five personas back, because we are including the default persona
    

    Tuple2<List<Persona>, Integer> pagedPersonas =null;
    


   
    System.err.println("\nAll Personas");
    int i=0;
    for(Persona p: allPersonasOnHost._1) {
      System.err.println(i++ + "-" + p.getKeyTag());
    }
    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 1, 0, null, APILocator.systemUser(), false);
    assertTrue("looking for: 1 personas back, got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 1);
    assertTrue("total personas should be 5, got:" + pagedPersonas._2, pagedPersonas._2 == 5);
    assert(pagedPersonas._1.get(0) == personaAPI.getDefaultPersona());
    
    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 2, 1, null, APILocator.systemUser(), false);
    i=0;
    System.err.println("\nPaginaged Personas");
    
    for(Persona p: pagedPersonas._1) {
      System.err.println(i++ + "-" + p.getKeyTag());
    }
    
    
    
    assertTrue("looking for: 1 personas back, got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 2);
    assertTrue("total personas should be 5, got:" + pagedPersonas._2, pagedPersonas._2 == 5);
    
    assertEquals("the first result is the second persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(1).getKeyTag());
    assertEquals("the second result is the third persona:" , pagedPersonas._1.get(1).getKeyTag(), allPersonasOnHost._1.get(2).getKeyTag());
    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 3, 3, null, APILocator.systemUser(), false);
    
    i=0;
    System.err.println("\nPaginaged Personas");
    
    for(Persona p: pagedPersonas._1) {
      System.err.println(i++ + "-" + p.getKeyTag());
    }
    
    
    assertTrue("looking for: 2 personas (even though limit is 3, end of list), got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 2);
    assertTrue("total personas should be 5, got:" + pagedPersonas._2, pagedPersonas._2 == 5);

    
    assertEquals("the first result is the forth persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(3).getKeyTag());
    assertEquals("the second result is the fith persona:" , pagedPersonas._1.get(1).getKeyTag(), allPersonasOnHost._1.get(4).getKeyTag());
    
    
  }

  @Test
  public void test_filtering_personas_by_name_and_keytag_including_default_persona() throws Exception {

    // we should get five personas back, because we are including the default persona
    

    Tuple2<List<Persona>, Integer> pagedPersonas, defaultSearch =null;
    
    defaultSearch  =     pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "Def", false, 100, 0 , null, APILocator.systemUser(), false);
    assertTrue("total defaultSearch should be 1, got:" + defaultSearch._2, defaultSearch._2 == 1);
    assert(defaultSearch._1.get(0) == personaAPI.getDefaultPersona());
    

    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "keyTag", false, 100, 0, null, APILocator.systemUser(), false);
    assertTrue("looking for: 4 personas back, got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 4);
    assertTrue("total personas should be 4, got:" + pagedPersonas._2, pagedPersonas._2 == 4);
    assertEquals("the first result is the second persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(1).getKeyTag());
    
    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "keyTag", false, 2, 1, null, APILocator.systemUser(), false);

    assertTrue("looking for: 2 personas back, got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 2);
    assertTrue("total personas should be 4, got:" + pagedPersonas._2, pagedPersonas._2 == 4);
    
    assertEquals("the first result is the second persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(2).getKeyTag());
    assertEquals("the second result is the third persona:" , pagedPersonas._1.get(1).getKeyTag(), allPersonasOnHost._1.get(3).getKeyTag());
    
    
    
    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, "keyTag", false, 3, 3, null, APILocator.systemUser(), false);

    assertTrue("looking for: 1 personas (even though limit is 3, end of list), got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 1);
    assertTrue("total personas should be 4, got:" + pagedPersonas._2, pagedPersonas._2 == 4);

    
    assertEquals("the first result is the forth persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(4).getKeyTag());

    
    pagedPersonas = personaAPI.getPersonasIncludingDefaultPersona(host, allPersonasOnHost._1.get(4).getKeyTag(), false, 100, 0, null, APILocator.systemUser(), false);
    assertTrue("looking for: 1 persona, got:" + pagedPersonas._1.size(), pagedPersonas._1.size() == 1);
    assertTrue("total personas should be 1, got:" + pagedPersonas._2, pagedPersonas._2 == 1);

    
    assertEquals("the first result is the forth persona:" , pagedPersonas._1.get(0).getKeyTag(), allPersonasOnHost._1.get(4).getKeyTag());
    
  }
  
  @Test
  public void testFindPersonaByTag_CustomPersonaType_ShouldReturnTag()
          throws DotDataException, DotSecurityException {
    ContentType customPersonaType = null;

    try {
      long time = System.currentTimeMillis();

      // create custom persona type

      customPersonaType = new ContentTypeDataGen()
              .baseContentType(BaseContentType.PERSONA).nextPersisted();

      final Contentlet customPersonaContent = new ContentletDataGen(customPersonaType.id())
              .setProperty("name", "persona"+time)
              .setProperty("keyTag", "personaKeyTag"+time)
              .nextPersisted();

      final String keyTagValue = customPersonaContent.getStringProperty("keyTag");

      Optional<Persona> optionalPersona = personaAPI.findPersonaByTag(keyTagValue,
              APILocator.systemUser(), false);

      assertTrue(optionalPersona.isPresent());
      assertEquals(keyTagValue, optionalPersona.get().getKeyTag());

    } finally {
        if(customPersonaType!=null) {
          ContentTypeDataGen.remove(customPersonaType);
        }
    }
  }

  @Test
  public void testgetPersonasIncludingDefaultPersona_filterNewPersonaContentType_ShouldReturnPersonas()
          throws DotSecurityException, DotDataException {
    ContentType customPersonaType = null;

    try {

      // create custom persona type

      customPersonaType = new ContentTypeDataGen()
              .host(host)
              .baseContentType(BaseContentType.PERSONA).nextPersisted();

      final Contentlet newPersona = new ContentletDataGen(customPersonaType.id())
              .host(host)
              .setProperty("name", "Testing Filter New CT")
              .setProperty("keyTag", "TestingFilterNewCT")
              .nextPersisted();

      final Tuple2<List<Persona>, Integer> filteredPersonas = personaAPI.getPersonasIncludingDefaultPersona(host,"ilter",false, 100, -1, null, APILocator.systemUser(), false);

      assertEquals(1,filteredPersonas._2.intValue());
      assertEquals(newPersona.getStringProperty("name"),filteredPersonas._1.get(0).getName());
      assertEquals(newPersona.getStringProperty("keyTag"),filteredPersonas._1.get(0).getKeyTag());

    } finally {
      if (customPersonaType != null) {
        ContentTypeDataGen.remove(customPersonaType);
      }
    }
  }


  /**
   * Method to test: {@link PersonaAPI#find(String identifier,com.liferay.portal.model.User user, boolean respectFrontendRoles)}
   * When: finding a persona
   * Should: the persona should be resolved by id or by keytag
   */
  @Test
  public void test_personas_test_resolve_by_keytag() throws Exception {

    ContentType customPersonaType = new ContentTypeDataGen()
            .host(host)
            .baseContentType(BaseContentType.PERSONA).nextPersisted();

    final Contentlet newPersona = new ContentletDataGen(customPersonaType.id())
            .host(host)
            .setProperty("name", "AnotherPersona" + System.currentTimeMillis())
            .setProperty("keyTag", "AnotherPersona" + System.currentTimeMillis())
            .nextPersisted();

    Contentlet personaById = personaAPI.find(newPersona.getIdentifier(), APILocator.systemUser(), true);


    assertTrue(UtilMethods.isSet(()->personaById.getIdentifier()));


    Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
      try {
        Contentlet personaByKeyTag = personaAPI.find(newPersona.getStringProperty("keyTag"), APILocator.systemUser(), true);
        assert(personaById.getIdentifier().equals(personaByKeyTag.getIdentifier()));
        return true;
      } catch (Exception e) {
        // ignore
      }
      return false;
    });

    APILocator.getContentletAPI().destroy(newPersona, APILocator.systemUser(), false);


  }
}
