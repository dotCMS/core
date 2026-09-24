package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.LayoutNameAlreadyExistsException;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Pure logic behind the {@code /v1/layouts} endpoints: mapping a stored {@link Layout} to the
 * view the Tools portlet consumes, resolving tool titles, and validating the inputs of the
 * writes. Holds no state beyond the APIs it reads from, so it is unit-testable with mocks.
 *
 * @author hassandotcms
 */
public class LayoutHelper {

    /** Prefix of the message key that holds a portlet's localized title. */
    static final String TITLE_KEY_PREFIX = "com.dotcms.repackage.javax.portlet.title.";

    /** Column limit of {@code cms_layout.layout_name} and {@code cms_layout.description}. */
    static final int MAX_LENGTH = 255;

    private static final String NAME_INIT_PARAM = "name";

    /** PostgreSQL SQLSTATE for a unique-constraint violation. */
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    /** Name of the unique constraint on {@code cms_layout.layout_name}. */
    private static final String NAME_CONSTRAINT = "cms_layout_name_parent";

    private final LayoutAPI layoutApi;
    private final PortletAPI portletApi;
    private final BiFunction<User, String, String> titleResolver;

    /**
     * Production wiring: titles come from {@link LanguageUtil#get(User, String)}.
     *
     * @param layoutApi  the layout API
     * @param portletApi the portlet API, used for the registered-name fallback of titles and for
     *                   tool validation
     */
    public LayoutHelper(final LayoutAPI layoutApi, final PortletAPI portletApi) {
        this(layoutApi, portletApi, (user, key) -> Try.of(() -> LanguageUtil.get(user, key)).getOrElse(key));
    }

    /**
     * Wiring with an injectable title lookup, for tests that run without a message bundle.
     *
     * @param layoutApi     the layout API
     * @param portletApi    the portlet API
     * @param titleResolver resolves a translation key for a user; must return the key itself on a
     *                      miss, as {@code LanguageUtil} does
     */
    public LayoutHelper(final LayoutAPI layoutApi, final PortletAPI portletApi,
                        final BiFunction<User, String, String> titleResolver) {
        this.layoutApi = layoutApi;
        this.portletApi = portletApi;
        this.titleResolver = titleResolver;
    }

    // ==================== read side ====================

    /**
     * Resolves a tool's title for the caller's language: the translation if one exists, else the
     * name the tool was registered with, else its id. The raw translation key is never returned,
     * so the same rule as the tools catalog applies and the two lists never disagree.
     *
     * @param user      the caller, whose language decides the translation
     * @param portletId the tool id
     * @return a displayable title
     */
    public String title(final User user, final String portletId) {
        final String key = TITLE_KEY_PREFIX + portletId;
        final String translated = titleResolver.apply(user, key);
        if (UtilMethods.isSet(translated) && !key.equals(translated)) {
            return translated;
        }
        final Portlet portlet = portletApi.findPortlet(portletId);
        final Map<String, String> initParams = null == portlet ? null : portlet.getInitParams();
        final String registeredName = null == initParams ? null : initParams.get(NAME_INIT_PARAM);
        return UtilMethods.isSet(registeredName) ? registeredName : portletId;
    }

    /**
     * Maps a stored section to its view: the description becomes {@code icon} and every tool id
     * gets a title in the same position.
     *
     * @param layout the stored section
     * @param user   the caller, for title localization
     * @return the view
     */
    public SectionView toView(final Layout layout, final User user) {
        final List<String> portletIds = List.copyOf(layout.getPortletIds());
        final List<String> titles = portletIds.stream()
                .map(id -> title(user, id))
                .collect(Collectors.toList());
        return new SectionView(layout.getId(), layout.getName(),
                null == layout.getDescription() ? "" : layout.getDescription(),
                layout.getTabOrder(), portletIds, titles);
    }

    /**
     * Maps sections to views, preserving the given order.
     *
     * @param layouts the stored sections, already in navigation order
     * @param user    the caller, for title localization
     * @return one view per section, same order
     */
    public List<SectionView> toViews(final List<Layout> layouts, final User user) {
        return layouts.stream().map(layout -> toView(layout, user)).collect(Collectors.toList());
    }

    // ==================== section name, icon, position ====================

