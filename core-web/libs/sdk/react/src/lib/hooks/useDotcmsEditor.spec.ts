import { renderHook } from '@testing-library/react-hooks';

import * as sdkClient from '@dotcms/client';

import { useDotcmsEditor } from './useDotcmsEditor';

import { DotcmsPageProps } from '../components/DotcmsLayout/DotcmsLayout';
import { mockPageContext } from '../mocks/mockPageContext';

jest.mock('@dotcms/client', () => ({
    ...jest.requireActual('@dotcms/client'),
    isInsideEditor: () => true,
    postMessageToEditor: jest.fn(),
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

const { DotCmsClient } = sdkClient as jest.Mocked<typeof sdkClient>;

describe('useDotcmsEditor', () => {
    let isInsideEditorSpy: jest.SpyInstance<boolean>;
    let initEditorSpy: jest.SpyInstance<void>;
    let destroyEditorSpy: jest.SpyInstance<void>;

    beforeEach(() => {
        isInsideEditorSpy = jest.spyOn(sdkClient, 'isInsideEditor');
        initEditorSpy = jest.spyOn(sdkClient, 'initEditor');
        destroyEditorSpy = jest.spyOn(sdkClient, 'destroyEditor');
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
            });

            it('should subscribe to the `CHANGE` event', () => {
                const client = DotCmsClient.instance;

                renderHook(() => useDotcmsEditor(dotCMSPagePropsMock));

                expect(client.editor.on).toHaveBeenCalledWith('changes', expect.any(Function));
            });

            it('should remove listener on unmount', () => {
                const client = DotCmsClient.instance;

                const { unmount } = renderHook(() => useDotcmsEditor(dotCMSPagePropsMock));

                unmount();

                expect(client.editor.off).toHaveBeenCalledWith('changes');
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
                    action: sdkClient.CUSTOMER_ACTIONS.CLIENT_READY,
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

            it('should update the page asset when changes are made in the editor', () => {
                const client = DotCmsClient.instance;

                renderHook(() => useDotcmsEditor(dotCMSPagePropsMock as DotcmsPageProps));

                expect(client.editor.on).toHaveBeenCalledWith('changes', expect.any(Function));
            });
        });
    });
});
