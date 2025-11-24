/* eslint-disable @typescript-eslint/no-explicit-any */

import { handleContentletClick } from './dot-analytics.click.utils';

import {
    ANALYTICS_CONTENTLET_CLASS,
    CLICK_EVENT_TYPE
} from '../../shared/constants/dot-analytics.constants';
import { DotCMSContentClickPayload } from '../../shared/models';
import * as sharedUtils from '../../shared/utils/dot-analytics.utils';
import * as impressionUtils from '../impression/dot-analytics.impression.utils';

// Mock dependencies
jest.mock('../../shared/utils/dot-analytics.utils');
jest.mock('../impression/dot-analytics.impression.utils');

describe('Click Utils', () => {
    let mockLogger: ReturnType<typeof sharedUtils.createPluginLogger>;

    beforeEach(() => {
        jest.clearAllMocks();
        jest.useFakeTimers();

        mockLogger = {
            debug: jest.fn(),
            info: jest.fn(),
            warn: jest.fn(),
            error: jest.fn(),
            log: jest.fn(),
            group: jest.fn(),
            groupEnd: jest.fn(),
            time: jest.fn(),
            timeEnd: jest.fn()
        } as unknown as ReturnType<typeof sharedUtils.createPluginLogger>;

        // Mock getViewportMetrics
        (impressionUtils.getViewportMetrics as jest.Mock).mockReturnValue({
            offsetPercentage: 50
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('handleContentletClick()', () => {
        let trackCallback: jest.Mock;

        beforeEach(() => {
            trackCallback = jest.fn();

            // Mock extractContentletData
            (sharedUtils.extractContentletData as jest.Mock).mockReturnValue({
                identifier: 'test-123',
                inode: 'inode-456',
                title: 'Test Content',
                contentType: 'Blog'
            });
        });

        describe('Early returns - invalid clicks', () => {
            it('should return early if click target is not <a> or <button>', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const div = document.createElement('div');
                div.textContent = 'Not clickable';
                contentlet.appendChild(div);
                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: div });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).not.toHaveBeenCalled();
                document.body.removeChild(contentlet);
            });

            it('should return early if clickable is outside the contentlet boundary', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;
                document.body.appendChild(contentlet);

                // Button outside contentlet
                const button = document.createElement('button');
                button.textContent = 'Click me';
                document.body.appendChild(button);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).not.toHaveBeenCalled();
                document.body.removeChild(contentlet);
                document.body.removeChild(button);
            });

            it('should return early if contentlet has no identifier', () => {
                const contentlet = createContentletWithButton('test-123');
                document.body.appendChild(contentlet);

                // Mock no identifier
                (sharedUtils.extractContentletData as jest.Mock).mockReturnValue({
                    identifier: '',
                    inode: 'inode-456',
                    title: 'Test',
                    contentType: 'Blog'
                });

                const button = contentlet.querySelector('button') as HTMLElement;
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).not.toHaveBeenCalled();
                document.body.removeChild(contentlet);
            });
        });

        describe('Valid clicks', () => {
            it('should call trackCallback with correct payload for button click', () => {
                const contentlet = createContentletWithButton('test-123');
                document.body.appendChild(contentlet);

                const button = contentlet.querySelector('button') as HTMLElement;
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).toHaveBeenCalledWith(CLICK_EVENT_TYPE, {
                    content: {
                        identifier: 'test-123',
                        inode: 'inode-456',
                        title: 'Test Content',
                        content_type: 'Blog'
                    },
                    position: {
                        viewport_offset_pct: 50,
                        dom_index: 0
                    },
                    element: {
                        text: 'Click me',
                        type: 'button',
                        id: 'test-btn',
                        class: 'btn btn-primary',
                        href: '',
                        attributes: expect.any(Object)
                    }
                });

                document.body.removeChild(contentlet);
            });

            it('should call trackCallback with correct payload for link click', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const link = document.createElement('a');
                link.href = '/test-page';
                link.textContent = 'Read more';
                link.id = 'test-link';
                link.className = 'link-primary';
                contentlet.appendChild(link);

                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: link });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).toHaveBeenCalledWith(
                    CLICK_EVENT_TYPE,
                    expect.objectContaining({
                        element: expect.objectContaining({
                            text: 'Read more',
                            type: 'a',
                            id: 'test-link',
                            class: 'link-primary',
                            href: '/test-page' // Relative path as written in HTML
                        })
                    })
                );

                document.body.removeChild(contentlet);
            });

            it('should work when clicking child element inside button', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const button = document.createElement('button');
                const span = document.createElement('span');
                span.textContent = 'Click me';
                button.appendChild(span);
                contentlet.appendChild(button);

                document.body.appendChild(contentlet);

                // Click on span (child of button)
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: span });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(trackCallback).toHaveBeenCalled();
                document.body.removeChild(contentlet);
            });

            it('should include useful attributes (data-*, aria-*) and capture href as top-level', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const link = document.createElement('a');
                link.textContent = 'Click me';
                link.id = 'cta-signup'; // Captured as top-level
                link.className = 'btn btn-primary inline-block px-4'; // Captured as top-level
                link.href = '/signup'; // Captured as top-level (most important for analytics)
                link.setAttribute('data-category', 'primary-cta'); // Include in attributes
                link.setAttribute('data-testid', 'signup-button'); // Include in attributes
                link.setAttribute('data-dot-analytics-test', 'exclude-me'); // Exclude (internal)
                link.setAttribute('aria-label', 'Sign up now'); // Include in attributes
                link.setAttribute('target', '_blank'); // Include in attributes
                contentlet.appendChild(link);

                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: link });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                // Verify top-level properties (required by backend + href)
                expect(payload.element.id).toBe('cta-signup');
                expect(payload.element.class).toBe('btn btn-primary inline-block px-4');
                expect(payload.element.type).toBe('a');
                expect(payload.element.href).toBe('/signup'); // Relative path as written in HTML

                // Verify attributes include useful data
                expect(payload.element.attributes).toHaveProperty('data-category', 'primary-cta');
                expect(payload.element.attributes).toHaveProperty('data-testid', 'signup-button');
                expect(payload.element.attributes).toHaveProperty('aria-label', 'Sign up now');
                expect(payload.element.attributes).toHaveProperty('target', '_blank');

                // Verify excluded attributes (already captured as top-level or internal)
                expect(payload.element.attributes).not.toHaveProperty('class'); // Already top-level
                expect(payload.element.attributes).not.toHaveProperty('id'); // Already top-level
                expect(payload.element.attributes).not.toHaveProperty('href'); // Already top-level
                expect(payload.element.attributes).not.toHaveProperty('data-dot-analytics-test'); // Internal

                document.body.removeChild(contentlet);
            });

            it('should truncate element text to 100 characters', () => {
                const longText = 'A'.repeat(200); // 200 characters
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const button = document.createElement('button');
                button.textContent = longText;
                contentlet.appendChild(button);

                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                expect(payload.element.text).toHaveLength(100);
                expect(payload.element.text).toBe('A'.repeat(100));

                document.body.removeChild(contentlet);
            });

            it('should calculate correct dom_index', () => {
                // Create multiple contentlets
                const contentlet1 = createContentletWithButton('test-1');
                const contentlet2 = createContentletWithButton('test-2');
                const contentlet3 = createContentletWithButton('test-3');

                document.body.appendChild(contentlet1);
                document.body.appendChild(contentlet2);
                document.body.appendChild(contentlet3);

                // Click on button in second contentlet
                const button = contentlet2.querySelector('button') as HTMLElement;
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet2, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                expect(payload.position.dom_index).toBe(1); // Zero-indexed, so second = 1

                document.body.removeChild(contentlet1);
                document.body.removeChild(contentlet2);
                document.body.removeChild(contentlet3);
            });

            it('should use viewport metrics from getViewportMetrics', () => {
                const contentlet = createContentletWithButton('test-123');
                document.body.appendChild(contentlet);

                (impressionUtils.getViewportMetrics as jest.Mock).mockReturnValue({
                    offsetPercentage: 75
                });

                const button = contentlet.querySelector('button') as HTMLElement;
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                expect(payload.position.viewport_offset_pct).toBe(75);
                expect(impressionUtils.getViewportMetrics).toHaveBeenCalledWith(contentlet);

                document.body.removeChild(contentlet);
            });

            it('should handle button with no text gracefully', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const button = document.createElement('button');
                // No text content
                contentlet.appendChild(button);

                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                expect(payload.element.text).toBe('');
                document.body.removeChild(contentlet);
            });

            it('should use empty string when element has no id attribute', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const button = document.createElement('button');
                button.textContent = 'Click me';
                button.className = 'btn';
                // No id attribute
                contentlet.appendChild(button);

                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                const payload = trackCallback.mock.calls[0][1] as DotCMSContentClickPayload;

                expect(payload.element.id).toBe('');
                expect(payload.element.type).toBe('button');
                expect(payload.element.class).toBe('btn');

                document.body.removeChild(contentlet);
            });
        });

        describe('Debug logging', () => {
            it('should log debug messages using logger', () => {
                const contentlet = createContentletWithButton('test-123');
                document.body.appendChild(contentlet);

                const button = contentlet.querySelector('button') as HTMLElement;
                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(mockLogger.debug).toHaveBeenCalledWith(
                    'Click detected on:',
                    expect.anything()
                );
                expect(mockLogger.debug).toHaveBeenCalledWith(
                    'Found clickable element:',
                    expect.anything()
                );
                expect(mockLogger.debug).toHaveBeenCalledWith(
                    'Contentlet data:',
                    expect.anything()
                );

                document.body.removeChild(contentlet);
            });

            it('should log reason for early return (no clickable)', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;

                const div = document.createElement('div');
                div.textContent = 'Not clickable';
                contentlet.appendChild(div);
                document.body.appendChild(contentlet);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: div });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(mockLogger.debug).toHaveBeenCalledWith(
                    'No <a> or <button> found in click path'
                );

                document.body.removeChild(contentlet);
            });

            it('should log reason for early return (outside contentlet)', () => {
                const contentlet = document.createElement('div');
                contentlet.className = ANALYTICS_CONTENTLET_CLASS;
                document.body.appendChild(contentlet);

                // Button outside contentlet
                const button = document.createElement('button');
                button.textContent = 'Outside';
                document.body.appendChild(button);

                const event = new MouseEvent('click', { bubbles: true });
                Object.defineProperty(event, 'target', { value: button });

                handleContentletClick(event, contentlet, trackCallback, mockLogger);

                expect(mockLogger.debug).toHaveBeenCalledWith(
                    'Click was outside contentlet boundary'
                );

                document.body.removeChild(contentlet);
                document.body.removeChild(button);
            });
        });
    });

    // Helper function
    function createContentletWithButton(identifier: string): HTMLElement {
        const contentlet = document.createElement('div');
        contentlet.className = ANALYTICS_CONTENTLET_CLASS;
        contentlet.dataset.dotAnalyticsIdentifier = identifier;

        const button = document.createElement('button');
        button.textContent = 'Click me';
        button.id = 'test-btn';
        button.className = 'btn btn-primary';
        contentlet.appendChild(button);

        return contentlet;
    }
});
