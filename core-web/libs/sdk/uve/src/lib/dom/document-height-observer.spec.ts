import { observeDocumentHeight } from './document-height-observer';

describe('observeDocumentHeight', () => {
    let onHeightChange: jest.Mock;
    let roObserveSpy: jest.Mock;
    let roDisconnectSpy: jest.Mock;
    let roCallback: ResizeObserverCallback;
    let moObserveSpy: jest.Mock;
    let moDisconnectSpy: jest.Mock;
    let moCallback: MutationCallback;

    beforeEach(() => {
        jest.useFakeTimers();

        onHeightChange = jest.fn();
        roObserveSpy = jest.fn();
        roDisconnectSpy = jest.fn();
        global.ResizeObserver = jest.fn((cb: ResizeObserverCallback) => {
            roCallback = cb;

            return { observe: roObserveSpy, disconnect: roDisconnectSpy, unobserve: jest.fn() };
        }) as unknown as typeof ResizeObserver;

        moObserveSpy = jest.fn();
        moDisconnectSpy = jest.fn();
        global.MutationObserver = jest.fn((cb: MutationCallback) => {
            moCallback = cb;

            return {
                observe: moObserveSpy,
                disconnect: moDisconnectSpy,
                takeRecords: jest.fn()
            };
        }) as unknown as typeof MutationObserver;

        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: jest.fn().mockReturnValue('complete')
        });

        Object.defineProperty(document.documentElement, 'offsetHeight', {
            configurable: true,
            get: jest.fn().mockReturnValue(1000)
        });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    const flushAll = () => {
        jest.advanceTimersByTime(50);
        jest.runAllTimers();
    };

    it('notifies with document.documentElement.offsetHeight after debounce and double rAF', () => {
        observeDocumentHeight({ onHeightChange });
        flushAll();

        expect(onHeightChange).toHaveBeenCalledWith(1000);
    });

    it('does not notify when offsetHeight is 0', () => {
        Object.defineProperty(document.documentElement, 'offsetHeight', {
            configurable: true,
            get: jest.fn().mockReturnValue(0)
        });

        observeDocumentHeight({ onHeightChange });
        flushAll();

        expect(onHeightChange).not.toHaveBeenCalled();
    });

    it('does not notify before the debounce fires', () => {
        observeDocumentHeight({ onHeightChange });

        jest.advanceTimersByTime(49);

        expect(onHeightChange).not.toHaveBeenCalled();
    });

    it('collapses rapid-fire observer callbacks into a single notification', () => {
        observeDocumentHeight({ onHeightChange });

        roCallback([], {} as ResizeObserver);
        moCallback([], {} as MutationObserver);
        roCallback([], {} as ResizeObserver);

        flushAll();

        expect(onHeightChange).toHaveBeenCalledTimes(1);
    });

    it('schedules again when observers fire after a settled notification', () => {
        observeDocumentHeight({ onHeightChange });
        flushAll();
        onHeightChange.mockClear();

        roCallback([], {} as ResizeObserver);
        flushAll();

        expect(onHeightChange).toHaveBeenCalledTimes(1);
    });

    it('does not notify again when the measured height did not change', () => {
        observeDocumentHeight({ onHeightChange });
        flushAll();
        onHeightChange.mockClear();

        roCallback([], {} as ResizeObserver);
        moCallback([], {} as MutationObserver);
        flushAll();

        expect(onHeightChange).not.toHaveBeenCalled();
    });

    it('waits for the load event when document is still loading', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: jest.fn().mockReturnValue('loading')
        });

        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        observeDocumentHeight({ onHeightChange });

        expect(addEventListenerSpy).toHaveBeenCalledWith('load', expect.any(Function));
        jest.runAllTimers();
        expect(onHeightChange).not.toHaveBeenCalled();
    });

    it('observes document.documentElement with ResizeObserver', () => {
        observeDocumentHeight({ onHeightChange });

        expect(roObserveSpy).toHaveBeenCalledWith(document.documentElement);
    });

    it('observes document.body with MutationObserver', () => {
        observeDocumentHeight({ onHeightChange });

        expect(moObserveSpy).toHaveBeenCalledWith(document.body, {
            childList: true,
            subtree: true
        });
    });

    it('destroys timers and observers', () => {
        const clearTimeoutSpy = jest.spyOn(global, 'clearTimeout');
        const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');
        const { destroy } = observeDocumentHeight({ onHeightChange });

        destroy();

        expect(clearTimeoutSpy).toHaveBeenCalled();
        expect(roDisconnectSpy).toHaveBeenCalled();
        expect(moDisconnectSpy).toHaveBeenCalled();
        expect(removeEventListenerSpy).toHaveBeenCalledWith('load', expect.any(Function));
    });

    it('ignores observer callbacks fired after destroy', () => {
        const { destroy } = observeDocumentHeight({ onHeightChange });

        destroy();
        roCallback([], {} as ResizeObserver);
        moCallback([], {} as MutationObserver);
        flushAll();

        expect(onHeightChange).not.toHaveBeenCalled();
    });
});
