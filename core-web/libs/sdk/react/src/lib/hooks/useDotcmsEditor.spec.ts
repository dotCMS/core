import { renderHook } from '@testing-library/react-hooks';

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

    it('should not call initEditor or destroyEditor when outside editor', () => {
        isInsideEditorSpy.mockReturnValueOnce(false);

        renderHook(() => useDotcmsEditor());

        expect(initEditorSpy).not.toHaveBeenCalled();
        expect(destroyEditorSpy).not.toHaveBeenCalled();
    });

    it('should call initEditor with options', () => {
        isInsideEditorSpy.mockReturnValueOnce(true);

        renderHook(() => useDotcmsEditor({ onReload: jest.fn() }));

        expect(initEditorSpy).toHaveBeenCalledWith({ onReload: expect.any(Function) });
    });
});
