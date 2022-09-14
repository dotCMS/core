import { Auth, User } from '@dotcms/dotcms-js';

const mockLoginAsUser: User = {
    admin: true,
    emailAddress: 'mail',
    firstName: 'Firstname',
    lastName: 'lastname',
    userId: 'login-as-user'
};
const mockUser: User = {
    admin: true,
    emailAddress: 'mail',
    firstName: 'Firstname',
    lastName: 'lastname',
    userId: '123'
};
export const mockUserAuth: Auth = {
    loginAsUser: mockLoginAsUser,
    user: mockUser
};
