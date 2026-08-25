import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotUsersService } from './dot-users.service';

import { createFakeUser } from '../testing/dot-user.mock';

const MOCK_USERS = [
    createFakeUser({
        userId: 'dotcms.org.1',
        id: 'dotcms.org.1',
        firstName: 'Admin',
        lastName: 'User',
        fullName: 'Admin User',
        name: 'Admin User',
        emailAddress: 'admin@dotcms.com',
        gravitar: 'abc123',
        admin: true,
        lastLoginDate: 1717977600000,
        lastLoginIP: '10.0.0.1'
    })
];

describe('DotUsersService', () => {
    let spectator: SpectatorHttp<DotUsersService>;

    const createService = createHttpFactory({ service: DotUsersService });

    beforeEach(() => {
        spectator = createService();
    });

    it('should build the filter URL with all query params', () => {
        spectator.service
            .getUsersPaginated({
                filter: 'jane',
                page: 2,
                perPage: 40,
                orderBy: 'firstName',
                direction: 'ASC',
                includeAnonymous: true
            })
            .subscribe();

        const req = spectator.expectOne(
            '/api/v1/users/filter?query=jane&page=2&per_page=40&orderby=firstName&direction=ASC&includeanonymous=true',
            HttpMethod.GET
        );
        req.flush({
            entity: MOCK_USERS,
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        });
    });

    it('should omit params that were not provided', () => {
        spectator.service.getUsersPaginated({}).subscribe();

        const req = spectator.expectOne('/api/v1/users/filter', HttpMethod.GET);
        expect(req.request.params.keys().length).toBe(0);
        req.flush({ entity: [], errors: [], messages: [], permissions: [], i18nMessagesMap: {} });
    });

    it('should send roleKey when provided', () => {
        spectator.service.getUsersPaginated({ roleKey: 'DOTCMS_BACK_END_USER' }).subscribe();

        const req = spectator.expectOne(
            '/api/v1/users/filter?roleKey=DOTCMS_BACK_END_USER',
            HttpMethod.GET
        );
        req.flush({ entity: [], errors: [], messages: [], permissions: [], i18nMessagesMap: {} });
    });

    it('should DELETE with replacementUserId when provided', () => {
        spectator.service.deleteUser('user-1', 'admin').subscribe();

        const req = spectator.expectOne(
            '/api/v1/users/user-1?replacementUserId=admin',
            HttpMethod.DELETE
        );
        req.flush({});
    });

    it('should DELETE without replacementUserId when omitted', () => {
        spectator.service.deleteUser('user-2').subscribe();

        const req = spectator.expectOne('/api/v1/users/user-2', HttpMethod.DELETE);
        expect(req.request.params.has('replacementUserId')).toBe(false);
        req.flush({});
    });
});
