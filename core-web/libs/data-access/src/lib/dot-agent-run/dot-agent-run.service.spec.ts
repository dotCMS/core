import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';
import { firstValueFrom, toArray } from 'rxjs';

import { AgentStreamEvent } from '@dotcms/dotcms-models';

import { DotAgentRunService } from './dot-agent-run.service';

/** Build a mock fetch Response whose body streams the given SSE text in chunks. */
function mockSseResponse(chunks: string[], { ok = true, status = 200 } = {}): Response {
    const encoder = new TextEncoder();
    let i = 0;
    const body = {
        getReader() {
            return {
                read() {
                    if (i < chunks.length) {
                        return Promise.resolve({ value: encoder.encode(chunks[i++]), done: false });
                    }

                    return Promise.resolve({ value: undefined, done: true });
                }
            };
        }
    } as unknown as ReadableStream<Uint8Array>;

    return { ok, status, statusText: 'OK', body } as unknown as Response;
}

interface DemoResult {
    total: number;
}

describe('DotAgentRunService', () => {
    let spectator: SpectatorHttp<DotAgentRunService>;
    let service: DotAgentRunService;
    const fetchMock = jest.fn();
    const originalFetch = global.fetch;

    const createHttp = createHttpFactory(DotAgentRunService);

    beforeEach(() => {
        spectator = createHttp();
        service = spectator.service;
        global.fetch = fetchMock as unknown as typeof fetch;
        fetchMock.mockReset();
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    describe('run() SSE parsing', () => {
        it('emits step events with message split from meta, then done with the result', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: step\ndata: {"phase":"scan","message":"Scanning page"}\n\n',
                    'event: step\ndata: {"phase":"fix","message":"Fixed alt text"}\n\n',
                    'event: done\ndata: {"total":3}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/api/v1/agent/demo/stream', { id: 'x' }).pipe(toArray())
            );

            expect(fetchMock).toHaveBeenCalledWith(
                '/api/v1/agent/demo/stream',
                expect.objectContaining({ method: 'POST' })
            );
            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                { type: 'step', step: { message: 'Scanning page', meta: { phase: 'scan' } } },
                { type: 'step', step: { message: 'Fixed alt text', meta: { phase: 'fix' } } },
                { type: 'done', result: { total: 3 } }
            ]);
        });

        it('handles frames that straddle chunk boundaries', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: step\ndata: {"mess',
                    'age":"partial"}\n\nevent: done\ndata: {"total":0}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([
                { type: 'step', step: { message: 'partial' } },
                { type: 'done', result: { total: 0 } }
            ]);
        });

        it('emits a step with no meta when only a message is present', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse(['event: step\ndata: {"message":"just text"}\n\n'])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'step', step: { message: 'just text' } }]);
        });

        it('maps aborted to a terminal event carrying the partial result', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse(['event: aborted\ndata: {"total":1}\n\n'])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'aborted', result: { total: 1 } }]);
        });

        it('maps an error event to a typed error with a fallback message', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse(['event: error\ndata: {}\n\n'])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'error', message: 'Agent run failed.' }]);
        });

        it('errors the observable when the response is not ok', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([], { ok: false, status: 500 })
            );

            await expect(
                firstValueFrom(service.run<DemoResult>('/url', {}).pipe(toArray()))
            ).rejects.toThrow(/Agent request failed \(500/);
        });
    });

    describe('stop()', () => {
        it('POSTs to the given url', () => {
            service.stop('/api/v1/agent/demo/stop').subscribe();
            const req = spectator.expectOne('/api/v1/agent/demo/stop', HttpMethod.POST);
            expect(req.request.body).toEqual({});
        });
    });
});
