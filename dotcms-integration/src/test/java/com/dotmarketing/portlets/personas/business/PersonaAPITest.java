package com.dotmarketing.portlets.personas.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Set;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;

import io.vavr.Tuple2;
import io.vavr.control.Try;

public class PersonaAPITest {

  private static PersonaAPI personaAPI;
  private static Host host;
  private static Persona persona1, persona2, persona3, persona4;
  private static Tuple2<List<Persona>, Integer> allPersonasOnHost;
  private static Tuple2<List<Persona>, Integer> keyTagPersonasOnHost;

  /** Filter that matches the key tag of every persona built by {@link PersonaDataGen}. */
  private static final String KEY_TAG_FILTER = "keyTag";

  @BeforeClass
  public static void initData() throws Exception {
      IntegrationTestInitService.getInstance().init();
      personaAPI = APILocator.getPersonaAPI();
      // create a host and add 4 personas to it
      host = new SiteDataGen().nextPersisted();
      when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                      .thenReturn(new MultiMessageResources(MultiMessageResourcesFactory.createFactory(), ""));

      deleteAllPersonas();
      logLeftoverPersonas();
      purgeOrphanedPersonas();

    // The API also counts personas on SYSTEM_HOST, so personas left behind by other tests in the
    // suite show up here. Take a baseline instead of assuming the site starts with only the
    // default persona.
    final Tuple2<List<Persona>, Integer> baseline = personaAPI.getPersonasIncludingDefaultPersona(
            host, "", false, 100, 0, null, APILocator.systemUser(), false);
    final Tuple2<List<Persona>, Integer> keyTagBaseline = personaAPI.getPersonasIncludingDefaultPersona(
            host, KEY_TAG_FILTER, false, 100, 0, null, APILocator.systemUser(), false);

    persona1 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona2 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona3 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();
    persona4 = new PersonaDataGen().hostFolder(host.getIdentifier()).nextPersisted();

    allPersonasOnHost  =  personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 100, 0 , null, APILocator.systemUser(), false);
    assertEquals("total allPersonas should be the baseline plus the 4 created personas",
            baseline._2 + 4, allPersonasOnHost._2.intValue());
    assertSame("the default persona should come first",
            personaAPI.getDefaultPersona(), allPersonasOnHost._1.get(0));
    assertContainsCreatedPersonas(allPersonasOnHost._1);

