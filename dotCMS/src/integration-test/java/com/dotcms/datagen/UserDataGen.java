package com.dotcms.datagen;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;

public class UserDataGen extends AbstractDataGen<User> {

  @Override
  public User next() {
    User user = new User();
    String key = "Test" + System.currentTimeMillis();
    user.setActive(true);
    user.setFirstName("Test");
    user.setLastName(key);
    user.setEmailAddress(key + "@test.com");
    user.setPassword(key);
    return user;
  }

  @Override
  public User persist(User user) {
    String id = user.getUserId() != null ? user.getUserId() : UUIDUtil.uuid();
    try {
      User user2 = APILocator.getUserAPI().createUser(id, user.getEmailAddress());

      user2 = APILocator.getUserAPI().loadUserById(id);
      user2.setFirstName(user.getFirstName());
      user2.setLastName(user.getLastName());
      user2.setPassword(user.getEmailAddress());
      user2.setActive(true);
      APILocator.getUserAPI().save(user2, APILocator.systemUser(), false);
      return user2;
    } catch (DotDataException | DotSecurityException e) {
      throw new RuntimeException("Unable to persist user.", e);
    }
  }
}
