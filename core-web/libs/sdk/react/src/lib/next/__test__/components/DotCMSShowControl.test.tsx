import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import * as dotcmsUVE from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

import { DotCMSShowControl } from '../../components/DotCMSShowControl/DotCMSShowControl';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('DotCMSShowControl', () => {
    const getUVEStateMock = dotcmsUVE.getUVEState as jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should render children when UVE is in edit mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

        render(
            <DotCMSShowControl>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShowControl>
        );

        expect(screen.getByTestId('edit-content')).toBeInTheDocument();
    });

    test('should not render children when UVE is not in edit mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.PREVIEW });

        const { container } = render(
            <DotCMSShowControl>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShowControl>
        );

        expect(container.innerHTML).toBe('');
    });

    test('should not render children when UVE state is undefined', () => {
        getUVEStateMock.mockReturnValue(undefined);

        const { container } = render(
            <DotCMSShowControl>
                <div data-testid="edit-content">Edit Mode Content</div>
            </DotCMSShowControl>
        );

        expect(container.innerHTML).toBe('');
    });
});
