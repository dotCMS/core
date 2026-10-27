package com.dotmarketing.portlets.hostvariable.bussiness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for saving and reading Site Variables through {@link HostVariableAPI}: a saved variable stays on
 * the Site it belongs to, an id that is not one of the given Site's variables is refused, and the variables handed to
 * callers are independent of the cached ones.
 */
public class HostVariableAPIImplTest {

    private static User systemUser;
    private static HostAPI hostAPI;
    private static HostVariableAPI hostVariableAPI;

    private static Host siteA;
    private static Host siteB;
    private static User editorOfSiteA;
    private static User editorOfBothSites;
    private static User readerOfBothSites;
    private static final List<User> users = new ArrayList<>();
    private static final List<Host> sites = new ArrayList<>();

    private static final int SLOW_TEST_REPORT_MINUTES = 3;
    private static ScheduledExecutorService slowTestReporter;
    private ScheduledFuture<?> slowTestReport;

    /**
     * Starts the slow-test reporter, boots the integration test environment and creates two Sites plus three users:
     * one who can edit Site A only, one who can edit both Sites, and one who can only read both Sites.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        slowTestReporter = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "HostVariableAPIImplTest-slow-test-report");
            thread.setDaemon(true);
            return thread;
        });
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        hostAPI = APILocator.getHostAPI();
        hostVariableAPI = APILocator.getHostVariableAPI();

        siteA = newSite();
        siteB = newSite();

        final int readAndEdit = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT;
        editorOfSiteA = newUser();
        grant(siteA, editorOfSiteA, readAndEdit);
        editorOfBothSites = newUser();
        grant(siteA, editorOfBothSites, readAndEdit);
        grant(siteB, editorOfBothSites, readAndEdit);
        readerOfBothSites = newUser();
        grant(siteA, readerOfBothSites, PermissionAPI.PERMISSION_READ);
        grant(siteB, readerOfBothSites, PermissionAPI.PERMISSION_READ);
    }

    /**
     * Removes the Sites (and with them their variables) and the users the tests created, then stops the slow-test
     * reporter. Every step is best effort so one failure does not hide the others.
     */
    @AfterClass
    public static void cleanup() {
        for (final Host site : sites) {
            try {
                hostAPI.archive(site, systemUser, false);
                hostAPI.delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.error(HostVariableAPIImplTest.class, "Unable to remove test site", e);
            }
        }
        users.forEach(user -> UserDataGen.remove(user, true));
        slowTestReporter.shutdownNow();
    }

