import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import { UveIframeMessengerService, IframeMessage } from './uve-iframe-messenger.service';

describe('UveIframeMessengerService', () => {
    let spectator: SpectatorService<UveIframeMessengerService>;
    let service: UveIframeMessengerService;
    let mockIframeWindow: Window;

    const createService = createServiceFactory({
        service: UveIframeMessengerService
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;

        // Create a mock iframe window with postMessage spy
        mockIframeWindow = {
            postMessage: jest.fn()
        } as unknown as Window;

        // Mock console.warn to avoid noise in tests
        jest.spyOn(console, 'warn').mockImplementation(jest.fn());
    });

    describe('setIframeWindow and getIframeWindow', () => {
        it('should set and get iframe window', () => {
            expect(service.getIframeWindow()).toBeNull();

            service.setIframeWindow(mockIframeWindow);
            expect(service.getIframeWindow()).toBe(mockIframeWindow);

            service.setIframeWindow(null);
            expect(service.getIframeWindow()).toBeNull();
        });
    });

    describe('sendPostMessage', () => {
        it('should send postMessage to iframe when window is set', () => {
            service.setIframeWindow(mockIframeWindow);
            const message: IframeMessage = {
                name: 'TEST_EVENT',
                payload: { test: 'data' }
            };

            service.sendPostMessage(message);

            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(message, '*');
            expect(console.warn).not.toHaveBeenCalled();
        });

        it('should warn when iframe window is not set', () => {
            const message: IframeMessage = {
                name: 'TEST_EVENT',
                payload: { test: 'data' }
            };

            service.sendPostMessage(message);

            expect(console.warn).toHaveBeenCalledWith(
                'Iframe window not set. Cannot send message:',
                message
            );
        });
    });

    describe('sendPageData', () => {
        it('should send page data message to iframe', () => {
            service.setIframeWindow(mockIframeWindow);
            const payload = { pageId: '123', data: 'test' };

            service.sendPageData(payload);

            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
                    payload
                },
                '*'
            );
        });
    });

    describe('requestBounds', () => {
        it('should send request bounds message to iframe', () => {
            service.setIframeWindow(mockIframeWindow);

            service.requestBounds();

            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS
                },
                '*'
            );
        });
    });

    describe('reloadPage', () => {
        it('should send reload page message to iframe', () => {
            service.setIframeWindow(mockIframeWindow);

            service.reloadPage();

            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                },
                '*'
            );
        });
    });

    describe('scrollInsideIframe', () => {
        it('should send scroll direction message to iframe', () => {
            service.setIframeWindow(mockIframeWindow);

            service.scrollInsideIframe('up');
            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME,
                    direction: 'up'
                },
                '*'
            );

            service.scrollInsideIframe('down');
            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME,
                    direction: 'down'
                },
                '*'
            );
        });
    });

    describe('copyContentletInlineEditingSuccess', () => {
        it('should send copy contentlet success message to iframe', () => {
            service.setIframeWindow(mockIframeWindow);
            const payload = { contentletId: '123' };

            service.copyContentletInlineEditingSuccess(payload);

            expect(mockIframeWindow.postMessage).toHaveBeenCalledWith(
                {
                    name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                    payload
                },
                '*'
            );
        });
    });
});
