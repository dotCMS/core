/**
 * Prefer `E2E_ADMIN_USER` / `E2E_ADMIN_PASS` in CI or shared environments so secrets are not
 * committed; defaults match local dotCMS demo credentials.
 */
export const admin1 = {
    username: process.env['E2E_ADMIN_USER'] ?? 'admin@dotcms.com',
    password: process.env['E2E_ADMIN_PASS'] ?? 'admin'
};

/**
 * This is a limited user that can only view the content
 */
export const limited1 = {
    username: 'chris@dotcms.com',
    password: 'chris'
};

/**
 * Combination of a valid user and wrong password
 */
export const wrong1 = {
    username: 'admin@dotcms.com',
    password: 'password'
};

/**
 * Combination of a wrong user and valid password
 */
export const wrong2 = {
    username: 'chris2@dotcms.com',
    password: 'chris'
};
