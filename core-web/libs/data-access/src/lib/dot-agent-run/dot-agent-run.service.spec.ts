import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';
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

        it('maps a phase event to a phase-typed step (message split from meta)', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: phase\ndata: {"phase":"scan","message":"Scanning live + working (preview) baseline"}\n\n',
                    'event: phase\ndata: {"phase":"read","message":"Agent: reading template.vtl"}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                {
                    type: 'phase',
                    step: {
                        message: 'Scanning live + working (preview) baseline',
                        meta: { phase: 'scan' }
                    }
                },
                {
                    type: 'phase',
                    step: { message: 'Agent: reading template.vtl', meta: { phase: 'read' } }
                }
            ]);
        });

        it('maps a progress event to a typed running count', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: progress\ndata: {"baseline":29,"current":3,"cleared":26}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                { type: 'progress', progress: { baseline: 29, current: 3, cleared: 26 } }
            ]);
        });

        it('maps a workingChanged event to the typed changed-file list (dropping malformed entries)', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: workingChanged\ndata: {"changedFiles":[' +
                        '{"path":"//site/a.css","identifier":"id-a"},' +
                        '{"path":"//site/b.vtl"},' + // no identifier → dropped
                        '{"path":"//site/c.vtl","identifier":"id-c"}]}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                {
                    type: 'workingChanged',
                    changedFiles: [
                        { path: '//site/a.css', identifier: 'id-a' },
                        { path: '//site/c.vtl', identifier: 'id-c' }
                    ]
                }
            ]);
        });

        it('maps a heartbeat event to the typed keep-alive timings', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: heartbeat\ndata: {"elapsedMs":45000,"sinceLastEventMs":8000}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                { type: 'heartbeat', heartbeat: { elapsedMs: 45000, sinceLastEventMs: 8000 } }
            ]);
        });

        it('emits a run event for the first run-id frame (no event name, no message)', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'data: {"runId":"r_abc123"}\n\n',
                    'event: step\ndata: {"message":"working"}\n\n',
                    'event: done\ndata: {"total":1}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual<AgentStreamEvent<DemoResult>[]>([
                { type: 'run', runId: 'r_abc123' },
                { type: 'step', step: { message: 'working' } },
                { type: 'done', result: { total: 1 } }
            ]);
        });

        it('keeps a step a step even if its payload carries a runId', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse(['event: step\ndata: {"runId":"r_x","message":"working"}\n\n'])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            // runId rides along as meta; the frame stays a step, not a run event.
            expect(events).toEqual([
                { type: 'step', step: { message: 'working', meta: { runId: 'r_x' } } }
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
            fetchMock.mockResolvedValue(mockSseResponse(['event: aborted\ndata: {"total":1}\n\n']));

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'aborted', result: { total: 1 } }]);
        });

        it('maps an error event to a typed error with a fallback message', async () => {
            fetchMock.mockResolvedValue(mockSseResponse(['event: error\ndata: {}\n\n']));

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'error', message: 'Agent run failed.' }]);
        });

        it('emits a trailing frame that never got its blank-line terminator', async () => {
            // A server that ends the body right after writing the terminal event leaves the
            // frame in the buffer with no `\n\n`. Dropping it turned a finished run into
            // one that looked hung — the consumer waited forever for a `done` already sent.
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: step\ndata: {"message":"Fixing"}\n\n',
                    'event: done\ndata: {"total":3}\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events[events.length - 1]).toEqual({ type: 'done', result: { total: 3 } });
        });

        it('does not invent an event from trailing whitespace', async () => {
            fetchMock.mockResolvedValue(
                mockSseResponse(['event: done\ndata: {"total":1}\n\n', '\n\n  \n'])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            expect(events).toEqual([{ type: 'done', result: { total: 1 } }]);
        });

        it('drops an unparseable frame but logs it, and keeps the stream alive', async () => {
            const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
            fetchMock.mockResolvedValue(
                mockSseResponse([
                    'event: step\ndata: {not json}\n\n',
                    'event: done\ndata: {"total":7}\n\n'
                ])
            );

            const events = await firstValueFrom(
                service.run<DemoResult>('/url', {}).pipe(toArray())
            );

            // One bad frame from a flaky backend must not take the whole stream down...
            expect(events).toEqual([{ type: 'done', result: { total: 7 } }]);
            // ...but it must not vanish silently either.
            expect(warn).toHaveBeenCalledWith(
                expect.stringContaining('dropped an unparseable "step" frame'),
                expect.stringContaining('{not json}')
            );
            warn.mockRestore();
        });

        it('errors the observable when the response is not ok', async () => {
            fetchMock.mockResolvedValue(mockSseResponse([], { ok: false, status: 500 }));

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