    keyTagPersonasOnHost = personaAPI.getPersonasIncludingDefaultPersona(host, KEY_TAG_FILTER, false, 100, 0, null, APILocator.systemUser(), false);
    assertEquals("total keyTag personas should be the baseline plus the 4 created personas",
            keyTagBaseline._2 + 4, keyTagPersonasOnHost._2.intValue());
    assertContainsCreatedPersonas(keyTagPersonasOnHost._1);
  }

  /**
   * Asserts that every persona created in {@link #initData()} is part of the given list.
   *
   * @param personas the personas returned by the API
   */
  private static void assertContainsCreatedPersonas(final List<Persona> personas) {
    final Set<String> identifiers = personas.stream().map(Persona::getIdentifier).collect(Collectors.toSet());
    for (final Persona created : List.of(persona1, persona2, persona3, persona4)) {
      assertTrue("missing created persona " + created.getKeyTag(), identifiers.contains(created.getIdentifier()));
    }
  }

  /**
   * Removes orphaned persona documents before every test.
   *
   * <p>A destroy can leave the index holding a document whose database rows are gone (#37886):
   * the reindex thread may read a pending entry for the content while the destroy has not
   * committed yet, and write it back after the destroy removed it. The tests in this class destroy
   * personas they create, so an orphan can appear between two tests, and the next one would count
   * it.</p>
   */
  @Before
  public void purgeOrphansBeforeEachTest() throws Exception {
    purgeOrphanedPersonas();
  }

  /**
   * Diagnostic for #37886: logs each persona the API's query finds in the index before this class
   * creates its own, with its content type, host and whether its identifier still exists in the
   * database, so a CI log shows what other tests left behind.
   */
  private static void logLeftoverPersonas() throws Exception {
    final List<ContentletSearch> hits = searchPersonasInIndex();
    if (hits.isEmpty()) {
      return;
    }

    Logger.warn(PersonaAPITest.class, String.format(
            "[#37886] %d leftover persona(s) visible before setup", hits.size()));
    for (final ContentletSearch hit : hits) {
      final String details = Try.of(() -> {
        final Contentlet contentlet = APILocator.getContentletAPI().find(hit.getInode(), APILocator.systemUser(), false);
        return contentlet == null ? "no contentlet for inode"
                : "contentType=" + contentlet.getContentType().variable()
                + ", host=" + contentlet.getHost()
                + ", title=" + contentlet.getTitle();
      }).getOrElseGet(e -> "unable to load: " + e.getMessage());
      Logger.warn(PersonaAPITest.class, String.format(
              "[#37886] leftover persona identifier=%s inode=%s index=%s inDb=%s %s",
              hit.getIdentifier(), hit.getInode(), hit.getIndex(), existsInDb(hit.getIdentifier()), details));
    }
  }

  /**
   * Removes from the index every persona document whose identifier no longer exists in the
   * database, and waits until none is left.
   *
   * <p>A persona that exists in the database is left alone: the baseline taken in
   * {@link #initData()} accounts for it. An orphan cannot be accounted for that way, because the
   * API counts it but cannot load it, so the total and the returned list disagree.</p>
   *
   * <p>Orphans can still be in flight when this runs: the reindex thread may hold a pending entry
   * for content destroyed a moment ago. So this keeps purging until the reindex journal has no
   * entry left for a missing identifier and two consecutive checks find no orphan.</p>
   */
  private static void purgeOrphanedPersonas() {
    final int[] cleanChecks = {0};
    Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      final boolean purged = removeOrphansFromIndex();
      final boolean pending = hasPendingReindexForMissingContent();
      cleanChecks[0] = purged || pending ? 0 : cleanChecks[0] + 1;
      return cleanChecks[0] >= 2;
    });
  }

  /**
   * Removes the orphaned persona documents currently in the index.
   *
   * @return {@code true} when at least one orphan was found and removed
   */
  private static boolean removeOrphansFromIndex() throws Exception {
    final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    final IndexBulkRequest orphanRemovals = indexAPI.createBulkRequest();
    indexAPI.setRefreshPolicy(orphanRemovals, IndexBulkRequest.RefreshPolicy.IMMEDIATE);
    boolean hasOrphans = false;
    for (final ContentletSearch hit : searchPersonasInIndex()) {
      if (!existsInDb(hit.getIdentifier())) {
        Logger.warn(PersonaAPITest.class, String.format(
                "[#37886] removing orphaned persona from the index: identifier=%s inode=%s index=%s",
                hit.getIdentifier(), hit.getInode(), hit.getIndex()));
        // Identifier-wide removal across every language and variant; nothing in the DB to keep.
        indexAPI.appendBulkRemoveRequest(orphanRemovals, ReindexEntry.builder()
                .id(0).identToIndex(hit.getIdentifier()).priority(0).isDelete(true).build());
        hasOrphans = true;
      }
    }
    if (hasOrphans) {
      indexAPI.putToIndex(orphanRemovals);
      // query results (counts included) are cached; drop them so the next count sees the purge
      CacheLocator.getESQueryCache().clearCache();
      CacheLocator.getOSQueryCache().clearCache();
    }
    return hasOrphans;
  }

  /**
   * Whether the reindex journal still holds an entry for an identifier that no longer exists, which
   * the reindex thread may yet turn into an orphaned document.
   */
  private static boolean hasPendingReindexForMissingContent() throws DotDataException {
    return !new DotConnect()
            // entries that already failed (priority ERROR and up) are never picked up again
            .setSQL("select j.id from dist_reindex_journal j where j.priority < ? and not exists "
                    + "(select 1 from identifier i where i.id = j.ident_to_index)")
            .addParam(ReindexQueueFactory.Priority.ERROR.dbValue())
            .setMaxRows(1)
            .loadObjectResults().isEmpty();
  }

  /**
   * Runs the query {@code getPersonasIncludingDefaultPersona} counts with, straight against the
   * index, so that documents without a database row are returned too.
   */
  private static List<ContentletSearch> searchPersonasInIndex() throws DotDataException, DotSecurityException {
    final String query = "+working:true -deleted:true +conHost:(" + host.getIdentifier() + " OR "
            + Host.SYSTEM_HOST + ") +basetype:6";
    return APILocator.getContentletAPI().searchIndex(query, 100, 0, null, APILocator.systemUser(), false);
  }

  /**
   * Whether the identifier still has a row in the database.
   *
   * @param identifier the content identifier
   */
  private static boolean existsInDb(final String identifier) throws DotDataException {
    return !new DotConnect()
            .setSQL("select id from identifier where id = ?")
            .addParam(identifier)
            .loadObjectResults().isEmpty();
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

  /**
   * Requesting the first {@code i} personas returns exactly {@code i}, always with the full total,
   * and the default persona comes first.
   */
  @Test
  public void test_pulling_personas_including_default_persona() throws Exception {

    final int total = allPersonasOnHost._2;
    for (int i = 1; i < 5; i++) {
      Tuple2<List<Persona>, Integer> personas =
          personaAPI.getPersonasIncludingDefaultPersona(host, "", false, i, 0, null, APILocator.systemUser(), false);
      assertEquals("looking for:" + i + " personas back", i, personas._1.size());
      assertEquals("total personas", total, personas._2.intValue());
    }
    Tuple2<List<Persona>, Integer> personas =
        personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 5, 0, null, APILocator.systemUser(), false);
    // the first result should be the default persona
    assertSame(personaAPI.getDefaultPersona(), personas._1.get(0));

  }

  /**
   * Each page (default persona included) is the matching slice of the full, unpaged list, and the
   * last page is cut short at the end of the list.
   */
  @Test
  public void test_pagination_of_pulling_personas_including_default_persona() throws Exception {

    final Tuple2<List<Persona>, Integer> firstPage = personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 1, 0, null, APILocator.systemUser(), false);
    assertPage(allPersonasOnHost, firstPage, 1, 0);
    assertSame(personaAPI.getDefaultPersona(), firstPage._1.get(0));

    assertPage(allPersonasOnHost,
            personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 2, 1, null, APILocator.systemUser(), false),
            2, 1);

    // the page that reaches the end of the list returns fewer items than the limit
    final int lastOffset = allPersonasOnHost._1.size() - 2;
    assertPage(allPersonasOnHost,
            personaAPI.getPersonasIncludingDefaultPersona(host, "", false, 3, lastOffset, null, APILocator.systemUser(), false),
            3, lastOffset);
  }

  /**
   * Filtering by name or key tag: "Def" matches only the default persona, a key tag filter pages
   * through the matching personas without the default one, and an exact key tag finds one persona.
   */
  @Test
  public void test_filtering_personas_by_name_and_keytag_including_default_persona() throws Exception {

    final Tuple2<List<Persona>, Integer> defaultSearch = personaAPI.getPersonasIncludingDefaultPersona(host, "Def", false, 100, 0 , null, APILocator.systemUser(), false);
    assertEquals("total defaultSearch", 1, defaultSearch._2.intValue());
    assertSame(personaAPI.getDefaultPersona(), defaultSearch._1.get(0));

    assertEquals("the default persona does not match the keyTag filter",
            keyTagPersonasOnHost._2.intValue(), keyTagPersonasOnHost._1.size());
    assertTrue("the default persona does not match the keyTag filter",
            keyTagPersonasOnHost._1.stream().noneMatch(p -> p == personaAPI.getDefaultPersona()));

    assertPage(keyTagPersonasOnHost,
            personaAPI.getPersonasIncludingDefaultPersona(host, KEY_TAG_FILTER, false, 2, 1, null, APILocator.systemUser(), false),
            2, 1);

    // the page that reaches the end of the list returns fewer items than the limit
    final int lastOffset = keyTagPersonasOnHost._1.size() - 1;
    assertPage(keyTagPersonasOnHost,
            personaAPI.getPersonasIncludingDefaultPersona(host, KEY_TAG_FILTER, false, 3, lastOffset, null, APILocator.systemUser(), false),
            3, lastOffset);

    final Tuple2<List<Persona>, Integer> exactKeyTag = personaAPI.getPersonasIncludingDefaultPersona(host, persona4.getKeyTag(), false, 100, 0, null, APILocator.systemUser(), false);
    assertEquals("looking for: 1 persona", 1, exactKeyTag._1.size());
    assertEquals("total personas", 1, exactKeyTag._2.intValue());
    assertEquals(persona4.getKeyTag(), exactKeyTag._1.get(0).getKeyTag());
  }

  /**
   * Asserts that a page is the {@code [offset, offset + limit)} slice of the full list, by key tag,
   * and that it reports the same total.
   *
   * @param all    the full, unpaged result
   * @param page   the paged result
   * @param limit  the page size requested
   * @param offset the offset requested
   */
  private static void assertPage(final Tuple2<List<Persona>, Integer> all,
          final Tuple2<List<Persona>, Integer> page, final int limit, final int offset) {
    final List<String> expected = all._1.subList(offset, Math.min(offset + limit, all._1.size()))
            .stream().map(Persona::getKeyTag).collect(Collectors.toList());
    final List<String> actual = page._1.stream().map(Persona::getKeyTag).collect(Collectors.toList());
    assertEquals("page limit=" + limit + " offset=" + offset, expected, actual);
    assertEquals("total personas", all._2, page._2);
  }
  
  @Test
  public void testFindPersonaByTag_CustomPersonaType_ShouldReturnTag()
          throws DotDataException, DotSecurityException {
    ContentType customPersonaType = null;
    Contentlet customPersonaContent = null;

    try {
      long time = System.currentTimeMillis();

      // create custom persona type

      customPersonaType = new ContentTypeDataGen()
              .baseContentType(BaseContentType.PERSONA).nextPersisted();

      customPersonaContent = new ContentletDataGen(customPersonaType.id())
              .setProperty("name", "persona"+time)
              .setProperty("keyTag", "personaKeyTag"+time)
              .nextPersisted();

      final String keyTagValue = customPersonaContent.getStringProperty("keyTag");

      Optional<Persona> optionalPersona = personaAPI.findPersonaByTag(keyTagValue,
              APILocator.systemUser(), false);

      assertTrue(optionalPersona.isPresent());
      assertEquals(keyTagValue, optionalPersona.get().getKeyTag());

    } finally {
        destroyNow(customPersonaContent);
        if(customPersonaType!=null) {
          ContentTypeDataGen.remove(customPersonaType);
        }
    }
  }

  @Test
  public void testgetPersonasIncludingDefaultPersona_filterNewPersonaContentType_ShouldReturnPersonas()
          throws DotSecurityException, DotDataException {
    ContentType customPersonaType = null;
    Contentlet newPersona = null;

    try {

      // create custom persona type

      customPersonaType = new ContentTypeDataGen()
              .host(host)
              .baseContentType(BaseContentType.PERSONA).nextPersisted();

      newPersona = new ContentletDataGen(customPersonaType.id())
              .host(host)
              .setProperty("name", "Testing Filter New CT")
              .setProperty("keyTag", "TestingFilterNewCT")
              .nextPersisted();

      final Tuple2<List<Persona>, Integer> filteredPersonas = personaAPI.getPersonasIncludingDefaultPersona(host,"ilter",false, 100, -1, null, APILocator.systemUser(), false);

      assertEquals(1,filteredPersonas._2.intValue());
      assertEquals(newPersona.getStringProperty("name"),filteredPersonas._1.get(0).getName());
      assertEquals(newPersona.getStringProperty("keyTag"),filteredPersonas._1.get(0).getKeyTag());

    } finally {
      destroyNow(newPersona);
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

    destroyNow(newPersona);
    ContentTypeDataGen.remove(customPersonaType);
  }

  /**
   * Destroys a persona created by a test and removes it from the index before returning.
   *
   * <p>The other tests in this class count every persona visible to {@link #host}. A default
   * (deferred) destroy removes the index document asynchronously, after the commit, so the next
   * test can still count it; {@link IndexPolicy#FORCE} applies the removal inline.</p>
   *
   * @param contentlet the persona to destroy; {@code null} is ignored
   */
  private static void destroyNow(final Contentlet contentlet) throws DotDataException, DotSecurityException {
    if (contentlet == null) {
      return;
    }
    contentlet.setIndexPolicy(IndexPolicy.FORCE);
    APILocator.getContentletAPI().destroy(contentlet, APILocator.systemUser(), false);
  }
}
