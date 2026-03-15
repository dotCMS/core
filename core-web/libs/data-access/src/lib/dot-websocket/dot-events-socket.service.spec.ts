import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DOT_EVENTS_SOCKET_URL } from './dot-events-socket-url';
import { DotEventsSocket, WebSocketStatus } from './dot-events-socket.service';

// ---------------------------------------------------------------------------
// Minimal WebSocket mock — exposes handlers so tests can trigger them
// ---------------------------------------------------------------------------
class MockWebSocket {
    static instances: MockWebSocket[] = [];

    onopen: (() => void) | null = null;
    onmessage: ((ev: Partial<MessageEvent>) => void) | null = null;
    onclose: ((ev: Partial<CloseEvent>) => void) | null = null;
    onerror: (() => void) | null = null;
    readyState = WebSocket.CONNECTING;

    constructor(public url: string) {
        MockWebSocket.instances.push(this);
    }

    close(): void {
        this.readyState = WebSocket.CLOSED;
    }

    /** Test helper — simulate a successful connection */
    triggerOpen(): void {
        this.readyState = WebSocket.OPEN;
        this.onopen?.();
    }

    /** Test helper — simulate a close event */
    triggerClose(code = 1006): void {
        this.readyState = WebSocket.CLOSED;
        this.onclose?.({ code });
    }