    /**
     * Schedules a report of every thread's stack and of the database sessions that are waiting, written to the log
     * every {@value #SLOW_TEST_REPORT_MINUTES} minutes while the test runs, so a test that stops making progress shows
     * where it waits. The report does not interrupt the test.
     */
    @Before
    public void scheduleSlowTestReport() {
        slowTestReport = slowTestReporter.scheduleAtFixedRate(HostVariableAPIImplTest::logThreadsAndDatabaseWaits,
                SLOW_TEST_REPORT_MINUTES, SLOW_TEST_REPORT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Cancels the slow-test report of the test that just finished.
     */
    @After
    public void cancelSlowTestReport() {
        slowTestReport.cancel(false);
    }

    /**
     * Writes to the log the state, the held and awaited locks and the full stack of every thread, then the sessions of
     * the current database with the sessions blocking them and the lock requests that are not granted. The threads are
     * written first, so they are logged even when the database cannot be read.
     */
    private static void logThreadsAndDatabaseWaits() {
        final StringBuilder threads = new StringBuilder("Test still running after more than ")
                .append(SLOW_TEST_REPORT_MINUTES).append(" minutes. Threads:\n");
        for (final ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            threads.append('"').append(thread.getThreadName()).append("\" ").append(thread.getThreadState());
            if (thread.getLockName() != null) {
                threads.append(" waiting on ").append(thread.getLockName());
            }
            if (thread.getLockOwnerName() != null) {
                threads.append(" owned by \"").append(thread.getLockOwnerName()).append('"');
            }
            threads.append('\n');
            for (final StackTraceElement frame : thread.getStackTrace()) {
                threads.append("    at ").append(frame).append('\n');
            }
        }
        Logger.warn(HostVariableAPIImplTest.class, threads.toString());

        final StringBuilder database = new StringBuilder("Database sessions:\n");
        try {
            database.append(new DotConnect().setSQL(
                    "SELECT pid, state, wait_event_type, wait_event, pg_blocking_pids(pid) AS blocked_by, "
                            + "now() - xact_start AS transaction_age, left(query, 300) AS query "
                            + "FROM pg_stat_activity WHERE datname = current_database()").loadObjectResults());
            database.append("\nLock requests not granted:\n").append(new DotConnect().setSQL(
                    "SELECT l.pid, l.locktype, l.mode, c.relname FROM pg_locks l "
                            + "LEFT JOIN pg_class c ON c.oid = l.relation WHERE NOT l.granted").loadObjectResults());
        } catch (final Exception e) {
            database.append("could not be read: ").append(e.getMessage());
        } finally {
            DbConnectionFactory.closeSilently();
        }
        Logger.warn(HostVariableAPIImplTest.class, database.toString());
    }

    /**
     * Given a variable of Site B, when a user who can edit Site A only saves it by id with Site A as its Site, then
     * the save is refused and the stored row keeps its Site, key, value and last modifier.
     */
    @Test
    public void save_withIdOfAnotherSiteVariable_isRejectedAndStoredRowIsUnchanged() throws Exception {
        final String key = uniqueKey("other");
        final String id = createVariable(siteB, key, "valueOnB");
        final Map<String, Object> before = storedRow(id).orElseThrow();

        assertThrows(DotDataException.class, () -> hostVariableAPI.save(
                variable(id, siteA, uniqueKey("changed"), "changedValue", editorOfSiteA), editorOfSiteA, false));

        assertEquals(before, storedRow(id).orElseThrow());
        assertTrue(keysOf(siteB).contains(key));
        assertFalse(idsOf(siteA).contains(id));
    }

    /**
     * Given a variable of Site B, when a user who can edit both Sites saves it by id with Site A as its Site, then the
     * save is refused too: a variable keeps the Site it was created on.
     */
    @Test
    public void save_withIdOfAnotherSiteVariable_isRejectedEvenWhenUserCanEditBothSites() throws Exception {
        final String id = createVariable(siteB, uniqueKey("both"), "valueOnB");
        final Map<String, Object> before = storedRow(id).orElseThrow();

        assertThrows(DotDataException.class, () -> hostVariableAPI.save(
                variable(id, siteA, uniqueKey("changed"), "changedValue", editorOfBothSites), editorOfBothSites,
                false));

        assertEquals(before, storedRow(id).orElseThrow());
    }

    /**
     * Given an id that matches no variable at all, when it is saved on Site A, then the save is refused with the same
     * exception and message as an id that belongs to a variable of another Site.
     */
    @Test
    public void save_withUnknownId_isRejectedWithTheSameMessageAsAnIdOfAnotherSite() throws Exception {
        final String otherSiteId = createVariable(siteB, uniqueKey("msg"), "valueOnB");

        final DotDataException otherSite = assertThrows(DotDataException.class, () -> hostVariableAPI.save(
                variable(otherSiteId, siteA, uniqueKey("changed"), "changedValue", editorOfSiteA), editorOfSiteA,
                false));
        final DotDataException unknown = assertThrows(DotDataException.class, () -> hostVariableAPI.save(
                variable(UUIDGenerator.generateUuid(), siteA, uniqueKey("changed"), "changedValue", editorOfSiteA),
                editorOfSiteA, false));

        assertEquals(unknown.getClass(), otherSite.getClass());
        assertEquals(unknown.getMessage(), otherSite.getMessage());
    }

    /**
     * Given a variable of Site A, when a user who can edit Site A saves it by id with a new key and value, then the
     * stored row is updated, stays on Site A, and the next read returns the new values.
     */
    @Test
    public void save_withIdOfOwnSiteVariable_updatesItAndKeepsItsSite() throws Exception {
        final String id = createVariable(siteA, uniqueKey("own"), "originalValue");
        final String newKey = uniqueKey("updated");

        hostVariableAPI.save(variable(id, siteA, newKey, "updatedValue", editorOfSiteA), editorOfSiteA, false);

        final Map<String, Object> row = storedRow(id).orElseThrow();
        assertEquals(siteA.getIdentifier(), row.get("host_id"));
        assertEquals(newKey, row.get("variable_key"));
        assertEquals("updatedValue", row.get("variable_value"));
        final HostVariable read = findById(siteA, id).orElseThrow();
        assertEquals("updatedValue", read.getValue());
    }

    /**
     * Given no id, when a user who can edit Site A saves a variable on Site A, then a new variable is stored on Site
     * A.
     */
    @Test
    public void save_withoutId_createsTheVariableOnTheGivenSite() throws Exception {
        final String key = uniqueKey("new");

        hostVariableAPI.save(variable("", siteA, key, "newValue", editorOfSiteA), editorOfSiteA, false);

        assertTrue(keysOf(siteA).contains(key));
        assertEquals(siteA.getIdentifier(), storedRowByKey(key).orElseThrow().get("host_id"));
    }

    /**
     * Given a variable of Site B, when a user who can only read Site B saves it, then the save is refused with a
     * {@link DotSecurityException} and the stored row is unchanged.
     */
    @Test
    public void save_withoutEditPermissionOnTheSite_throwsSecurityException() throws Exception {
        final String id = createVariable(siteB, uniqueKey("ro"), "valueOnB");
        final Map<String, Object> before = storedRow(id).orElseThrow();

        assertThrows(DotSecurityException.class, () -> hostVariableAPI.save(
                variable(id, siteB, uniqueKey("changed"), "changedValue", readerOfBothSites), readerOfBothSites,
                false));

        assertEquals(before, storedRow(id).orElseThrow());
    }

    /**
     * Given a variable of Site B, when a user who can edit Site A saves Site A's list with a change to one of Site A's
     * own variables and with Site B's variable id in it, then the save is refused and neither Site B's variable nor
     * Site A's own variable changes.
     */
    @Test
    public void saveList_withIdOfAnotherSiteVariable_isRejectedAndNothingChanges() throws Exception {
        final String ownKey = uniqueKey("listown");
        final String ownId = createVariable(siteA, ownKey, "ownValue");
        final String otherId = createVariable(siteB, uniqueKey("listother"), "valueOnB");
        final Map<String, Object> ownBefore = storedRow(ownId).orElseThrow();
        final Map<String, Object> otherBefore = storedRow(otherId).orElseThrow();
        // The list save processes entries in the order Collectors.toMap gives their keys: pick a key for the refused
        // entry that comes after the own change, so the own change is written first and only the rollback undoes it
        List<HostVariable> list;
        String refusedKey;
        do {
            refusedKey = uniqueKey("listchanged");
            list = variablesOf(siteA).stream()
                    .filter(variable -> !ownId.equals(variable.getId())).collect(Collectors.toList());
            list.add(variable(ownId, siteA, ownKey, "ownValueChanged", editorOfSiteA));
            list.add(variable(otherId, siteA, refusedKey, "changedValue", editorOfSiteA));
        } while (!processedBefore(list, ownKey, refusedKey));
        final List<HostVariable> toSave = list;

        assertThrows(DotDataException.class,
                () -> hostVariableAPI.save(toSave, siteA.getIdentifier(), editorOfSiteA, false));

        assertEquals(otherBefore, storedRow(otherId).orElseThrow());
        assertEquals(ownBefore, storedRow(ownId).orElseThrow());
    }

    /**
     * Given a new variable (no id) that names Site B as its Site, when it is saved as part of Site A's list, then it
     * is stored on Site A and Site B gets no new variable.
     */
    @Test
    public void saveList_newVariable_isStoredOnTheTargetSite() throws Exception {
        final String key = uniqueKey("target");
        final List<HostVariable> list = new ArrayList<>(variablesOf(siteA));
        list.add(variable("", siteB, key, "newValue", systemUser));

        hostVariableAPI.save(list, siteA.getIdentifier(), systemUser, false);

        assertEquals(siteA.getIdentifier(), storedRowByKey(key).orElseThrow().get("host_id"));
        assertFalse(keysOf(siteB).contains(key));
    }

    /**
     * Given two variables of a new Site, when its list is saved with the first one changed by id, the second one left
     * out and a new one without id, then the first is updated, the second is removed and the new one is created, all
     * on that Site.
     */
    @Test
    public void saveList_withOwnIdsAndNewVariables_updatesCreatesAndRemoves() throws Exception {
        final Host site = newSite();
        final String keptKey = uniqueKey("kept");
        final String keptId = createVariable(site, keptKey, "keptValue");
        final String removedId = createVariable(site, uniqueKey("removed"), "removedValue");
        final String newKey = uniqueKey("added");

        final List<HostVariable> saved = hostVariableAPI.save(List.of(
                variable(keptId, site, keptKey, "keptValueChanged", systemUser),
                variable("", site, newKey, "addedValue", systemUser)), site.getIdentifier(), systemUser, false);

        assertEquals(2, saved.size());
        assertEquals("keptValueChanged", storedRow(keptId).orElseThrow().get("variable_value"));
        assertEquals(site.getIdentifier(), storedRow(keptId).orElseThrow().get("host_id"));
        assertFalse(storedRow(removedId).isPresent());
        assertEquals(site.getIdentifier(), storedRowByKey(newKey).orElseThrow().get("host_id"));
    }

    /**
     * Given a variable of Site B, when Site A's list is saved with an entry that carries that variable's id but the key
     * of one of Site A's own variables, then the entry is matched by key: Site A's variable is updated and Site B's
     * variable does not change.
     */
    @Test
    public void saveList_withIdOfAnotherSiteVariableAndAnOwnKey_updatesOnlyTheOwnVariable() throws Exception {
        final String ownKey = uniqueKey("keymatch");
        final String ownId = createVariable(siteA, ownKey, "ownValue");
        final String otherId = createVariable(siteB, uniqueKey("keyother"), "valueOnB");
        final Map<String, Object> otherBefore = storedRow(otherId).orElseThrow();
        final List<HostVariable> list = variablesOf(siteA).stream()
                .filter(variable -> !ownId.equals(variable.getId())).collect(Collectors.toList());
        list.add(variable(otherId, siteA, ownKey, "ownValueChanged", editorOfSiteA));

        hostVariableAPI.save(list, siteA.getIdentifier(), editorOfSiteA, false);

        assertEquals("ownValueChanged", storedRow(ownId).orElseThrow().get("variable_value"));
        assertEquals(siteA.getIdentifier(), storedRow(ownId).orElseThrow().get("host_id"));
        assertEquals(otherBefore, storedRow(otherId).orElseThrow());
    }

    /**
     * Given an id that matches no variable at all, when Site A's list is saved with it and a new key, then the save is
     * refused with the same exception and message as a list that carries an id of another Site's variable, and the
     * new key is not stored.
     */
    @Test
    public void saveList_withUnknownId_isRejectedWithTheSameMessageAsAnIdOfAnotherSite() throws Exception {
        final String otherSiteId = createVariable(siteB, uniqueKey("listmsg"), "valueOnB");
        final String unknownKey = uniqueKey("listunknown");
        final List<HostVariable> withOtherSiteId = new ArrayList<>(variablesOf(siteA));
        withOtherSiteId.add(variable(otherSiteId, siteA, uniqueKey("listchanged"), "changedValue", editorOfSiteA));
        final List<HostVariable> withUnknownId = new ArrayList<>(variablesOf(siteA));
        withUnknownId.add(variable(UUIDGenerator.generateUuid(), siteA, unknownKey, "changedValue", editorOfSiteA));

        final DotDataException otherSite = assertThrows(DotDataException.class,
                () -> hostVariableAPI.save(withOtherSiteId, siteA.getIdentifier(), editorOfSiteA, false));
        final DotDataException unknown = assertThrows(DotDataException.class,
                () -> hostVariableAPI.save(withUnknownId, siteA.getIdentifier(), editorOfSiteA, false));

        assertEquals(unknown.getClass(), otherSite.getClass());
        assertEquals(unknown.getMessage(), otherSite.getMessage());
        assertFalse(storedRowByKey(unknownKey).isPresent());
    }

    /**
     * Given a variable of Site A, when a user who can only read Site A saves Site A's list with that variable changed,
     * then the save is refused with a {@link DotSecurityException} and the stored row is unchanged.
     */
    @Test
    public void saveList_withoutEditPermissionOnTheSite_throwsSecurityExceptionAndNothingChanges() throws Exception {
        final String key = uniqueKey("listro");
        final String id = createVariable(siteA, key, "storedValue");
        final Map<String, Object> before = storedRow(id).orElseThrow();
        final List<HostVariable> list = variablesOf(siteA).stream()
                .filter(variable -> !id.equals(variable.getId())).collect(Collectors.toList());
        list.add(variable(id, siteA, key, "changedValue", readerOfBothSites));

        assertThrows(DotSecurityException.class,
                () -> hostVariableAPI.save(list, siteA.getIdentifier(), readerOfBothSites, false));

        assertEquals(before, storedRow(id).orElseThrow());
    }

    /**
     * Given Site A's variables are cached, when a caller changes the objects a read returned without saving them,
     * then the next read still returns the stored values.
     */
    @Test
    public void getVariablesForHost_changesToReturnedVariables_doNotAffectLaterReads() throws Exception {
        final String id = createVariable(siteA, uniqueKey("read"), "storedValue");
        variablesOf(siteA);
        final HostVariable fromCache = findById(siteA, id).orElseThrow();

        fromCache.setValue("changedInMemory");
        fromCache.setName("changedInMemory");

        final HostVariable next = findById(siteA, id).orElseThrow();
        assertEquals("storedValue", next.getValue());
        assertEquals("storedValue", next.getName());
    }

    /**
     * Given a user who can only read Site A and Site A's variables are cached, when the user changes a variable read
     * from Site A and its save is refused, then the next read, by any user, returns the stored value.
     */
    @Test
    public void save_refusedAfterChangingAReadVariable_laterReadsReturnTheStoredValue() throws Exception {
        final String id = createVariable(siteA, uniqueKey("refused"), "storedValue");
        variablesOf(siteA);
        final HostVariable read = hostVariableAPI.getVariablesForHost(siteA.getIdentifier(), readerOfBothSites,
                false).stream().filter(variable -> id.equals(variable.getId())).findFirst().orElseThrow();

        read.setValue("changedValue");
        read.setLastModifierId(readerOfBothSites.getUserId());
        assertThrows(DotSecurityException.class, () -> hostVariableAPI.save(read, readerOfBothSites, false));

        assertEquals("storedValue", findById(siteA, id).orElseThrow().getValue());
        assertEquals("storedValue", storedRow(id).orElseThrow().get("variable_value"));
    }

    /**
     * Creates a Site and records it for cleanup.
     *
     * @return The new Site.
     */
    private static Host newSite() {
        final Host site = new SiteDataGen().nextPersisted();
        sites.add(site);
        return site;
    }

    /**
     * Creates a user with no rights of its own and records it for cleanup.
     *
     * @return The new user.
     */
    private static User newUser() {
        final User user = new UserDataGen().nextPersisted();
        users.add(user);
        return user;
    }

    /**
     * Grants the user's personal role an individual permission bitmask on a Site.
     *
     * @param site        The Site to grant the permission on.
     * @param user        The user whose personal role receives the permission.
     * @param permissions The permission bitmask, e.g. {@code PERMISSION_READ | PERMISSION_EDIT}.
     */
    private static void grant(final Host site, final User user, final int permissions) throws Exception {
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId();
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                site.getPermissionId(), roleId, permissions, true), site, systemUser, false);
    }

