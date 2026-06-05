import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotVelocityPlaygroundService } from './dot-velocity-playground.service';

import { DotVelocityPlaygroundResponse } from '../models/dot-velocity-playground.models';

describe('DotVelocityPlaygroundService', () => {
    let spectator: SpectatorHttp<DotVelocityPlaygroundService>;
    const createService = createHttpFactory(DotVelocityPlaygroundService);

    beforeEach(() => {
        spectator = createService();
    });

    it('posts the velocity body as JSON to /api/vtl/dynamic/', () => {
        let received: DotVelocityPlaygroundResponse | undefined;
        spectator.service.runScript({ velocity: '$foo' }).subscribe((res) => {
            received = res;
        });

        const req = spectator.expectOne('/api/vtl/dynamic/', HttpMethod.POST);
        expect(req.request.body).toEqual({ velocity: '$foo' });
        req.flush('hello', {
            headers: { 'Content-Type': 'text/plain' },
            status: 200,
            statusText: 'OK'
        });

        expect(received?.body).toBe('hello');
        expect(received?.contentType).toBe('plaintext');
        expect(typeof received?.elapsedMs).toBe('number');
    });

    it('maps application/json content type to "json"', () => {
        let received: DotVelocityPlaygroundResponse | undefined;
        spectator.service.runScript({ velocity: 'x' }).subscribe((res) => {
            received = res;
        });

        const req = spectator.expectOne('/api/vtl/dynamic/', HttpMethod.POST);
        req.flush('{"ok":true}', {
            headers: { 'Content-Type': 'application/json' },
            status: 200,
            statusText: 'OK'
        });

        expect(received?.contentType).toBe('json');
    });

    it('strips charset and maps application/xml;charset=utf-8 to "xml"', () => {
        let received: DotVelocityPlaygroundResponse | undefined;
        spectator.service.runScript({ velocity: 'x' }).subscribe((res) => {
            received = res;
        });

        const req = spectator.expectOne('/api/vtl/dynamic/', HttpMethod.POST);
        req.flush('<foo/>', {
            headers: { 'Content-Type': 'application/xml;charset=utf-8' },
            status: 200,
            statusText: 'OK'
        });

        expect(received?.contentType).toBe('xml');
    });

    it('defaults to "plaintext" when no Content-Type header is present', () => {
        let received: DotVelocityPlaygroundResponse | undefined;
        spectator.service.runScript({ velocity: 'x' }).subscribe((res) => {
            received = res;
        });

        const req = spectator.expectOne('/api/vtl/dynamic/', HttpMethod.POST);
        req.flush('raw output', {
            status: 200,
            statusText: 'OK'
        });

        expect(received?.contentType).toBe('plaintext');
        expect(received?.body).toBe('raw output');
    });
});