    /** Test helper — simulate an incoming message */
    triggerMessage(data: unknown): void {
        this.onmessage?.({ data: JSON.stringify(data) });
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
const WS_URL = 'ws://localhost:8080/api/ws/v1/system/events';

const mockSocketURL = WS_URL;

function latestSocket(): MockWebSocket {
    return MockWebSocket.instances[MockWebSocket.instances.length - 1];
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('DotEventsSocket', () => {
    let spectator: SpectatorService<DotEventsSocket>;
    let service: DotEventsSocket;

    const createService = createServiceFactory({
        service: DotEventsSocket,
        providers: [{ provide: DOT_EVENTS_SOCKET_URL, useValue: mockSocketURL }]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        MockWebSocket.instances = [];
        (global as unknown as { WebSocket: unknown }).WebSocket = MockWebSocket;

        spectator = createService();
        service = spectator.service;
    });

    afterEach(() => {
        jest.useRealTimers();
        service.destroy();
    });

    // -----------------------------------------------------------------------
    // connect()
    // -----------------------------------------------------------------------
    describe('connect()', () => {
        it('should open a WebSocket to the configured URL', () => {
            service.connect().subscribe();

            expect(MockWebSocket.instances.length).toBe(1);
            expect(latestSocket().url).toBe(WS_URL);
        });

        it('should emit and complete immediately', (done) => {
            let emitted = false;
            service.connect().subscribe({
                next: () => {
                    emitted = true;
                },
                complete: () => {
                    expect(emitted).toBe(true);
                    done();
                }
            });
        });

        it('should set status to "connecting" on first connect', () => {
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));

            service.connect().subscribe();

            expect(statuses).toContain('connecting');
        });

        it('should set status to "connected" when socket opens', () => {
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));

            service.connect().subscribe();
            latestSocket().triggerOpen();

            expect(statuses[statuses.length - 1]).toBe('connected');
        });

        it('should reset retryCount on successful open', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            latestSocket().triggerClose();
            jest.advanceTimersByTime(2000);

            latestSocket().triggerOpen();

            // After reconnect succeeds, status goes back to connected
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));
            expect(service.isConnected()).toBe(true);
        });
    });

    // -----------------------------------------------------------------------
    // on<T>() — message filtering
    // -----------------------------------------------------------------------
    describe('on()', () => {
        it('should emit payload for matching event type', (done) => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            service.on<{ name: string }>('PUBLISH_SITE').subscribe((data) => {
                expect(data).toEqual({ name: 'demo.dotcms.com' });
                done();
            });

            latestSocket().triggerMessage({
                event: 'PUBLISH_SITE',
                payload: { data: { name: 'demo.dotcms.com' } }
            });
        });

        it('should not emit for non-matching event type', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            const received: unknown[] = [];
            service.on('OTHER_EVENT').subscribe((d) => received.push(d));

            latestSocket().triggerMessage({
                event: 'PUBLISH_SITE',
                payload: { data: {} }
            });

            expect(received).toHaveLength(0);
        });

        it('should ignore unparseable messages', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            const received: unknown[] = [];
            service.on('ANY').subscribe((d) => received.push(d));

            // Trigger invalid JSON via onmessage directly
            latestSocket().onmessage?.({ data: 'not-json' });

            expect(received).toHaveLength(0);
        });
    });

    // -----------------------------------------------------------------------
    // messages()
    // -----------------------------------------------------------------------
    describe('messages()', () => {
        it('should emit all raw messages', (done) => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            service.messages().subscribe((msg) => {
                expect(msg.event).toBe('UPDATE_SITE');
                done();
            });

            latestSocket().triggerMessage({ event: 'UPDATE_SITE', payload: { data: {} } });
        });
    });

    // -----------------------------------------------------------------------
    // isConnected()
    // -----------------------------------------------------------------------
    describe('isConnected()', () => {
        it('should return false before connection opens', () => {
            service.connect().subscribe();

            expect(service.isConnected()).toBe(false);
        });

        it('should return true after socket opens', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            expect(service.isConnected()).toBe(true);
        });

        it('should return false after destroy', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            service.destroy();

            expect(service.isConnected()).toBe(false);
        });
    });

    // -----------------------------------------------------------------------
    // Reconnection
    // -----------------------------------------------------------------------
    describe('reconnection', () => {
        it('should reconnect after socket closes unexpectedly', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            latestSocket().triggerClose(1006);

            jest.advanceTimersByTime(3000);

            expect(MockWebSocket.instances.length).toBe(2);
        });

        it('should set status to "reconnecting" after first disconnect', () => {
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));

            service.connect().subscribe();
            latestSocket().triggerOpen();
            latestSocket().triggerClose(1006);

            expect(statuses).toContain('reconnecting');
        });

        it('should NOT reconnect after destroy', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            service.destroy();
            latestSocket().triggerClose(1006);

            jest.advanceTimersByTime(5000);

            expect(MockWebSocket.instances.length).toBe(1);
        });

        it('should not reconnect on close code 1001 (going away)', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            latestSocket().triggerClose(1001);

            jest.advanceTimersByTime(5000);

            expect(MockWebSocket.instances.length).toBe(1);
        });

        it('should use exponential backoff — second retry waits longer than first', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();

            // First disconnect
            latestSocket().triggerClose(1006);
            const countAfterFirst = MockWebSocket.instances.length;
            jest.advanceTimersByTime(2000); // enough for first retry (1s base + jitter)
            const countAfterFirstRetry = MockWebSocket.instances.length;

            // Second disconnect
            latestSocket().triggerClose(1006);
            jest.advanceTimersByTime(2000); // NOT enough for second retry (2s base + jitter)
            const countAfterShortWait = MockWebSocket.instances.length;

            jest.advanceTimersByTime(3000); // now enough
            const countAfterLongWait = MockWebSocket.instances.length;

            expect(countAfterFirstRetry).toBeGreaterThan(countAfterFirst);
            expect(countAfterShortWait).toBe(countAfterFirstRetry); // no new socket yet
            expect(countAfterLongWait).toBeGreaterThan(countAfterShortWait);
        });
    });

    // -----------------------------------------------------------------------
    // destroy()
    // -----------------------------------------------------------------------
    describe('destroy()', () => {
        it('should set status to "closed"', () => {
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));

            service.connect().subscribe();
            latestSocket().triggerOpen();
            service.destroy();

            expect(statuses[statuses.length - 1]).toBe('closed');
        });

        it('should close the underlying socket', () => {
            service.connect().subscribe();
            latestSocket().triggerOpen();
            const socket = latestSocket();
            service.destroy();

            expect(socket.readyState).toBe(WebSocket.CLOSED);
        });

        it('should do nothing if called without a prior connect()', () => {
            expect(() => service.destroy()).not.toThrow();
        });
    });

    // -----------------------------------------------------------------------
    // status$() deduplication
    // -----------------------------------------------------------------------
    describe('status$()', () => {
        it('should not emit duplicate statuses', () => {
            const statuses: WebSocketStatus[] = [];
            service.status$().subscribe((s) => statuses.push(s));

            service.connect().subscribe();
            // Both openSocket calls set 'connecting' — only one emission expected
            service.connect().subscribe();

            expect(statuses.filter((s) => s === 'connecting')).toHaveLength(1);
        });
    });
});
