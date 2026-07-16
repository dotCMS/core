import { faker } from '@faker-js/faker';

import { DotFolder } from '@dotcms/dotcms-models';

export function createFakeFolder(overrides: Partial<DotFolder> = {}): DotFolder {
    return {
        id: faker.string.uuid(),
        hostName: faker.internet.domainName(),
        path: faker.string.alphanumeric(10),
        addChildrenAllowed: faker.datatype.boolean(),
        ...overrides
    };
}
