import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { firstValueFrom, lastValueFrom, toArray } from 'rxjs';

import { DotA11yAgentService } from './dot-a11y-agent.service';

import { AgentFixRequest, AgentStreamEvent } from '../models/accessibility-studio.models';

/**
 * Builds a fetch Response whose body streams the given chunks. Chunks model how
 * SSE frames arrive over the wire — possibly split mid-frame across reads.
 *
 * jsdom has no ReadableStream, so we hand-roll the minimal reader contract the
 * service uses: body.getReader() → reader.read() yielding {value, done}.
 */
function streamResponse(chunks: string[], init: Partial<Response> = {}): Response {
    const encoder = new TextEncoder();
    let i = 0;
    const reader = {
        read: () =>
            Promise.resolve(
                i < chunks.length
                    ? { value: encoder.encode(chunks[i++]), done: false }
                    : { value: undefined, done: true }
            )
    };

    return {
        ok: init.ok ?? true,
        status: init.status ?? 200,
        statusText: init.statusText ?? 'OK',
        body: { getReader: () => reader }
    } as unknown as Response;
}

const REQUEST: AgentFixRequest = {
    runId: 'r1',
    dotcmsBaseUrl: 'http://localhost',
    page: {
        identifier: 'id-1',
        uri: '/index',
        liveUrl: 'http://localhost/index',
        host: 'demo.dotcms.com',
        hostId: 'h1',
        languageId: 1
    },
    options: { skipCss: false }
};

describe('DotA11yAgentService', () => {
    let spectator: SpectatorService<DotA11yAgentService>;
    let service: DotA11yAgentService;
    const fetchMock = jest.fn();

    const createService = createServiceFactory(DotA11yAgentService);

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        global.fetch = fetchMock as unknown as typeof fetch;
        fetchMock.mockReset();
    });

    it('parses step + done events from the SSE stream', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([
                'event: step\ndata: {"phase":"scan","message":"Scanning"}\n\n',
                'event: step\ndata: {"phase":"fix","message":"Fixing .btn"}\n\n',
                'event: done\ndata: {"report":{"runId":"r1"}}\n\n'
            ])
        );

        const events = await lastValueFrom(service.fixStream(REQUEST).pipe(toArray()));

        expect(events).toEqual<AgentStreamEvent[]>([
            { type: 'step', phase: 'scan', message: 'Scanning' },
            { type: 'step', phase: 'fix', message: 'Fixing .btn' },
            { type: 'done', report: { runId: 'r1' } as never }
        ]);
    });

    it('reassembles a frame split across read chunks', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([
                'event: step\ndata: {"phase":"re',
                'ad","message":"reading x.vtl"}\n\n'
            ])
        );

        const first = await firstValueFrom(service.fixStream(REQUEST));
        expect(first).toEqual({ type: 'step', phase: 'read', message: 'reading x.vtl' });
    });

    it('maps an SSE error event to an error event (not a throw)', async () => {
        fetchMock.mockResolvedValue(
            streamResponse(['event: error\ndata: {"message":"render unreliable"}\n\n'])
        );

        const event = await firstValueFrom(service.fixStream(REQUEST));
        expect(event).toEqual({ type: 'error', message: 'render unreliable' });
    });

    it('errors the observable on a non-OK response', async () => {
        fetchMock.mockResolvedValue(
            streamResponse([], { ok: false, status: 502, statusText: 'Bad Gateway' })
        );

        await expect(firstValueFrom(service.fixStream(REQUEST))).rejects.toThrow(/502/);
    });

    it('unknown phase falls back to "fix"', async () => {
        fetchMock.mockResolvedValue(
            streamResponse(['event: step\ndata: {"phase":"bogus","message":"x"}\n\n'])
        );

        const event = await firstValueFrom(service.fixStream(REQUEST));
        expect(event).toEqual({ type: 'step', phase: 'fix', message: 'x' });
    });
});
