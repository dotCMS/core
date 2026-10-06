import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { CONTENTLET_CLASS, CONTENTLET_IDENTIFIER_ATTRIBUTE } from './constants';
import { createContentletObserver, createThrottle } from './utils';

describe('createThrottle', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    it('runs the first call at once and drops the calls made inside the window', () => {
        const callback = vi.fn();
        const throttled = createThrottle(callback, 250);

        throttled();
        throttled();
        vi.advanceTimersByTime(250);

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('with trailing, runs the calls made inside the window once, when it ends', () => {
        const callback = vi.fn();
        const throttled = createThrottle(callback, 250, { trailing: true });

        throttled();
        throttled();
        throttled();

        expect(callback).toHaveBeenCalledTimes(1);

        vi.advanceTimersByTime(250);

        expect(callback).toHaveBeenCalledTimes(2);
    });

    it('with trailing, runs a call made after the window at once', () => {
        const callback = vi.fn();
        const throttled = createThrottle(callback, 250, { trailing: true });

        throttled();
        vi.advanceTimersByTime(300);
        throttled();

        expect(callback).toHaveBeenCalledTimes(2);
    });
});

describe('createContentletObserver', () => {
    let observer: MutationObserver | null = null;

    // MutationObserver callbacks run as microtasks, which fake timers leave alone.
    const flushMutations = () => Promise.resolve();

    const addContentlet = (): HTMLElement => {
        const contentlet = document.createElement('div');
        contentlet.className = CONTENTLET_CLASS;
        document.body.appendChild(contentlet);

        return contentlet;
    };

    beforeEach(() => {
        vi.useFakeTimers();
        document.body.innerHTML = '';
    });

    afterEach(() => {
        observer?.disconnect();
        observer = null;
        document.body.innerHTML = '';
    });

    it('calls back when a contentlet is added', async () => {
        const callback = vi.fn();
        observer = createContentletObserver(callback);

        addContentlet();
        await flushMutations();

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('ignores a contentlet receiving its identifier by default', async () => {
        const contentlet = addContentlet();
        const callback = vi.fn();
        observer = createContentletObserver(callback);

        contentlet.setAttribute(CONTENTLET_IDENTIFIER_ATTRIBUTE, 'content-1');
        await flushMutations();

        expect(callback).not.toHaveBeenCalled();
    });

    it('with identifiers, calls back when a contentlet receives its identifier', async () => {
        const contentlet = addContentlet();
        const callback = vi.fn();
        observer = createContentletObserver(callback, 250, { identifiers: true });

        contentlet.setAttribute(CONTENTLET_IDENTIFIER_ATTRIBUTE, 'content-1');
        await flushMutations();

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('with identifiers, ignores the attribute on elements that are not contentlets', async () => {
        const element = document.createElement('div');
        document.body.appendChild(element);
        const callback = vi.fn();
        observer = createContentletObserver(callback, 250, { identifiers: true });

        element.setAttribute(CONTENTLET_IDENTIFIER_ATTRIBUTE, 'content-1');
        await flushMutations();

        expect(callback).not.toHaveBeenCalled();
    });

    it('with identifiers, scans again for an identifier that lands right after its contentlet', async () => {
        const callback = vi.fn();
        observer = createContentletObserver(callback, 250, { identifiers: true });

        const contentlet = addContentlet();
        await flushMutations();
        contentlet.setAttribute(CONTENTLET_IDENTIFIER_ATTRIBUTE, 'content-1');
        await flushMutations();

        expect(callback).toHaveBeenCalledTimes(1);

        vi.advanceTimersByTime(250);

        expect(callback).toHaveBeenCalledTimes(2);
    });
});
