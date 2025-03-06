import { of } from 'rxjs';

export const CurrentUserAdminDataMock = {
    admin: true,
    email: 'admin@dotcms.com',
    givenName: 'TEST',
    roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
    surname: 'User',
    userId: 'testId'
};

export const CurrentUserDataMock = {
    admin: false,
    email: 'admin@dotcms.com',
    givenName: 'TEST',
    roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
    surname: 'User',
    userId: 'testId'
};

export class DotCurrentUserServiceMock {
    getCurrentUser() {
        return of(CurrentUserAdminDataMock);
    }
}
