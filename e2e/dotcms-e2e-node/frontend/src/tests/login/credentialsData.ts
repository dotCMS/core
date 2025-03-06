/**
 * This is a valid user that can do everything
 */
export const admin1 = {
  username: "admin@dotcms.com",
  password: "admin",
};

/**
 * This is a limited user that can only view the content
 */
export const limited1 = {
  username: "chris@dotcms.com",
  password: "chris",
};

/**
 * Combination of a valid user and wrong password
 */
export const wrong1 = {
  username: "admin@dotcms.com",
  password: "password",
};

/**
 * Combination of a wrong user and valid password
 */
export const wrong2 = {
  username: "chris2@dotcms.com",
  password: "chris",
};
