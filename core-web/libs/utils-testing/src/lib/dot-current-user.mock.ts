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

    /**
     * Read+write on both types, matching the admin user above.
     *
     * This was missing, so anything piping it — DotPageStore's initial load, for one —
     * threw `getUserPermissions is not a function` from inside a mergeMap and the load
     * never completed. rxjs reports that asynchronously: Jest discarded it and five of
     * dot-pages.store's expectations read the untouched initial state instead, which is
     * why they asserted `canRead: {}` while the test's own comment expected permissions.
     */
    getUserPermissions() {
        return of({
            CONTENTLETS: { canRead: true, canWrite: true },
            HTMLPAGES: { canRead: true, canWrite: true }
        });
    }
}
