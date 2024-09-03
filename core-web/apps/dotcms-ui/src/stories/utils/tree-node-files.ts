import { faker } from '@faker-js/faker';

import { TreeNode } from 'primeng/api';

export const generateFakeChildrenNode = (): TreeNode => ({
    key: faker.string.uuid(),
    label: faker.lorem.word(),
    icon: 'pi pi-file'
});

export const generateFakeNode = (): TreeNode => ({
    key: faker.string.uuid(),
    label: faker.lorem.word(),
    expandedIcon: 'pi pi-folder-open',
    collapsedIcon: 'pi pi-folder',
    children: faker.helpers.multiple(generateFakeChildrenNode, {
        count: faker.number.int({ max: 5, min: 1 })
    })
});

export const generateFakeTree = (count = 10) => faker.helpers.multiple(generateFakeNode, { count });
