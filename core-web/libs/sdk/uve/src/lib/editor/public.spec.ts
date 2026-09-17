import { MockInstance, vi } from 'vitest';

import {
    Contentlet,
    DotCMSUVEAction,
    DotCMSBasicContentlet,
    DotCMSPageResponse
} from '@dotcms/types';
import { DotCMSReorderMenuConfig } from '@dotcms/types/internal';

import {
    sendMessageToUVE,
    editContentlet,
    reorderMenu,
    initUVE,
    enableBlockEditorInline,
    createContentlet
} from './public';

import * as utils from '../../script/utils';

interface TestContentlet extends DotCMSBasicContentlet {
    testField: string;
}

describe('UVE Public Functions', () => {
    let postMessageSpy: MockInstance;

    beforeEach(() => {
        // Mock window.parent.postMessage
        postMessageSpy = vi.spyOn(window.parent, 'postMessage');

        // Mock all utility functions
        vi.spyOn(utils, 'scrollHandler').mockImplementation(() => ({
            destroyScrollHandler: vi.fn()
        }));
        vi.spyOn(utils, 'addClassToEmptyContentlets').mockImplementation(() => undefined);
        vi.spyOn(utils, 'listenBlockEditorInlineEvent').mockImplementation(() => ({
            destroyListenBlockEditorInlineEvent: vi.fn()
        }));
        vi.spyOn(utils, 'setClientIsReady').mockImplementation(() => undefined);
        vi.spyOn(utils, 'registerUVEEvents').mockReturnValue({
            subscriptions: [
                { unsubscribe: vi.fn(), event: 'test1' },
                { unsubscribe: vi.fn(), event: 'test2' }
            ]
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('sendMessageToUVE', () => {
        it('should send message to parent window', () => {
            const message = {
                action: DotCMSUVEAction.EDIT_CONTENTLET,
                payload: { data: 'test' }
            };

            sendMessageToUVE(message);

            expect(postMessageSpy).toHaveBeenCalledWith(message, '*');
        });
    });

    describe('editContentlet', () => {
        it('should send edit contentlet message to UVE', () => {
            const contentlet = {
                identifier: '123',
                inode: '456',
                languageId: 1
            };

            editContentlet(contentlet as Contentlet<typeof contentlet>);

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.EDIT_CONTENTLET,
                    payload: contentlet
                },
                '*'
            );
        });
    });

    describe('reorderMenu', () => {
        it('should send reorder menu message with default values when no config provided', () => {
            reorderMenu();

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.REORDER_MENU,
                    payload: { startLevel: 1, depth: 2 }
                },
                '*'
            );
        });

        it('should send reorder menu message with custom config', () => {
            const config = { startLevel: 2, depth: 3 };
            reorderMenu(config);

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.REORDER_MENU,
                    payload: config
                },
                '*'
            );
        });

        it('should use default values for missing config properties', () => {
            reorderMenu({ startLevel: 2 } as DotCMSReorderMenuConfig);

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.REORDER_MENU,
                    payload: { startLevel: 2, depth: 2 }
                },
                '*'
            );
        });
    });

    describe('initUVE', () => {
        it('should initialize all required handlers and listeners', () => {
            initUVE();

            expect(utils.scrollHandler).toHaveBeenCalled();
            expect(utils.addClassToEmptyContentlets).toHaveBeenCalled();
            expect(utils.listenBlockEditorInlineEvent).toHaveBeenCalled();
            expect(utils.setClientIsReady).toHaveBeenCalled();
            expect(utils.registerUVEEvents).toHaveBeenCalled();
        });

        it('should call setClientIsReady with empty config when no config is provided', () => {
            const setClientIsReadySpy = vi.spyOn(utils, 'setClientIsReady');
            initUVE();
            expect(setClientIsReadySpy).toHaveBeenCalledWith({});
        });

        it('should call setClientIsReady with graphql config when provided', () => {
            const setClientIsReadySpy = vi.spyOn(utils, 'setClientIsReady');
            const config = { graphql: { query: '{ test }', variables: {} } } as DotCMSPageResponse;
            initUVE(config);
            expect(setClientIsReadySpy).toHaveBeenCalledWith(config);
        });

        it('should call setClientIsReady with params config when provided', () => {
            const setClientIsReadySpy = vi.spyOn(utils, 'setClientIsReady');
            const config = { params: { depth: '1' } } as unknown as DotCMSPageResponse;
            initUVE(config);
            expect(setClientIsReadySpy).toHaveBeenCalledWith(config);
        });

        it('should return destroy function that unsubscribes all subscriptions', () => {
            // Create spy functions for unsubscribe
            const unsubscribeSpy1 = vi.fn();
            const unsubscribeSpy2 = vi.fn();
            const destroyScrollHandler = vi.fn();
            const destroyListenBlockEditorInlineEvent = vi.fn();

            // Mock registerUVEEvents with these spy functions
            vi.spyOn(utils, 'registerUVEEvents').mockReturnValue({
                subscriptions: [
                    { unsubscribe: unsubscribeSpy1, event: 'test1' },
                    { unsubscribe: unsubscribeSpy2, event: 'test2' }
                ]
            });

            vi.spyOn(utils, 'scrollHandler').mockReturnValue({
                destroyScrollHandler
            });

            vi.spyOn(utils, 'listenBlockEditorInlineEvent').mockReturnValue({
                destroyListenBlockEditorInlineEvent
            });

            const { destroyUVESubscriptions } = initUVE();

            destroyUVESubscriptions();

            expect(unsubscribeSpy1).toHaveBeenCalled();
            expect(unsubscribeSpy2).toHaveBeenCalled();
            expect(destroyScrollHandler).toHaveBeenCalled();
            expect(destroyListenBlockEditorInlineEvent).toHaveBeenCalled();
        });

        it('should handle empty subscriptions array', () => {
            vi.spyOn(utils, 'registerUVEEvents').mockReturnValue({ subscriptions: [] });

            const { destroyUVESubscriptions } = initUVE();

            expect(() => destroyUVESubscriptions()).not.toThrow();
        });
    });

    describe('createContentlet', () => {
        it('should send create contentlet message to UVE with the given content type', () => {
            createContentlet('Event');

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.CREATE_CONTENTLET,
                    payload: { contentType: 'Event' }
                },
                '*'
            );
        });

        it('should send create contentlet message with a content type variable', () => {
            createContentlet('dotAsset');

            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.CREATE_CONTENTLET,
                    payload: { contentType: 'dotAsset' }
                },
                '*'
            );
        });
    });

    describe('enableBlockEditorInline', () => {
        let consoleErrorSpy: MockInstance;

        beforeEach(() => {
            consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        });

        afterEach(() => {
            consoleErrorSpy.mockRestore();
        });

        it('should call initInlineEditing with correct parameters when contentlet has the specified field', () => {
            const contentlet = {
                inode: '123',
                languageId: 1,
                identifier: 'test-identifier',
                contentType: 'test-content-type',
                testField: 'test content'
            } as unknown as TestContentlet;

            const fieldName = 'testField';

            const expectedPayload = {
                type: 'BLOCK_EDITOR',
                data: {
                    fieldName,
                    inode: contentlet.inode,
                    language: contentlet.languageId,
                    contentType: contentlet.contentType,
                    content: contentlet.testField
                }
            };

            enableBlockEditorInline(contentlet, fieldName);
            expect(postMessageSpy).toHaveBeenCalledWith(
                {
                    action: DotCMSUVEAction.INIT_INLINE_EDITING,
                    payload: expectedPayload
                },
                '*'
            );
        });

        it('should log error and not call initInlineEditing when contentlet does not have the specified field', () => {
            const contentlet = {
                inode: '123',
                languageId: 1,
                identifier: 'test-identifier',
                contentType: 'test-content-type'
            } as unknown as DotCMSBasicContentlet;

            const fieldName = 'nonExistentField';

            enableBlockEditorInline(contentlet, fieldName as keyof DotCMSBasicContentlet);

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                `Contentlet ${contentlet.identifier} does not have field ${fieldName}`
            );
            expect(postMessageSpy).not.toHaveBeenCalled();
        });

        it('should handle undefined contentlet gracefully', () => {
            const contentlet = undefined;
            const fieldName = 'testField';

            enableBlockEditorInline(
                contentlet as unknown as DotCMSBasicContentlet,
                fieldName as keyof DotCMSBasicContentlet
            );

            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(postMessageSpy).not.toHaveBeenCalled();
        });
    });
});
