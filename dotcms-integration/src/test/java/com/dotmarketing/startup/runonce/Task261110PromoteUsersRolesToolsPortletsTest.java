package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for {@link Task261110PromoteUsersRolesToolsPortlets}: the {@code -beta} menu rows
 * become the promoted {@code users}, {@code roles} and {@code tools} ids, Tools lands right after
 * Roles in every layout that has Roles, rows that already point at {@code users} or {@code roles}
 * are left exactly as they were, and a second run changes nothing.
 * <p>
 * Each test builds its own layouts with direct SQL, so the rows and their order are fully known.
 * The task itself reconciles every layout in the database, the starter's included, which is what a
 * real startup does too.
 */
public class Task261110PromoteUsersRolesToolsPortletsTest {

    private static final String USERS = "users";
    private static final String ROLES = "roles";
    private static final String TOOLS = "tools";
    // the beta ids leave PortletID with this change, so they are spelled out here
    private static final String USERS_BETA = "users-beta";
    private static final String ROLES_BETA = "roles-beta";
    private static final String TOOLS_BETA = "tools-beta";

    private static final List<String> layoutsToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Removes every layout these tests created, rows first. A failure on one layout does not
     * strand the others.
     */
    @AfterClass
    public static void cleanUp() {
        for (final String layoutId : layoutsToClean) {
            try {
                new DotConnect()
                        .setSQL("delete from cms_layouts_portlets where layout_id = ?")
                        .addParam(layoutId)
                        .loadResult();
                new DotConnect()
                        .setSQL("delete from cms_layout where id = ?")
                        .addParam(layoutId)
                        .loadResult();
            } catch (final Exception e) {
                Logger.warn(Task261110PromoteUsersRolesToolsPortletsTest.class,
                        "Unable to delete test layout " + layoutId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout holding Users, Roles and Maintenance, in that order, and no Tools
     * ExpectedResult: Tools sits right after Roles, Maintenance moves down one place, and Users
     * and Roles keep their positions
     */
    @Test
    public void executeUpgrade_insertsToolsRightAfterRoles_andShiftsTheRest() throws Exception {
        final String layoutId = newLayout("insert");
        insert(layoutId, USERS, 1);
        insert(layoutId, ROLES, 2);
        insert(layoutId, "maintenance", 3);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put(USERS, 1);
        expected.put(ROLES, 2);
        expected.put(TOOLS, 3);
        expected.put("maintenance", 4);
        assertEquals(expected, portletOrder(layoutId));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout that already holds Roles and Tools, with Tools placed away from Roles
     * ExpectedResult: the layout is skipped: no second Tools row and no position changes
     */
    @Test
    public void executeUpgrade_layoutAlreadyHoldingTools_isSkipped() throws Exception {
        final String layoutId = newLayout("skip");
        insert(layoutId, ROLES, 1);
        insert(layoutId, "maintenance", 2);
        insert(layoutId, TOOLS, 3);
        final Map<String, Integer> before = portletOrder(layoutId);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        assertEquals(before, portletOrder(layoutId));
        assertEquals(1, countInLayout(layoutId, TOOLS));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout that opted into the three beta portlets and holds none of the
     * promoted ids
     * ExpectedResult: each beta row becomes its promoted id at the same position, no beta row is
     * left, and no extra Tools row is inserted because the converted Tools already follows Roles
     */
    @Test
    public void executeUpgrade_convertsBetaRowsToThePromotedIds() throws Exception {
        final String layoutId = newLayout("convert");
        insert(layoutId, USERS_BETA, 1);
        insert(layoutId, ROLES_BETA, 2);
        insert(layoutId, TOOLS_BETA, 3);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put(USERS, 1);
        expected.put(ROLES, 2);
        expected.put(TOOLS, 3);
        assertEquals(expected, portletOrder(layoutId));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout holding each promoted id and its beta twin side by side, which a
     * blind rename would turn into duplicate rows the unique (layout, portlet) constraint forbids
     * ExpectedResult: the beta rows are deleted, one row per promoted id remains, and the rows that
     * already pointed at {@code users} and {@code roles} are the same rows as before
     */
    @Test
    public void executeUpgrade_betaRowBesideItsPromotedId_isDeleted() throws Exception {
        final String layoutId = newLayout("dedupe");
        final String usersRowId = insert(layoutId, USERS, 1);
        insert(layoutId, USERS_BETA, 2);
        final String rolesRowId = insert(layoutId, ROLES, 3);
        insert(layoutId, ROLES_BETA, 4);
        insert(layoutId, TOOLS, 5);
        insert(layoutId, TOOLS_BETA, 6);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        assertEquals(0, countInLayout(layoutId, USERS_BETA));
        assertEquals(0, countInLayout(layoutId, ROLES_BETA));
        assertEquals(0, countInLayout(layoutId, TOOLS_BETA));
        assertEquals(1, countInLayout(layoutId, USERS));
        assertEquals(1, countInLayout(layoutId, ROLES));
        assertEquals(1, countInLayout(layoutId, TOOLS));
        assertEquals(USERS, portletOfRow(usersRowId));
        assertEquals(ROLES, portletOfRow(rolesRowId));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout holding the shipped {@code users} and {@code roles} rows, which every
     * customer menu already has
     * ExpectedResult: those rows keep their id, portlet id and position, so customers see the
     * Angular portlets in the same places with no data change
     */
    @Test
    public void executeUpgrade_leavesUsersAndRolesRowsUntouched() throws Exception {
        final String layoutId = newLayout("untouched");
        final String usersRowId = insert(layoutId, USERS, 4);
        final String rolesRowId = insert(layoutId, ROLES, 7);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        assertEquals(USERS, portletOfRow(usersRowId));
        assertEquals(4, orderOfRow(usersRowId));
        assertEquals(ROLES, portletOfRow(rolesRowId));
        assertEquals(7, orderOfRow(rolesRowId));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout holding only the Tools beta portlet, without Roles
     * ExpectedResult: the row becomes {@code tools}; nothing is inserted, since there is no Roles to
     * follow
     */
    @Test
    public void executeUpgrade_toolsBetaWithoutRoles_isConvertedOnly() throws Exception {
        final String layoutId = newLayout("tools-only");
        insert(layoutId, TOOLS_BETA, 1);

        new Task261110PromoteUsersRolesToolsPortlets().executeUpgrade();

        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put(TOOLS, 1);
        assertEquals(expected, portletOrder(layoutId));
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#forceRun()}
     * Given Scenario: every layout has been reconciled, then a layout with Roles and no Tools appears
     * ExpectedResult: the task needs to run
     */
    @Test
    public void forceRun_isTrueWhenALayoutHasRolesWithoutTools() throws Exception {
        final Task261110PromoteUsersRolesToolsPortlets task = new Task261110PromoteUsersRolesToolsPortlets();
        task.executeUpgrade();

        final String layoutId = newLayout("pending-tools");
        insert(layoutId, ROLES, 1);

        assertTrue(task.forceRun());
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#forceRun()}
     * Given Scenario: every layout has been reconciled, then a beta row appears in a layout that
     * otherwise needs nothing
     * ExpectedResult: the task needs to run
     */
    @Test
    public void forceRun_isTrueWhenABetaRowExists() throws Exception {
        final Task261110PromoteUsersRolesToolsPortlets task = new Task261110PromoteUsersRolesToolsPortlets();
        task.executeUpgrade();

        final String layoutId = newLayout("pending-beta");
        insert(layoutId, USERS_BETA, 1);

        assertTrue(task.forceRun());
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#forceRun()} and
     * {@link Task261110PromoteUsersRolesToolsPortlets#executeUpgrade()}
     * Given Scenario: a layout with a beta row and a layout with Roles but no Tools; the task runs,
     * then runs again
     * ExpectedResult: the task reports work before the first run and none after it, and the second
     * run leaves every row as the first one did
     */
    @Test
    public void runningTheTaskTwice_isANoOpTheSecondTime() throws Exception {
        final String betaLayoutId = newLayout("twice-beta");
        insert(betaLayoutId, USERS_BETA, 1);
        final String rolesLayoutId = newLayout("twice-roles");
        insert(rolesLayoutId, ROLES, 1);
        insert(rolesLayoutId, "maintenance", 2);
        final Task261110PromoteUsersRolesToolsPortlets task = new Task261110PromoteUsersRolesToolsPortlets();

        assertTrue(task.forceRun());
        task.executeUpgrade();
        final Map<String, Integer> betaAfterFirstRun = portletOrder(betaLayoutId);
        final Map<String, Integer> rolesAfterFirstRun = portletOrder(rolesLayoutId);

        assertFalse(task.forceRun());
        task.executeUpgrade();

        assertEquals(betaAfterFirstRun, portletOrder(betaLayoutId));
        assertEquals(rolesAfterFirstRun, portletOrder(rolesLayoutId));
        assertEquals(1, countInLayout(rolesLayoutId, TOOLS));
        assertFalse(task.forceRun());
    }

    /**
     * Method to test: {@link Task261110PromoteUsersRolesToolsPortlets#addToolsAfterRoles(String, int)}
     * Given Scenario: two nodes start together and both read this layout as "Roles without Tools";
     * the other node has already inserted Tools when this one gets to the layout
     * ExpectedResult: this node does nothing: no error (which would stop its startup), no second
     * Tools row and no position changes
     */
    @Test
    public void addToolsAfterRoles_toolsAddedSinceTheRead_isANoOp() throws Exception {
        final String layoutId = newLayout("race");
        insert(layoutId, ROLES, 1);
        insert(layoutId, TOOLS, 2);
        insert(layoutId, "maintenance", 3);
        final Map<String, Integer> before = portletOrder(layoutId);

        final boolean inserted = new Task261110PromoteUsersRolesToolsPortlets().addToolsAfterRoles(layoutId, 1);

        assertFalse(inserted);
        assertEquals(before, portletOrder(layoutId));
        assertEquals(1, countInLayout(layoutId, TOOLS));
    }

    /**
     * Creates an empty layout for one test and records it for clean-up.
     *
     * @param label a short label that makes the layout name readable in the database
     * @return the new layout's id
     * @throws DotDataException if the layout cannot be inserted
     */
    private static String newLayout(final String label) throws DotDataException {
        final String layoutId = UUIDUtil.uuid();
        new DotConnect()
                .setSQL("insert into cms_layout (id, layout_name, description, tab_order) values (?, ?, ?, ?)")
                .addParam(layoutId)
                .addParam("Task261110 " + label)
                .addParam("Layout used by Task261110PromoteUsersRolesToolsPortletsTest")
                .addParam(0)
                .loadResult();
        layoutsToClean.add(layoutId);
        return layoutId;
    }

    /**
     * Adds a portlet row to a layout at the given position.
     *
     * @param layoutId  the layout that receives the row
     * @param portletId the portlet the row points at
     * @param order     the row's position in the layout
     * @return the new row's id
     * @throws DotDataException if the row cannot be inserted
     */
    private static String insert(final String layoutId, final String portletId, final int order)
            throws DotDataException {
        final String rowId = UUIDUtil.uuid();
        new DotConnect()
                .setSQL("insert into cms_layouts_portlets (id, layout_id, portlet_id, portlet_order) values (?, ?, ?, ?)")
                .addParam(rowId)
                .addParam(layoutId)
                .addParam(portletId)
                .addParam(order)
                .loadResult();
        return rowId;
    }

    /**
     * Reads a layout's portlets and their positions, ordered by position.
     *
     * @param layoutId the layout to read
     * @return each portlet id mapped to its position, in position order
     * @throws DotDataException if the rows cannot be read
     */
    private static Map<String, Integer> portletOrder(final String layoutId) throws DotDataException {
        final Map<String, Integer> order = new LinkedHashMap<>();
        for (final Map<String, Object> row : new DotConnect()
                .setSQL("select portlet_id, portlet_order from cms_layouts_portlets where layout_id = ? "
                        + "order by portlet_order")
                .addParam(layoutId)
                .loadObjectResults()) {
            order.put(String.valueOf(row.get("portlet_id")),
                    Integer.parseInt(String.valueOf(row.get("portlet_order"))));
        }
        return order;
    }

    /**
     * Counts the rows of a layout that point at the given portlet.
     *
     * @param layoutId  the layout to read
     * @param portletId the portlet to count
     * @return how many rows of the layout point at the portlet
     * @throws DotDataException if the rows cannot be read
     */
    private static int countInLayout(final String layoutId, final String portletId) throws DotDataException {
        return new DotConnect()
                .setSQL("select count(*) as count from cms_layouts_portlets where layout_id = ? and portlet_id = ?")
                .addParam(layoutId)
                .addParam(portletId)
                .getInt("count");
    }

    /**
     * Reads the portlet a row points at.
     *
     * @param rowId the row's id
     * @return the row's portlet id, or {@code "null"} when the row is gone
     * @throws DotDataException if the row cannot be read
     */
    private static String portletOfRow(final String rowId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select portlet_id from cms_layouts_portlets where id = ?")
                .addParam(rowId)
                .loadObjectResults();
        return rows.isEmpty() ? "null" : String.valueOf(rows.get(0).get("portlet_id"));
    }

    /**
     * Reads a row's position.
     *
     * @param rowId the row's id
     * @return the row's position
     * @throws DotDataException if the row cannot be read
     */
    private static int orderOfRow(final String rowId) throws DotDataException {
        return new DotConnect()
                .setSQL("select portlet_order from cms_layouts_portlets where id = ?")
                .addParam(rowId)
                .getInt("portlet_order");
    }
}
