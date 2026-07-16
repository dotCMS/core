import { DotSite } from '@dotcms/dotcms-models';

/**
 * Mock DotSite for testing purposes
 */
export const mockSiteEntity: DotSite = {
    identifier: 'site-123',
    hostname: 'demo.dotcms.com',
    aliases: null,
    archived: false
};

/**
 * Mock user data for testing purposes
 */
export const mockUserData = {
    name: 'John Doe',
    email: 'john@example.com'
};

/**
 * Alternative mock user data for testing purposes
 */
export const mockUserDataAlt = {
    name: 'Jane Smith',
    email: 'jane@example.com'
};