    /**
     * Trims and validates a section name: present, at most {@value #MAX_LENGTH} characters.
     * Uniqueness is enforced when the section is saved.
     *
     * @param name the raw name
     * @return the trimmed name
     * @throws BadRequestException if the name is blank or too long
     */
    public String validateName(final String name) {
        final String trimmed = null == name ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Section name is required");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new BadRequestException("Section name must be at most " + MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Validates an icon: at most {@value #MAX_LENGTH} characters; absent means empty.
     *
     * @param icon the raw icon
     * @return the icon to store, never null
     * @throws BadRequestException if the icon is too long
     */
    public String validateIcon(final String icon) {
        final String value = null == icon ? "" : icon;
        if (value.length() > MAX_LENGTH) {
            throw new BadRequestException("Section icon must be at most " + MAX_LENGTH + " characters");
        }
        return value;
    }

    /**
     * Position for a new section: right after the current last one, or 1 when there is none.
     *
     * @param existing every existing section
     * @return the position to store
     */
    public int nextTabOrder(final List<Layout> existing) {
        return existing.stream().mapToInt(Layout::getTabOrder).max().orElse(0) + 1;
    }

    /**
     * Loads a section by id straight from the database.
     *
     * @param layoutId the section id
     * @return the section, without its tools populated
     * @throws DoesNotExistException if no section has that id
     * @throws DotDataException      if the lookup fails
     */
    public Layout findSectionOrThrow(final String layoutId) throws DotDataException {
        final Layout layout = UtilMethods.isSet(layoutId) ? layoutApi.findLayout(layoutId) : null;
        if (null == layout || !UtilMethods.isSet(layout.getId())) {
            throw new DoesNotExistException("Navigation section '" + layoutId + "' does not exist");
        }
        return layout;
    }

    /**
     * Whether the section is the product's Getting Started section: the fixed id, or the fixed
     * name when no section holds the fixed id (installs that predate it).
     *
     * @param layout the section
     * @return true for Getting Started
     * @throws DotDataException if the lookup fails
     */
    public boolean isGettingStarted(final Layout layout) throws DotDataException {
        if (LayoutAPI.GETTING_STARTED_LAYOUT_ID.equals(layout.getId())) {
            return true;
        }
        if (LayoutAPI.GETTING_STARTED_LAYOUT_NAME.equals(layout.getName())) {
            final Layout fixed = layoutApi.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            return null == fixed || !UtilMethods.isSet(fixed.getId());
        }
        return false;
    }

    /**
     * Whether the section is a Getting Started that the product can only recognise by its name:
     * an install that predates the fixed id, where no section holds that id. Renaming such a
     * section would make it unrecognisable and the next toggle would create a second one.
     *
     * @param layout the section
     * @return true when Getting Started is identified by name only
     * @throws DotDataException if the lookup fails
     */
    public boolean isLegacyGettingStarted(final Layout layout) throws DotDataException {
        return !LayoutAPI.GETTING_STARTED_LAYOUT_ID.equals(layout.getId()) && isGettingStarted(layout);
    }

    /**
     * Saves the section. A duplicate name caught by the database's uniqueness rule, when two
     * writes race past the application check, is reported as the same duplicate-name error the
     * application check raises, so the caller answers 400 rather than 500.
     *
     * @param layout the section to save
     * @throws LayoutNameAlreadyExistsException on a duplicate name, from either check
     * @throws DotDataException                 on any other failure
     */
    public void saveOrDuplicate(final Layout layout) throws DotDataException {
        try {
            layoutApi.saveLayout(layout);
        } catch (final DotDataException | DotRuntimeException e) {
            if (isUniqueNameViolation(e)) {
                throw new LayoutNameAlreadyExistsException("Layout with name: " + layout.getName()
                        + " already exists in the system, cannot save a new layout using the same name");
            }
            throw e;
        }
    }

    private static boolean isUniqueNameViolation(final Throwable failure) {
        for (Throwable t = failure; null != t; t = t.getCause()) {
            if (t instanceof SQLException && UNIQUE_VIOLATION_SQL_STATE.equals(((SQLException) t).getSQLState())) {
                return true;
            }
            if (null != t.getMessage() && t.getMessage().contains(NAME_CONSTRAINT)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    // ==================== tool list and reorder validation ====================

    /**
     * Validates a full ordered tool list: every id names a registered portlet the product allows
     * in a section, and appears once. Registered tools the catalog hides (the old Languages tool)
     * are accepted: this is a placement rule, not a catalog rule. An empty list is accepted.
     *
     * @param portletIds the ordered tool ids
     * @return the same list
     * @throws BadRequestException naming the first offending id
     */
    public List<String> validateToolIds(final List<String> portletIds) {
        final Set<String> seen = new HashSet<>();
        for (final String portletId : portletIds) {
            if (!UtilMethods.isSet(portletId)) {
                throw new BadRequestException("Tool ids must not be blank");
            }
            if (!seen.add(portletId)) {
                throw new BadRequestException("Tool '" + portletId + "' appears more than once");
            }
            final Portlet portlet = portletApi.findPortlet(portletId);
            if (null == portlet || !UtilMethods.isSet(portlet.getPortletId())) {
                throw new BadRequestException("Tool '" + portletId + "' is not a registered portlet");
            }
            if (!portletApi.canAddPortletToLayout(portletId)) {
                throw new BadRequestException("Tool '" + portletId + "' cannot be placed in a navigation section");
            }
        }
        return portletIds;
    }

    /**
     * Validates a full reorder list: it must contain every existing section id exactly once.
     *
     * @param sent     the ids in the new order
     * @param existing every existing section
     * @throws BadRequestException naming the missing, unknown or repeated id
     */
    public void validateOrder(final List<String> sent, final List<Layout> existing) {
        final Set<String> known = existing.stream().map(Layout::getId).collect(Collectors.toSet());
        final Set<String> seen = new HashSet<>();
        for (final String id : sent) {
            if (!known.contains(id)) {
                throw new BadRequestException("Section '" + id + "' does not exist");
            }
            if (!seen.add(id)) {
                throw new BadRequestException("Section '" + id + "' appears more than once");
            }
        }
        for (final String id : known) {
            if (!seen.contains(id)) {
                throw new BadRequestException("Section '" + id + "' is missing from the order");
            }
        }
    }

    /**
     * Maps a validated reorder list to the positions to write: 1-based index in list order, so
     * positions are strictly increasing and no two sections share one.
     *
     * @param sent the ids in the new order
     * @return position by section id, in list order
     */
    public Map<String, Integer> positions(final List<String> sent) {
        final Map<String, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < sent.size(); i++) {
            positions.put(sent.get(i), i + 1);
        }
        return positions;
    }
}
