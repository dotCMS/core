import { waitFor } from '@testing-library/react';
import { renderHook } from '@testing-library/react-hooks';
import { act } from 'react';

import * as dotcmsClient from '@dotcms/client';

import { useDotcmsEditor } from './useDotcmsEditor';

describe('useDotcmsEditor', () => {
    let isInsideEditorSpy: jest.SpyInstance<boolean>;
    let initEditorSpy: jest.SpyInstance<void>;
    let destroyEditorSpy: jest.SpyInstance<void>;

    beforeEach(() => {
        isInsideEditorSpy = jest.spyOn(dotcmsClient, 'isInsideEditor');
        initEditorSpy = jest.spyOn(dotcmsClient, 'initEditor');
        destroyEditorSpy = jest.spyOn(dotcmsClient, 'destroyEditor');
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('when outside editor', () => {
        it('should not call initEditor or destroyEditor when outside editor', () => {
            isInsideEditorSpy.mockReturnValue(false);

            renderHook(() => useDotcmsEditor());

            expect(initEditorSpy).not.toHaveBeenCalled();
            expect(destroyEditorSpy).not.toHaveBeenCalled();
        });

        it('should dont update pageInfo when received from editor', async () => {
            isInsideEditorSpy.mockReturnValue(false);
            const pageInfoMock = { title: 'Hello World' };

            const { result } = renderHook(() => useDotcmsEditor());

            act(() => {
                const event = new MessageEvent('message', {
                    data: {
                        name: 'SET_PAGE_INFO',
                        payload: pageInfoMock
                    }
                });
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(result.current.pageInfo).toEqual(null);
            });
        });
    });

    describe('when inside editor', () => {
        it('should call initEditor when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);

            renderHook(() => useDotcmsEditor());

            expect(initEditorSpy).toHaveBeenCalled();
        });

        it('should call destroyEditor on unmount when inside editor', () => {
            isInsideEditorSpy.mockReturnValueOnce(true);

            const { unmount } = renderHook(() => useDotcmsEditor());

            unmount();

            expect(destroyEditorSpy).toHaveBeenCalled();
        });

        it('should update pageInfo when received from editor', async () => {
            isInsideEditorSpy.mockReturnValue(true);
            const pageInfoMock = { title: 'Hello World' };

            const { result } = renderHook(() => useDotcmsEditor());

            act(() => {
                const event = new MessageEvent('message', {
                    data: {
                        name: 'SET_PAGE_INFO',
                        payload: pageInfoMock
                    }
                });
                window.dispatchEvent(event);
            });

            await waitFor(() => {
                expect(result.current.pageInfo).toEqual(pageInfoMock);
            });
        });
    });
});
