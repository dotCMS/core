import { faker } from '@faker-js/faker';

import { Site } from '@dotcms/dotcms-js';

/**
 * Create a fake Site object.
 *
 * @export
 * @param {Partial<Site>} [overrides={}]
 * @return {*}  {Site}
 */
export function createFakeSite(overrides: Partial<Site> = {}): Site {
    return {
        identifier: faker.string.uuid(),
        hostname: faker.internet.domainName(),
        type: 'site',
        archived: faker.datatype.boolean(),
        ...overrides
    };
}
