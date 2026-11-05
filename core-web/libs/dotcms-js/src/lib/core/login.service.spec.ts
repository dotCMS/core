import { EMPTY, firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotcmsEventsService } from './dotcms-events.service';
import { DotLoginParams, LoginService, User } from './login.service';

const user: User = {
    admin: true,
    emailAddress: 'user@example.com',
    firstName: 'Test',
    lastName: 'User',
    userId: 'user-id',
    languageId: 'en_US'
};

const loginParams = (language?: string): DotLoginParams => ({
    login: user.emailAddress,
    password: '',
    rememberMe: false,
    language,
    backEndLogin: true
});

describe('LoginService locale', () => {
    let service: LoginService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotcmsEventsService, useValue: { subscribeTo: () => EMPTY } }
            ]
        });
        service = TestBed.inject(LoginService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('uses the successful login selection before notifying auth subscribers without changing the saved language', async () => {
        const locales: string[] = [];
        const subscription = service.auth$.subscribe(() => {
            locales.push(service.currentUserLanguageId);
            expect(service.auth.user.languageId).toBe('en_US');
        });
        const loggedIn = firstValueFrom(service.loginUser(loginParams('es_ES')));
        expect(service.currentUserLanguageId).toBe('');
        const request = http.expectOne('/api/v1/authentication');
        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({
            userId: user.emailAddress,
            password: '',
            rememberMe: false,
            language: 'es',
            country: 'ES',
            backEndLogin: true
        });
        request.flush({ entity: user });

        await expect(loggedIn).resolves.toEqual(user);
        expect(service.currentUserLanguageId).toBe('es_ES');
        expect(service.auth).toEqual({ user, loginAsUser: null, isLoginAs: false });
        expect(locales).toEqual(['es_ES']);
        subscription.unsubscribe();
    });

    it.each([undefined, ''])(
        'falls back to the saved language when selection is %s, not a previous user selection',
        async (language) => {
            service.setAuth(
                { user: { ...user, userId: 'previous-user' }, loginAsUser: null },
                'fr_FR'
            );
            const loggedIn = firstValueFrom(service.loginUser(loginParams(language)));
            const request = http.expectOne('/api/v1/authentication');
            expect(request.request.body).toMatchObject({ language: '', country: '' });
            request.flush({ entity: user });

            await loggedIn;
            expect(service.currentUserLanguageId).toBe('en_US');
            expect(service.auth.user.languageId).toBe('en_US');
        }
    );

    it('does not override auth or locale on a failed login', async () => {
        service.setAuth({ user, loginAsUser: null }, 'fr_FR');
        const previousAuth = service.auth;
        const updates = vi.fn();
        const subscription = service.auth$.subscribe(updates);
        const loggedIn = firstValueFrom(service.loginUser(loginParams('es_ES')));
        const rejected = expect(loggedIn).rejects.toMatchObject({ status: 401 });
        http.expectOne('/api/v1/authentication').flush(null, {
            status: 401,
            statusText: 'Unauthorized'
        });

        await rejected;
        expect(service.currentUserLanguageId).toBe('fr_FR');
        expect(service.auth).toBe(previousAuth);
        expect(updates).not.toHaveBeenCalled();
        subscription.unsubscribe();
    });

    it('loads existing auth using the saved user language and preserves the impersonated principal', async () => {
        const loginAsUser = { ...user, userId: 'impersonated-user', languageId: 'de_DE' };
        const locales: string[] = [];
        const subscription = service.auth$.subscribe(() =>
            locales.push(service.currentUserLanguageId)
        );
        const loaded = firstValueFrom(service.loadAuth());
        const request = http.expectOne('/api/v1/authentication/logInUser');
        expect(request.request.method).toBe('GET');
        request.flush({ entity: { user, loginAsUser } });

        await expect(loaded).resolves.toEqual({ user, loginAsUser, isLoginAs: true });
        expect(service.auth).toEqual({ user, loginAsUser, isLoginAs: true });
        expect(locales).toEqual(['en_US']);
        subscription.unsubscribe();
    });

    it('does not replace current auth when an auth load has no user', async () => {
        service.setAuth({ user, loginAsUser: null }, 'es_ES');
        const previousAuth = service.auth;
        const loaded = firstValueFrom(service.loadAuth());
        http.expectOne('/api/v1/authentication/logInUser').flush({
            entity: { user: null, loginAsUser: null }
        });

        await expect(loaded).resolves.toEqual({ user: null, loginAsUser: null, isLoginAs: false });
        expect(service.auth).toBe(previousAuth);
        expect(service.currentUserLanguageId).toBe('es_ES');
    });

    it('retains loginAs and logoutAs auth behavior without transferring the selected locale to another principal', async () => {
        service.setAuth({ user, loginAsUser: null }, 'es_ES');
        const loginAsUser = { ...user, userId: 'impersonated-user', languageId: 'de_DE' };
        const impersonated = firstValueFrom(service.loginAs({ user: loginAsUser, password: '' }));
        const loginAsRequest = http.expectOne('/api/v1/users/loginas');
        expect(loginAsRequest.request.body).toEqual({ password: '', userId: loginAsUser.userId });
        loginAsRequest.flush({ entity: { loginAs: true } });

        await expect(impersonated).resolves.toBe(true);
        expect(service.auth).toEqual({ user, loginAsUser, isLoginAs: true });
        expect(service.currentUserLanguageId).toBe('en_US');

        const loggedOutAs = firstValueFrom(service.logoutAs());
        const logoutAsRequest = http.expectOne('/api/v1/users/logoutas');
        expect(logoutAsRequest.request.method).toBe('PUT');
        logoutAsRequest.flush({ entity: { logoutAs: true } });

        await expect(loggedOutAs).resolves.toBe(true);
        expect(service.auth).toEqual({ user, loginAsUser: null, isLoginAs: false });
        expect(service.currentUserLanguageId).toBe('en_US');
    });
});
