import { signalStore, withState } from '@ngrx/signals';
import { Subject, of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotEventsSocket } from '@dotcms/data-access';

import { withWebSocket } from './with-websocket.feature';

describe('withWebSocket Feature', () => {
    const TestStore = signalStore(withState({}), withWebSocket());

    let store: InstanceType<typeof TestStore>;
    let statusSubject: Subject<'connecting' | 'reconnecting' | 'connected' | 'closed'>;
    let mockEventsSocket: jest.Mocked<Partial<DotEventsSocket>>;

    beforeEach(() => {
        statusSubject = new Subject();

        mockEventsSocket = {
            connect: jest.fn().mockReturnValue(of({})),
            status$: jest.fn().mockReturnValue(statusSubject.asObservable()),
            on: jest.fn().mockReturnValue(new Subject()),
            destroy: jest.fn()
        };

        TestBed.configureTestingModule({
            providers: [
                TestStore,
                { provide: DotEventsSocket, useValue: mockEventsSocket }
            ]
        });

        store = TestBed.inject(TestStore);
    });

    it('should initialize with connecting status', () => {
        expect(store.wsStatus()).toBe('connecting');
    });

    it('should call connect and trackStatus on init', () => {
        expect(mockEventsSocket.connect).toHaveBeenCalled();
        expect(mockEventsSocket.status$).toHaveBeenCalled();
    });

    it('should update wsStatus to connected when socket connects', () => {
        statusSubject.next('connected');
        expect(store.wsStatus()).toBe('connected');
    });

    it('should update wsStatus to reconnecting when socket reconnects', () => {
        statusSubject.next('connected');
        statusSubject.next('reconnecting');
        expect(store.wsStatus()).toBe('reconnecting');
    });

    it('should update wsStatus to closed when socket closes', () => {
        statusSubject.next('closed');
        expect(store.wsStatus()).toBe('closed');
    });

    it('should set wsStatus to closed on connect error', () => {
        mockEventsSocket.connect = jest.fn().mockReturnValue(throwError(() => new Error('fail')));

        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [
                TestStore,
                { provide: DotEventsSocket, useValue: mockEventsSocket }
            ]
        });

        store = TestBed.inject(TestStore);
        expect(store.wsStatus()).toBe('closed');
    });

    it('should call destroy on store destroy', () => {
        TestBed.resetTestingModule();
        expect(mockEventsSocket.destroy).toHaveBeenCalled();
    });
});
