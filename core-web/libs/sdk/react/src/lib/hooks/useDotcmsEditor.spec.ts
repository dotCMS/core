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

    describe('when outside editor', () => {
        it('should not call initEditor or destroyEditor when outside editor', () => {
            isInsideEditorSpy.mockReturnValue(false);

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
    });
});
