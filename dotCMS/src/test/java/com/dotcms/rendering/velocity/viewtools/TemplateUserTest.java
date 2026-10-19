package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.model.User;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link TemplateUser} must carry the profile of the source user without its credential
 * material, and must never write back to the source instance.
 */
public class TemplateUserTest {

    private static User sourceUser() {
        final User user = new User("user-1");
        user.setCompanyId("dotcms.org");
        user.setPassword("stored-password-hash");
        user.setPasswordEncrypted(true);
        user.setRememberMeToken("remember-me-token");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmailAddress("ada@example.com");
        user.setLanguageId("en_US");
        user.setTimeZoneId("UTC");
        user.setCreateDate(new Date(1_000L));
        user.setLastLoginDate(new Date(2_000L));
        user.setLastLoginIP("10.0.0.1");
        user.setActive(true);
        user.setModificationDate(new Date(3_000L));
        final Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("department", "research");
        user.setAdditionalInfo(additionalInfo);
        return user;
    }

    /**
     * Method to test: {@link TemplateUser#getPassword()}, {@link TemplateUser#getRememberMeToken()},
     * {@link TemplateUser#getSkinId()}
     * Given scenario: a user with a stored password and a remember-me token
     * Expected result: the template copy returns null for all three accessors
     */
    @Test
    public void credentialAccessorsReturnNull() {
        final TemplateUser templateUser = new TemplateUser(sourceUser());

        assertNull(templateUser.getPassword());
        assertNull(templateUser.getRememberMeToken());
        assertNull(templateUser.getSkinId());
    }

    /**
     * Method to test: {@link TemplateUser#TemplateUser(User)}
     * Given scenario: a user with profile data
     * Expected result: the template copy carries the same profile data
     */
    @Test
    public void profileDataIsPreserved() {
        final User source = sourceUser();
        final TemplateUser templateUser = new TemplateUser(source);

        assertEquals("user-1", templateUser.getUserId());
        assertEquals("dotcms.org", templateUser.getCompanyId());
        assertEquals("Ada", templateUser.getFirstName());
        assertEquals("Lovelace", templateUser.getLastName());
        assertEquals("Ada Lovelace", templateUser.getFullName());
        assertEquals("ada@example.com", templateUser.getEmailAddress());
        assertEquals("en_US", templateUser.getLanguageId());
        assertEquals("UTC", templateUser.getTimeZoneId());
        assertEquals(new Date(1_000L), templateUser.getCreateDate());
        assertEquals(new Date(2_000L), templateUser.getLastLoginDate());
        assertEquals("10.0.0.1", templateUser.getLastLoginIP());
        assertEquals(new Date(3_000L), templateUser.getModificationDate());
        assertTrue(templateUser.isActive());
        assertTrue(templateUser.isPasswordEncrypted());
        assertEquals("research", templateUser.getAdditionalInfo().get("department"));
    }

    /**
     * Method to test: {@link TemplateUser#TemplateUser(User)}
     * Given scenario: the template copy is mutated after creation
     * Expected result: the source user keeps its password, token and profile
     */
    @Test
    public void sourceUserIsNeverModified() {
        final User source = sourceUser();
        source.setModified(false);
        final TemplateUser templateUser = new TemplateUser(source);

        templateUser.setFirstName("Mallory");
        templateUser.setPassword("other");
        templateUser.getAdditionalInfo().put("department", "changed");

        assertEquals("stored-password-hash", source.getPassword());
        assertEquals("remember-me-token", source.getRememberMeToken());
        assertEquals("Ada", source.getFirstName());
        assertEquals("research", source.getAdditionalInfo().get("department"));
        assertFalse(source.isModified());
    }

    /**
     * Method to test: {@link TemplateUser#clone()}
     * Given scenario: a template copy is cloned
     * Expected result: the clone is a distinct TemplateUser that still hides the credentials
     */
    @Test
    public void cloneStaysTemplateFacing() {
        final TemplateUser templateUser = new TemplateUser(sourceUser());

        final Object clone = templateUser.clone();

        assertTrue(clone instanceof TemplateUser);
        assertNotSame(templateUser, clone);
        assertNull(((User) clone).getPassword());
        assertEquals("ada@example.com", ((User) clone).getEmailAddress());
    }

}
