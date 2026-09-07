import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotApiToken, DotUsersService } from './dot-users.service';

const MOCK_TOKEN: DotApiToken = {
    id: 'tok-1',
    userId: 'user-1',
    requestingUserId: 'admin',
    requestingIp: null,
    issuer: null,
    subject: null,
    tokenType: null,
    claims: { label: 'ci' },
    allowNetwork: '0.0.0.0/0',
    issueDate: 1_700_000_000_000,
    expiresDate: 1_800_000_000_000,
    revokedDate: null,
    modificationDate: 1_700_000_000_000,
    valid: true,
    expired: false,
    revoked: false
};

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

    describe('API tokens', () => {
        it('should GET tokens with showRevoked=false by default', () => {
            spectator.service.getApiTokens('user-1', false).subscribe();

            const req = spectator.expectOne(
                '/api/v1/apitoken/user-1/tokens?showRevoked=false',
                HttpMethod.GET
            );
            req.flush({ entity: { tokens: [MOCK_TOKEN] } });
        });

        it('should GET tokens with showRevoked=true when the toggle is on', () => {
            spectator.service.getApiTokens('user-1', true).subscribe();

            const req = spectator.expectOne(
                '/api/v1/apitoken/user-1/tokens?showRevoked=true',
                HttpMethod.GET
            );
            req.flush({ entity: { tokens: [] } });
        });

        it('should unwrap the tokens envelope', (done) => {
            spectator.service.getApiTokens('user-1', false).subscribe((tokens) => {
                expect(tokens).toEqual([MOCK_TOKEN]);
                done();
            });

            spectator
                .expectOne('/api/v1/apitoken/user-1/tokens?showRevoked=false', HttpMethod.GET)
                .flush({ entity: { tokens: [MOCK_TOKEN] } });
        });

        it('should fall back to [] when the envelope has no tokens', (done) => {
            spectator.service.getApiTokens('user-1', false).subscribe((tokens) => {
                expect(tokens).toEqual([]);
                done();
            });

            spectator
                .expectOne('/api/v1/apitoken/user-1/tokens?showRevoked=false', HttpMethod.GET)
                .flush({ entity: null });
        });

        it('should POST createApiToken and unwrap the { jwt, token } envelope', (done) => {
            spectator.service
                .createApiToken({
                    userId: 'user-1',
                    expirationSeconds: 3600,
                    network: '0.0.0.0/0',
                    claims: { label: 'ci' }
                })
                .subscribe((result) => {
                    expect(result).toEqual({ jwt: 'raw-jwt', token: MOCK_TOKEN });
                    done();
                });

            const req = spectator.expectOne('/api/v1/apitoken', HttpMethod.POST);
            expect(req.request.body).toEqual({
                userId: 'user-1',
                expirationSeconds: 3600,
                network: '0.0.0.0/0',
                claims: { label: 'ci' }
            });
            req.flush({ entity: { jwt: 'raw-jwt', token: MOCK_TOKEN } });
        });

        it('should GET a fresh JWT and unwrap it', (done) => {
            spectator.service.getApiTokenJwt('tok-1').subscribe((jwt) => {
                expect(jwt).toBe('fresh-jwt');
                done();
            });

            spectator
                .expectOne('/api/v1/apitoken/tok-1/jwt', HttpMethod.GET)
                .flush({ entity: { jwt: 'fresh-jwt' } });
        });

        it('should throw when the JWT envelope is malformed instead of returning ""', (done) => {
            spectator.service.getApiTokenJwt('tok-1').subscribe({
                next: () => done.fail('expected an error, got a value'),
                error: (error: Error) => {
                    expect(error.message).toBe('Malformed JWT response');
                    done();
                }
            });

            spectator.expectOne('/api/v1/apitoken/tok-1/jwt', HttpMethod.GET).flush({ entity: {} });
        });

        it('should PUT to /revoke with a null body', () => {
            spectator.service.revokeApiToken('tok-1').subscribe();

            const req = spectator.expectOne('/api/v1/apitoken/tok-1/revoke', HttpMethod.PUT);
            expect(req.request.body).toBeNull();
            req.flush({});
        });

        it('should DELETE the token by id', () => {
            spectator.service.deleteApiToken('tok-1').subscribe();

            spectator.expectOne('/api/v1/apitoken/tok-1', HttpMethod.DELETE).flush({});
        });
    });
});
