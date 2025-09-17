import { faker } from '@faker-js/faker';

import { SiteEntity } from '@dotcms/dotcms-models';

/**
 * Create a fake Site object.
 *
 * @export
 * @param {Partial<SiteEntity>} [overrides={}]
 * @return {*}  {Site}
 */
export function createFakeSite(overrides: Partial<SiteEntity> = {}): SiteEntity {
    return {
        identifier: faker.string.uuid(),
        hostname: faker.internet.domainName(),
        type: 'site',
        archived: faker.datatype.boolean(),
        ...overrides
    } as SiteEntity;
}
