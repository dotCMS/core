import { faker } from '@faker-js/faker';

import { DotSite } from '@dotcms/dotcms-models';

/**
 * Create a fake Site object.
 *
 * @export
 * @param {Partial<DotSite>} [overrides={}]
 * @return {*}  {DotSite}
 */
export function createFakeSite(overrides: Partial<DotSite> = {}): DotSite {
    return {
        identifier: faker.string.uuid(),
        hostname: faker.internet.domainName(),
        aliases: null,
        archived: faker.datatype.boolean(),
        ...overrides
    } as DotSite;
}