    /**
     * Builds an unsaved Site Variable.
     *
     * @param id       The variable id, or an empty string for a new variable.
     * @param site     The Site the variable is saved on.
     * @param key      The variable key.
     * @param value    The variable value, also used as its name.
     * @param modifier The user recorded as last modifier.
     *
     * @return The variable.
     */
    private static HostVariable variable(final String id, final Host site, final String key, final String value,
            final User modifier) {
        final HostVariable variable = new HostVariable();
        variable.setId(id);
        variable.setHostId(site.getIdentifier());
        variable.setName(value);
        variable.setKey(key);
        variable.setValue(value);
        variable.setLastModifierId(modifier.getUserId());
        variable.setLastModDate(new Date());
        return variable;
    }

    /**
     * Creates a Site Variable as the system user and returns its stored id.
     *
     * @param site  The Site the variable belongs to.
     * @param key   The variable key.
     * @param value The variable value, also used as its name.
     *
     * @return The id of the stored row.
     */
    private static String createVariable(final Host site, final String key, final String value) throws Exception {
        hostVariableAPI.save(variable("", site, key, value, systemUser), systemUser, false);
        final Optional<Map<String, Object>> row = storedRowByKey(key);
        assertTrue("The variable was not stored", row.isPresent());
        assertNotNull(row.get().get("id"));
        return row.get().get("id").toString();
    }

