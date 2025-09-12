/**
 * Creates a fake event for testing purposes.
 * This is a Jest-compatible replacement for @ngneat/spectator's createFakeEvent.
 *
 * @param eventType - The type of event to create (e.g., 'click', 'input', 'change')
 * @param eventInit - Optional event initialization properties
 * @returns A fake event object that can be used in tests
 */
export function createFakeEvent(eventType: string, eventInit?: EventInit): Event {
    // For common UI events, use more specific event constructors
    switch (eventType.toLowerCase()) {
        case 'click':
        case 'dblclick':
        case 'mousedown':
        case 'mouseup':
        case 'mouseover':
        case 'mouseout':
        case 'mousemove':
            return new MouseEvent(eventType, {
                bubbles: true,
                cancelable: true,
                view: window,
                ...eventInit
            });

        case 'keydown':
        case 'keyup':
        case 'keypress':
            return new KeyboardEvent(eventType, {
                bubbles: true,
                cancelable: true,
                view: window,
                ...eventInit
            });

        case 'focus':
        case 'blur':
        case 'focusin':
        case 'focusout':
            return new FocusEvent(eventType, {
                bubbles: true,
                cancelable: true,
                view: window,
                ...eventInit
            });

        case 'input':
        case 'change':
            return new Event(eventType, {
                bubbles: true,
                cancelable: true,
                ...eventInit
            });

        default:
            return new Event(eventType, {
                bubbles: true,
                cancelable: true,
                ...eventInit
            });
    }
}

/**
 * Creates a fake mouse event with specific coordinates.
 * Useful for testing drag & drop, click positioning, etc.
 */
export function createFakeMouseEvent(
    eventType: string,
    options: {
        clientX?: number;
        clientY?: number;
        screenX?: number;
        screenY?: number;
        button?: number;
        buttons?: number;
    } = {}
): MouseEvent {
    return new MouseEvent(eventType, {
        bubbles: true,
        cancelable: true,
        view: window,
        clientX: options.clientX || 0,
        clientY: options.clientY || 0,
        screenX: options.screenX || 0,
        screenY: options.screenY || 0,
        button: options.button || 0,
        buttons: options.buttons || 0
    });
}

/**
 * Creates a fake keyboard event with specific key information.
 * Useful for testing keyboard shortcuts, form inputs, etc.
 */
export function createFakeKeyboardEvent(
    eventType: string,
    options: {
        key?: string;
        code?: string;
        keyCode?: number;
        ctrlKey?: boolean;
        shiftKey?: boolean;
        altKey?: boolean;
        metaKey?: boolean;
    } = {}
): KeyboardEvent {
    return new KeyboardEvent(eventType, {
        bubbles: true,
        cancelable: true,
        view: window,
        key: options.key || '',
        code: options.code || '',
        keyCode: options.keyCode || 0,
        ctrlKey: options.ctrlKey || false,
        shiftKey: options.shiftKey || false,
        altKey: options.altKey || false,
        metaKey: options.metaKey || false
    });
}
