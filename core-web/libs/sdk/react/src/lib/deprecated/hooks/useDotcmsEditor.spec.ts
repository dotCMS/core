import { renderHook } from '@testing-library/react-hooks';

import * as sdkClient from '@dotcms/client';
import * as sdkUVE from '@dotcms/uve';
import { UVE_MODE, UVEState, UVEEventSubscription } from '@dotcms/uve/types';

import { useDotcmsEditor } from './useDotcmsEditor';

import { DotcmsPageProps } from '../components/DotcmsLayout/DotcmsLayout';
import { mockPageContext } from '../mocks/mockPageContext';

jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: jest.fn().mockReturnValue(true),
    postMessageToEditor: jest.fn(),
    initEditor: jest.fn(),
    destroyEditor: jest.fn(),
    DotCmsClient: {
        instance: {
            editor: {
                on: jest.fn(),
                off: jest.fn(),
                callbacks: {}
            }
        }
    }
}));

jest.mock('@dotcms/uve', () => ({
    ...jest.requireActual('@dotcms/uve'),
    getUVEState: jest.fn().mockReturnValue({
        mode: 'EDIT_MODE',
        persona: null,
        variantName: null,
        experimentId: null,
        publishDate: null,
        languageId: null,
        dotCMSHost: null
    }),
    createUVESubscription: jest.fn().mockReturnValue({
        event: 'changes',
        unsubscribe: jest.fn()
    })
}));

describe('useDotcmsEditor', () => {
    let isInsideEditorSpy: jest.SpyInstance<boolean>;
    let getUVEStateSpy: jest.SpyInstance<UVEState | undefined>;
    let createUVESubscriptionSpy: jest.SpyInstance<UVEEventSubscription>;
    let initEditorSpy: jest.SpyInstance<void>;
    let destroyEditorSpy: jest.SpyInstance<void>;

    beforeEach(() => {
        isInsideEditorSpy = jest.spyOn(sdkClient, 'isInsideEditor');
        initEditorSpy = jest.spyOn(sdkClient, 'initEditor');
        destroyEditorSpy = jest.spyOn(sdkClient, 'destroyEditor');
        getUVEStateSpy = jest.spyOn(sdkUVE, 'getUVEState');
        createUVESubscriptionSpy = jest.spyOn(sdkUVE, 'createUVESubscription');
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('when outside editor', () => {
        it('should not call initEditor or destroyEditor when outside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(false);

            renderHook(() =>
                useDotcmsEditor({
                    pageContext: mockPageContext,
                    config: { pathname: '' }
                })
            );

            expect(initEditorSpy).not.toHaveBeenCalled();
            expect(destroyEditorSpy).not.toHaveBeenCalled();
        });
    });

    describe('when inside editor', () => {
        it('should call initEditor when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);
            getUVEStateSpy.mockReturnValueOnce({
                mode: UVE_MODE.EDIT,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null,
                languageId: null,
                dotCMSHost: null
            });

            renderHook(() =>
                useDotcmsEditor({
                    pageContext: mockPageContext,
                    config: { pathname: '' }
                })
            );

            expect(initEditorSpy).toHaveBeenCalled();
        });

        it('should call destroyEditor on unmount when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);
            getUVEStateSpy.mockReturnValueOnce({
                mode: UVE_MODE.EDIT,
                persona: null,
                variantName: null,
                experimentId: null,
                publishDate: null,
                languageId: null,
                dotCMSHost: null
            });

            const { unmount } = renderHook(() =>
                useDotcmsEditor({
                    pageContext: mockPageContext,
                    config: { pathname: '' }
                })
            );

            unmount();

            expect(destroyEditorSpy).toHaveBeenCalled();
        });

        describe('onReload', () => {
            const dotCMSPagePropsMock = {
                pageContext: mockPageContext,
                config: {
                    pathname: '',
                    onReload: () => {
                        /* do nothing */
                    }
                }
            };

            beforeEach(() => {
                isInsideEditorSpy.mockReturnValueOnce(true);
                getUVEStateSpy.mockReturnValueOnce({
                    mode: UVE_MODE.EDIT,
                    persona: null,
                    variantName: null,
                    experimentId: null,
                    publishDate: null,
                    languageId: null,
                    dotCMSHost: null
                });
            });

            it('should subscribe to the `CHANGE` event', () => {
                renderHook(() => useDotcmsEditor(dotCMSPagePropsMock));

                expect(sdkUVE.createUVESubscription).toHaveBeenCalledWith(
                    'changes',
                    expect.any(Function)
                );
            });

            it('should remove listener on unmount', () => {
                const removeListenerSpy = jest.fn();
                createUVESubscriptionSpy.mockReturnValue({
                    event: 'changes',
                    unsubscribe: removeListenerSpy
                });

                const { unmount } = renderHook(() => useDotcmsEditor(dotCMSPagePropsMock));

                unmount();

                expect(removeListenerSpy).toHaveBeenCalled();
            });
        });

        describe('Client is ready', () => {
            const dotCMSPagePropsMock = {
                pageContext: mockPageContext,
                config: {
                    pathname: '',
                    onReload: () => {
                        /* do nothing */
                    }
                }
            };

            it('should send a message to the editor when the client is ready', () => {
                const editor: sdkClient.EditorConfig = {
                    params: {
                        depth: '0'
                    }
                };

                renderHook(() =>
                    useDotcmsEditor({
                        ...dotCMSPagePropsMock,
                        config: {
                            pathname: '',
                            editor
                        }
                    })
                );

                expect(sdkClient.postMessageToEditor).toHaveBeenCalledWith({
                    action: sdkClient.CLIENT_ACTIONS.CLIENT_READY,
                    payload: editor
                });
            });
        });

        describe('onChange', () => {
            const dotCMSPagePropsMock = {
                pageContext: mockPageContext,
                config: {
                    pathname: '',
                    editor: {
                        query: '{query { asset { identifier } }}'
                    }
                }
            };

            beforeEach(() => {
                isInsideEditorSpy.mockReturnValueOnce(true);
                getUVEStateSpy.mockReturnValueOnce({
                    mode: UVE_MODE.EDIT,
                    persona: null,
                    variantName: null,
                    experimentId: null,
                    publishDate: null,
                    languageId: null,
                    dotCMSHost: null
                });
            });

            it('should update the page asset when changes are made in the editor', () => {
                renderHook(() => useDotcmsEditor(dotCMSPagePropsMock as DotcmsPageProps));

                expect(sdkUVE.createUVESubscription).toHaveBeenCalledWith(
                    'changes',
                    expect.any(Function)
                );
            });
        });
    });
});
