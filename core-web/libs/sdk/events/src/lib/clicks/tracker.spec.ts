import { vi } from 'vitest';

import { CLICK_EVENT_TYPE, DEFAULT_CLICK_THROTTLE_MS } from './constants';
import { DotCMSClickTracker } from './tracker';

import { CONTENTLET_CLASS } from '../contentlets/constants';
import * as coreUtils from '../pipeline/utils';

import type { DotCMSContentClickPayload, PipelineConfig } from '../pipeline/models';
import type { Mock } from 'vitest';

vi.mock('../pipeline/utils', async () => {
    const actual = (await vi.importActual('../pipeline/utils')) as Record<string, unknown>;
    return {
        ...actual,
        createPluginLogger: vi.fn(() => ({
            debug: vi.fn(),
            info: vi.fn(),
            warn: vi.fn(),
            error: vi.fn(),
            log: vi.fn()
        })),
        isBrowser: vi.fn(() => true)
    };
});

describe('DotCMSClickTracker', () => {
    const config: PipelineConfig = {
        server: 'https://test.com',
        siteAuth: 'test-key',
        debug: false
    };

    let tracker: DotCMSClickTracker;
    let clicked: Mock;

    /** A contentlet as the renderers print it, with a link inside by default */
    const contentlet = (identifier: string, inner = '<a href="#detail">View detail</a>') => {
        const element = document.createElement('div');
        element.className = CONTENTLET_CLASS;
        element.dataset['dotIdentifier'] = identifier;
        element.dataset['dotInode'] = `inode-${identifier}`;
        element.dataset['dotType'] = 'Blog';
        element.dataset['dotTitle'] = `Title ${identifier}`;
        element.dataset['dotBasetype'] = 'CONTENT';
        element.innerHTML = inner;

        return element;
    };

    const click = (target: Element) =>
        target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    const payloads = () =>
        clicked.mock.calls.map(([, payload]) => payload as DotCMSContentClickPayload);

    const identifiers = () => payloads().map((payload) => payload.content.identifier);

    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        (coreUtils.isBrowser as Mock).mockReturnValue(true);
        document.body.innerHTML = '';
        clicked = vi.fn();
        tracker = new DotCMSClickTracker(config);
        tracker.onClick(clicked);
    });

    afterEach(() => {
        tracker.cleanup();
        vi.useRealTimers();
        document.body.innerHTML = '';
    });

    it('listens once, on the document, in the capture phase', () => {
        const addEventListener = vi.spyOn(document, 'addEventListener');

        tracker.initialize();

        expect(addEventListener).toHaveBeenCalledWith('click', expect.any(Function), true);

        addEventListener.mockRestore();
    });

    it('reports a click on a link inside a contentlet', () => {
        const element = contentlet('blog-1');
        document.body.appendChild(element);
        tracker.initialize();

        click(element.querySelector('a')!);

        expect(clicked).toHaveBeenCalledTimes(1);
        expect(clicked).toHaveBeenCalledWith(
            CLICK_EVENT_TYPE,
            expect.objectContaining({
                content: expect.objectContaining({ identifier: 'blog-1', title: 'Title blog-1' }),
                element: expect.objectContaining({ type: 'a', href: '#detail' })
            })
        );
    });

    it('counts a contentlet added after initialize, with no scan to wait for', () => {
        tracker.initialize();

        const element = contentlet('later', '<button type="button">Buy</button>');
        document.body.appendChild(element);
        click(element.querySelector('button')!);

        expect(identifiers()).toEqual(['later']);
    });

    it('ignores clicks outside contentlets, and clicks in one that are not on a link or button', () => {
        const outside = document.createElement('a');
        outside.href = '#outside';
        const element = contentlet('blog-1', '<p>Just text</p><a href="#detail">View detail</a>');
        document.body.append(outside, element);
        tracker.initialize();

        click(outside);
        click(element.querySelector('p')!);

        expect(clicked).not.toHaveBeenCalled();
    });

    it('counts a click whose own handler stops its propagation', () => {
        const element = contentlet('blog-1');
        document.body.appendChild(element);
        element.querySelector('a')!.addEventListener('click', (event) => event.stopPropagation());
        tracker.initialize();

        click(element.querySelector('a')!);

        expect(identifiers()).toEqual(['blog-1']);
    });

    it('counts a click inside nested contentlets once, for the innermost', () => {
        const outer = contentlet('outer', '');
        const inner = contentlet('inner');
        outer.appendChild(inner);
        document.body.appendChild(outer);
        tracker.initialize();

        click(inner.querySelector('a')!);

        expect(identifiers()).toEqual(['inner']);
    });

    it('reports the position the contentlet has when it is clicked', () => {
        const first = contentlet('first');
        const second = contentlet('second');
        document.body.append(first, second);
        tracker.initialize();
        vi.advanceTimersByTime(1000);

        // A contentlet rendered later, before the clicked one
        document.body.insertBefore(contentlet('inserted'), second);
        click(second.querySelector('a')!);

        expect(payloads()[0]?.position.dom_index).toBe(2);
    });

    it('writes nothing to the contentlets', () => {
        const element = contentlet('blog-1');
        document.body.appendChild(element);
        const attributesBefore = element.getAttributeNames();
        tracker.initialize();
        vi.advanceTimersByTime(1000);

        click(element.querySelector('a')!);

        expect(element.getAttributeNames()).toEqual(attributesBefore);
    });

    it('throttles each contentlet on its own', () => {
        const first = contentlet('first');
        const second = contentlet('second');
        document.body.append(first, second);
        tracker.initialize();

        click(first.querySelector('a')!);
        click(first.querySelector('a')!);
        click(second.querySelector('a')!);
        vi.advanceTimersByTime(DEFAULT_CLICK_THROTTLE_MS + 1);
        click(first.querySelector('a')!);

        expect(identifiers()).toEqual(['first', 'second', 'first']);
    });

    it('notifies every subscriber, and stops notifying one that unsubscribed', () => {
        const element = contentlet('blog-1');
        document.body.appendChild(element);
        const other = vi.fn();
        const subscription = tracker.onClick(other);
        tracker.initialize();

        click(element.querySelector('a')!);
        subscription.unsubscribe();
        vi.advanceTimersByTime(DEFAULT_CLICK_THROTTLE_MS + 1);
        click(element.querySelector('a')!);

        expect(clicked).toHaveBeenCalledTimes(2);
        expect(other).toHaveBeenCalledTimes(1);
    });

    it('stops listening on cleanup', () => {
        const element = contentlet('blog-1');
        document.body.appendChild(element);
        const removeEventListener = vi.spyOn(document, 'removeEventListener');
        tracker.initialize();

        tracker.cleanup();
        click(element.querySelector('a')!);

        expect(clicked).not.toHaveBeenCalled();
        expect(removeEventListener).toHaveBeenCalledWith('click', expect.any(Function), true);

        removeEventListener.mockRestore();
    });

    it('does nothing without a document, as on the server', () => {
        (coreUtils.isBrowser as Mock).mockReturnValue(false);
        const addEventListener = vi.spyOn(document, 'addEventListener');

        tracker.initialize();

        expect(addEventListener).not.toHaveBeenCalledWith('click', expect.any(Function), true);

        addEventListener.mockRestore();
    });
});