    /**
     * Reads a Site's variables through the API as the system user.
     *
     * @param site The Site.
     *
     * @return The variables the API returns.
     */
    private static List<HostVariable> variablesOf(final Host site) throws Exception {
        return hostVariableAPI.getVariablesForHost(site.getIdentifier(), systemUser, false);
    }

    /**
     * Finds one of a Site's variables by id, through the API as the system user.
     *
     * @param site The Site.
     * @param id   The variable id.
     *
     * @return The variable, if the Site lists it.
     */
    private static Optional<HostVariable> findById(final Host site, final String id) throws Exception {
        return variablesOf(site).stream().filter(variable -> id.equals(variable.getId())).findFirst();
    }

    /**
     * Returns the keys of a Site's variables, through the API as the system user.
     */
    private static List<String> keysOf(final Host site) throws Exception {
        return variablesOf(site).stream().map(HostVariable::getKey).collect(Collectors.toList());
    }

    /**
     * Returns the ids of a Site's variables, through the API as the system user.
     */
    private static List<String> idsOf(final Host site) throws Exception {
        return variablesOf(site).stream().map(HostVariable::getId).collect(Collectors.toList());
    }

    /**
     * Reads a variable's row straight from the database, not from any cache.
     *
     * @param id The variable id.
     *
     * @return The row (id, host, name, key, value, last modifier), if it exists.
     */
    private static Optional<Map<String, Object>> storedRow(final String id) throws DotDataException {
        return firstRow(new DotConnect().setSQL("SELECT id, host_id, variable_name, variable_key, variable_value, "
                + "user_id FROM host_variable WHERE id = ?").addParam(id).loadObjectResults());
    }

