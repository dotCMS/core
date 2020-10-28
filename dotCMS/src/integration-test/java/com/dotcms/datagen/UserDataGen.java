package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDataGen extends AbstractDataGen<User> {

    private final long currentTime = System.currentTimeMillis();

    private String id = UUIDUtil.uuid();
    private Boolean active = Boolean.TRUE;
    private String firstName = "testFirstName" + currentTime;
    private String lastName = "testLastName" + currentTime;
    private String emailAddress = "testEmailAddress@" + currentTime + ".com";
    private String password = String.valueOf(currentTime);
    private String skinIdentifier = UUIDGenerator.generateUuid();
    private String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
    private List<Role> roles = new ArrayList<>();

    @SuppressWarnings("unused")
    public UserDataGen id(final String id) {
        this.id = id;
        return this;
    }

    public UserDataGen companyId(final String companyId) {
        this.companyId = companyId;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen skinId(final String skinIdentifier) {
        this.skinIdentifier = skinIdentifier;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen active(final Boolean active) {
        this.active = active;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen firstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen lastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen emailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen password(final String password) {
        this.password = password;
        return this;
    }

    @SuppressWarnings("unused")
    public UserDataGen roles(final Role... roles) {
        this.roles.addAll(Arrays.asList(roles));
        return this;
    }

    @Override
    public User next() {
        final User user = new User();
        user.setUserId(id);
        user.setActive(active);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailAddress(emailAddress);
        user.setPassword(password);
        user.setSkinId(this.skinIdentifier);
        user.setCompanyId(companyId);

        return user;
    }

    @Override
    @WrapInTransaction
    public User persist(User user) {

        try {
            User newUser = APILocator.getUserAPI()
                    .createUser(user.getUserId(), user.getEmailAddress());
            newUser.setFirstName(user.getFirstName());
            newUser.setLastName(user.getLastName());
            newUser.setPassword(user.getPassword());
            newUser.setActive(user.getActive());
            newUser.setSkinId(user.getSkinId());
            newUser.setCompanyId(user.getCompanyId());
            APILocator.getUserAPI().save(newUser, APILocator.systemUser(), false);

            for(final Role role:roles){
                APILocator.getRoleAPI().addRoleToUser(role, newUser);
            }

            return newUser;

        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to persist user.", e);
        }
    }

    /**
     * Creates a new {@link User} instance and persists it in DB
     *
     * @return A new User instance persisted in DB
     */
    @WrapInTransaction
    @Override
    public User nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(final User user) {
        remove(user, true);
    }

    @WrapInTransaction
    public static void remove(final User user, final Boolean failSilently) {
        try {
            APILocator.getUserAPI()
                    .delete(user, APILocator.systemUser(), APILocator.systemUser(), false);
        } catch (Exception e) {
            if (failSilently) {
                Logger.error(ContentTypeDataGen.class, "Unable to remove User.", e);
            } else {
                throw new RuntimeException("Unable to remove User.", e);
            }
        }
    }

}
