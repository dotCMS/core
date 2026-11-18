package com.dotcms.company;

import com.liferay.portal.model.Company;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Lists which basic information and branding fields of the default company changed in a save,
 * by their branding names, so the change can be recorded without recording the values.
 */
public final class CompanyBrandingChanges {

    private static final List<Field> FIELDS = List.of(
            new Field("portalURL", Company::getPortalURL),
            new Field("emailAddress", Company::getEmailAddress),
            new Field("mx", Company::getMx),
            new Field("backgroundColor", Company::getSize),
            new Field("primaryColor", Company::getType),
            new Field("secondaryColor", Company::getStreet),
            new Field("backgroundImage", Company::getHomeURL),
            new Field("loginScreenLogo", Company::getCity),
            new Field("navBarLogo", Company::getState));

    private CompanyBrandingChanges() {
    }

    /**
     * Lists the fields whose value differs between the two companies. An empty value and a
     * missing one count as equal.
     *
     * @param before the company before the save
     * @param after the company after the save
     * @return the names of the changed fields, in a fixed order; empty when nothing changed
     */
    public static List<String> changedFields(final Company before, final Company after) {
        final List<String> changed = new ArrayList<>();
        for (final Field field : FIELDS) {
            if (!Objects.equals(valueOf(field, before), valueOf(field, after))) {
                changed.add(field.name);
            }
        }
        return changed;
    }

    private static String valueOf(final Field field, final Company company) {
        final String value = field.getter.apply(company);
        return value == null ? "" : value;
    }

    /** A branding field: its name and how to read it from the company. */
    private static final class Field {
        private final String name;
        private final Function<Company, String> getter;

        private Field(final String name, final Function<Company, String> getter) {
            this.name = name;
            this.getter = getter;
        }
    }
}
