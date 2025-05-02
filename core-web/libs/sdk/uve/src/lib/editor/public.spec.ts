import { Contentlet, DotCMSUVEAction } from '@dotcms/types';
import { DotCMSReorderMenuConfig } from '@dotcms/types/internal';

import { sendMessageToUVE, editContentlet, reorderMenu, initUVE } from './public';

import * as utils from '../../script/utils';

describe('UVE Public Functions', () => {
    let postMessageSpy: jest.SpyInstance;

    beforeEach(() => {
        // Mock window.parent.postMessage
        postMessageSpy = jest.spyOn(window.parent, 'postMessage');

        // Mock all utility functions
        jest.spyOn(utils, 'scrollHandler').mockImplementation(() => ({
            destroyScrollHandler: jest.fn()
        }));
        jest.spyOn(utils, 'addClassToEmptyContentlets').mockImplementation();
        jest.spyOn(utils, 'listenBlockEditorInlineEvent').mockImplementation(() => ({
            destroyListenBlockEditorInlineEvent: jest.fn()
        }));
        jest.spyOn(utils, 'setClientIsReady').mockImplementation();
        jest.spyOn(utils, 'registerUVEEvents').mockReturnValue({
            subscriptions: [
                { unsubscribe: jest.fn(), event: 'test1' },
                { unsubscribe: jest.fn(), event: 'test2' }
            ]
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
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
            const setClientIsReadySpy = jest.spyOn(utils, 'setClientIsReady');
            initUVE();
            expect(setClientIsReadySpy).toHaveBeenCalledWith({});
        });

        it('should call setClientIsReady with graphql config when provided', () => {
            const setClientIsReadySpy = jest.spyOn(utils, 'setClientIsReady');
            const config = { graphql: { query: '{ test }', variables: {} } };
            initUVE(config);
            expect(setClientIsReadySpy).toHaveBeenCalledWith(config);
        });

        it('should call setClientIsReady with params config when provided', () => {
            const setClientIsReadySpy = jest.spyOn(utils, 'setClientIsReady');
            const config = { params: { depth: '1' } };
            initUVE(config);
            expect(setClientIsReadySpy).toHaveBeenCalledWith(config);
        });

        it('should return destroy function that unsubscribes all subscriptions', () => {
            // Create spy functions for unsubscribe
            const unsubscribeSpy1 = jest.fn();
            const unsubscribeSpy2 = jest.fn();
            const destroyScrollHandler = jest.fn();
            const destroyListenBlockEditorInlineEvent = jest.fn();

            // Mock registerUVEEvents with these spy functions
            jest.spyOn(utils, 'registerUVEEvents').mockReturnValue({
                subscriptions: [
                    { unsubscribe: unsubscribeSpy1, event: 'test1' },
                    { unsubscribe: unsubscribeSpy2, event: 'test2' }
                ]
            });

            jest.spyOn(utils, 'scrollHandler').mockReturnValue({
                destroyScrollHandler
            });

            jest.spyOn(utils, 'listenBlockEditorInlineEvent').mockReturnValue({
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
            jest.spyOn(utils, 'registerUVEEvents').mockReturnValue({ subscriptions: [] });

            const { destroyUVESubscriptions } = initUVE();

            expect(() => destroyUVESubscriptions()).not.toThrow();
        });
    });
});
