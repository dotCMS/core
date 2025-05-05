import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { UVE_MODE } from '@dotcms/types';
import * as dotcmsUVE from '@dotcms/uve';

import { DotCMSShow } from '../../components/DotCMSShow/DotCMSShow';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('DotCMSShow', () => {
    const getUVEStateMock = dotcmsUVE.getUVEState as jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should render children when UVE is in edit mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

        render(
            <DotCMSShow>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShow>
        );

        expect(screen.getByTestId('edit-content')).toBeInTheDocument();
    });

    test('should not render children when UVE is not in edit mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.PREVIEW });

        const { container } = render(
            <DotCMSShow>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShow>
        );

        expect(container.innerHTML).toBe('');
    });

    test('should not render children when UVE state is undefined', () => {
        getUVEStateMock.mockReturnValue(undefined);

        const { container } = render(
            <DotCMSShow>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShow>
        );

        expect(container.innerHTML).toBe('');
    });

    describe('when when prop is provided', () => {
        test('should render children when UVE is in the provided mode', () => {
            getUVEStateMock.mockReturnValue({ mode: UVE_MODE.PREVIEW });

            render(
                <DotCMSShow when={UVE_MODE.PREVIEW}>
                    <div data-testid="preview-content">Preview Mode Content</div>
                </DotCMSShow>
            );

            expect(screen.getByTestId('preview-content')).toBeInTheDocument();
        });

        test('should not render children when UVE is not in the provided mode', () => {
            getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

            const { container } = render(
                <DotCMSShow when={UVE_MODE.PREVIEW}>
                    <div data-testid="preview-content">Preview Mode Content</div>
                </DotCMSShow>
            );

            expect(container.innerHTML).toBe('');
        });
    });
});
