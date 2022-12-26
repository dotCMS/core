import { DotRole } from '@dotcms/dotcms-models';

export const mockProcessedRoles: DotRole[] = [
    {
        id: '1',
        name: 'Current User',
        user: false,
        roleKey: 'CMS Anonymous'
    },
    { id: '2', name: 'Some Role (User)', user: true, roleKey: 'roleKey1' }
];
