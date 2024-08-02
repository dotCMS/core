import { renderHook } from '@testing-library/react-hooks';

import * as sdkClient from '@dotcms/client';

import { useDotcmsEditor } from './useDotcmsEditor';

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

            renderHook(() => useDotcmsEditor({ pathname: '' }));

            expect(initEditorSpy).not.toHaveBeenCalled();
            expect(destroyEditorSpy).not.toHaveBeenCalled();
        });
    });

    describe('when inside editor', () => {
        it('should call initEditor when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);

            renderHook(() => useDotcmsEditor({ pathname: '' }));

            expect(initEditorSpy).toHaveBeenCalled();
        });

        it('should call destroyEditor on unmount when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);

            const { unmount } = renderHook(() => useDotcmsEditor({ pathname: '' }));

            unmount();

            expect(destroyEditorSpy).toHaveBeenCalled();
        });

        describe('onReload', () => {
            beforeEach(() => {
                isInsideEditorSpy.mockReturnValueOnce(true);
            });

            it('should subscribe to the `CHANGE` event', () => {
                const client = DotCmsClient.instance;

                renderHook(() =>
                    useDotcmsEditor({
                        pathname: '',
                        onReload: () => {
                            /* do nothing */
                        }
                    })
                );

                expect(client.editor.on).toHaveBeenCalledWith('changes', expect.any(Function));
            });

            it('should remove listener on unmount', () => {
                const client = DotCmsClient.instance;

                const { unmount } = renderHook(() =>
                    useDotcmsEditor({
                        pathname: '',
                        onReload: () => {
                            /* do nothing */
                        }
                    })
                );

                unmount();

                expect(client.editor.off).toHaveBeenCalledWith('changes');
            });
        });
    });
});
