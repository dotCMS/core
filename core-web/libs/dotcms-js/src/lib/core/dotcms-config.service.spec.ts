import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { catchError } from 'rxjs/operators';

import { DotcmsConfigService } from './dotcms-config.service';
import { LoggerService } from './logger.service';

const CONFIG_URL = '/api/v1/appconfiguration';
const VERSION = 'a'.repeat(64);
const NEXT_VERSION = 'b'.repeat(64);

const configResponse = (languageBundleVersion?: string) => ({
    entity: {
        config: {
            colors: { primary: '#000', secondary: '#fff', background: '#eee' },
            emailRegex: '.*@.*',
            ...(languageBundleVersion ? { languageBundleVersion } : {}),
            license: {
                displayServerId: 'server-id',
                isCommunity: true,
                level: 100,
                levelName: 'COMMUNITY'
            },
            logos: { loginScreen: '/login.png', navBar: '/navbar.png' },
            'dotcms.paginator.links': 5,
            'dotcms.paginator.rows': 40,
            releaseInfo: { buildDate: '2026-11-05', version: '1.0.0' },
            websocket: {
                'dotcms.websocket.reconnect.time': 5000,
                'dotcms.websocket.disable': false
            },
            systemTimezone: { id: 'UTC', label: 'UTC', offset: '0' },
            timezones: []
        },
        menu: []
    }
});

describe('DotcmsConfigService', () => {
    let service: DotcmsConfigService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: LoggerService, useValue: { debug: vi.fn(), error: vi.fn() } }
            ]
        });
        service = TestBed.inject(DotcmsConfigService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        http.verify();
    });

    it('should keep passive update listeners alive across a failed load and later recovery', async () => {
        const updates = vi.fn();
        const error = vi.fn();
        const subscription = service.getConfigUpdates().subscribe({ next: updates, error });
        http.expectOne(CONFIG_URL).flush(null, { status: 500, statusText: 'Server Error' });
        expect(updates).not.toHaveBeenCalled();
        expect(error).not.toHaveBeenCalled();

        const recovered = firstValueFrom(service.getConfig());
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));
        await recovered;
        expect(updates).toHaveBeenCalledExactlyOnceWith(
            expect.objectContaining({ languageBundleVersion: VERSION })
        );
        expect(error).not.toHaveBeenCalled();
        subscription.unsubscribe();
    });

    it('should propagate the initial HTTP error to getConfig subscribers', () => {
        const next = vi.fn();
        const error = vi.fn();
        service.getConfig().subscribe({ next, error });

        http.expectOne(CONFIG_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

        expect(next).not.toHaveBeenCalled();
        expect(error).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ status: 401 }));
    });

    it('should retry on a subsequent getConfig call after the initial request fails', async () => {
        const first = firstValueFrom(service.getConfig());
        const failed = expect(first).rejects.toMatchObject({ status: 401 });
        http.expectOne(CONFIG_URL).flush(null, { status: 401, statusText: 'Unauthorized' });
        await failed;

        const retried = firstValueFrom(service.getConfig());
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));

        await expect(retried).resolves.toMatchObject({ languageBundleVersion: VERSION });
    });

    it('should retry after an eager constructor failure even without an initial getConfig subscriber', async () => {
        http.expectOne(CONFIG_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

        const loaded = firstValueFrom(service.getConfig());
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));

        await expect(loaded).resolves.toMatchObject({ languageBundleVersion: VERSION });
    });

    it('should allow an immediate retry from an error handler and share the retried request', async () => {
        const config$ = service.getConfig();
        const loaded = firstValueFrom(config$.pipe(catchError(() => config$)));
        http.expectOne(CONFIG_URL).flush(null, { status: 401, statusText: 'Unauthorized' });
        const second = firstValueFrom(service.getConfig());

        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));

        await expect(loaded).resolves.toMatchObject({ languageBundleVersion: VERSION });
        await expect(second).resolves.toBe(await loaded);
    });

    it('should share the eager request between concurrent getConfig subscribers', async () => {
        const first = firstValueFrom(service.getConfig());
        const second = firstValueFrom(service.getConfig());
        const request = http.expectOne(CONFIG_URL);
        expect(request.request.method).toBe('GET');
        request.flush(configResponse(VERSION));

        const [firstConfig, secondConfig] = await Promise.all([first, second]);
        expect(firstConfig).toBe(secondConfig);
        expect(firstConfig).toMatchObject({
            languageBundleVersion: VERSION,
            paginatorLinks: 5,
            paginatorRows: 40,
            websocket: { disabledWebsockets: false, websocketReconnectTime: 5000 }
        });
    });

    it('should replay already loaded configuration without a new GET', async () => {
        const loaded = firstValueFrom(service.getConfig());
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));
        const config = await loaded;

        await expect(firstValueFrom(service.getConfig())).resolves.toBe(config);
        await expect(firstValueFrom(service.getConfig())).resolves.toBe(config);
        http.expectNone(CONFIG_URL);
    });

    it('should keep long-lived subscribers connected and broadcast explicit loadConfig updates', async () => {
        const next = vi.fn();
        const complete = vi.fn();
        const subscription = service.getConfig().subscribe({ next, complete });
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));
        expect(next).toHaveBeenCalledTimes(1);
        expect(next).toHaveBeenLastCalledWith(
            expect.objectContaining({ languageBundleVersion: VERSION })
        );

        service.loadConfig();
        await expect(firstValueFrom(service.getConfig())).resolves.toMatchObject({
            languageBundleVersion: VERSION
        });
        http.expectOne(CONFIG_URL).flush(configResponse(NEXT_VERSION));

        expect(next).toHaveBeenCalledTimes(2);
        expect(next).toHaveBeenLastCalledWith(
            expect.objectContaining({ languageBundleVersion: NEXT_VERSION })
        );
        expect(complete).not.toHaveBeenCalled();
        expect(subscription.closed).toBe(false);
        await expect(firstValueFrom(service.getConfig())).resolves.toMatchObject({
            languageBundleVersion: NEXT_VERSION
        });
        http.expectNone(CONFIG_URL);
        subscription.unsubscribe();
    });

    it('should preserve cached config and existing subscribers if an explicit refresh fails', async () => {
        const next = vi.fn();
        const error = vi.fn();
        const subscription = service.getConfig().subscribe({ next, error });
        http.expectOne(CONFIG_URL).flush(configResponse(VERSION));

        service.loadConfig();
        http.expectOne(CONFIG_URL).flush(null, { status: 500, statusText: 'Server Error' });

        expect(error).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        await expect(firstValueFrom(service.getConfig())).resolves.toMatchObject({
            languageBundleVersion: VERSION
        });
        http.expectNone(CONFIG_URL);

        service.loadConfig();
        http.expectOne(CONFIG_URL).flush(configResponse(NEXT_VERSION));
        expect(next).toHaveBeenCalledTimes(2);
        expect(next).toHaveBeenLastCalledWith(
            expect.objectContaining({ languageBundleVersion: NEXT_VERSION })
        );
        subscription.unsubscribe();
    });

    it('should accept legacy configuration responses without languageBundleVersion', async () => {
        const loaded = firstValueFrom(service.getConfig());
        http.expectOne(CONFIG_URL).flush(configResponse());

        await expect(loaded).resolves.toMatchObject({
            languageBundleVersion: undefined,
            releaseInfo: { buildDate: '2026-11-05', version: '1.0.0' }
        });
    });
});