    /**
     * Reads a variable's row straight from the database by its key, not from any cache.
     *
     * @param key The variable key (unique across the tests of this class).
     *
     * @return The row, if it exists.
     */
    private static Optional<Map<String, Object>> storedRowByKey(final String key) throws DotDataException {
        return firstRow(new DotConnect().setSQL("SELECT id, host_id, variable_name, variable_key, variable_value, "
                + "user_id FROM host_variable WHERE variable_key = ?").addParam(key).loadObjectResults());
    }

    /**
     * Tells whether the list save processes the entry with the first key before the entry with the second key. The
     * list save removes duplicate keys with {@code Collectors.toMap} and processes the entries in that map's order.
     *
     * @param list   The variables passed to the list save.
     * @param first  The key expected first.
     * @param second The key expected later.
     *
     * @return {@code true} when the first key comes before the second.
     */
    private static boolean processedBefore(final List<HostVariable> list, final String first, final String second) {
        final List<String> order = new ArrayList<>(list.stream()
                .collect(Collectors.toMap(HostVariable::getKey, HostVariable::getKey, (existing, replacement) -> existing))
                .values());
        return order.indexOf(first) < order.indexOf(second);
    }

    /**
     * Returns the first row of a result, if any.
     */
    private static Optional<Map<String, Object>> firstRow(final List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Returns a key that no other test uses: letters and digits only, as the Site Variable screens require.
     *
     * @param prefix A readable prefix.
     *
     * @return The key.
     */
    private static String uniqueKey(final String prefix) {
        return prefix + UUIDGenerator.generateUuid().replace("-", "");
    }

}
