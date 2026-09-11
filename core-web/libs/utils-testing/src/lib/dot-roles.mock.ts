import { DotRole } from '@dotcms/dotcms-models';

export const mockProcessedRoles: DotRole[] = [
    {
        id: '1',
        name: 'Current User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Some Role', user: true, roleKey: 'roleKey1' }
];
