import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';
import React from 'react';

import { DotCMSBasicContentlet } from '@dotcms/types';

import { FallbackComponent } from '../../components/FallbackComponent/FallbackComponent';
import * as useIsDevModeHook from '../../hooks/useIsDevMode';

jest.mock('../../hooks/useIsDevMode', () => ({
    useIsDevMode: jest.fn()
}));

const { useIsDevMode } = useIsDevModeHook as jest.Mocked<typeof useIsDevModeHook>;
const MOCK_DUMMY_CONTENTLET = { contentType: 'test-type' } as unknown as DotCMSBasicContentlet;

const CustomNoComponent: React.FC<DotCMSBasicContentlet> = ({ contentType }) => (
    <div data-testid="custom-no-component">
        Custom Component for <strong>{contentType}</strong>.
    </div>
);

describe('FallbackComponent', () => {
    test('should render nothing when not in development mode', () => {
        const { container } = render(
            <FallbackComponent
                UserNoComponent={CustomNoComponent}
                contentlet={MOCK_DUMMY_CONTENTLET}
            />
        );
        expect(container.firstChild).toBeNull();
    });

    describe('when in development mode', () => {
        beforeEach(() => useIsDevMode.mockReturnValue(true));

        test('should render default NoComponent when is in development mode and no custom component is provided', () => {
            render(
                <FallbackComponent
                    UserNoComponent={
                        undefined as unknown as React.ComponentType<DotCMSBasicContentlet>
                    }
                    contentlet={MOCK_DUMMY_CONTENTLET}
                />
            );
            const defaultComponent = screen.getByTestId('no-component');
            expect(defaultComponent).toBeInTheDocument();
            expect(defaultComponent).toHaveTextContent('No Component for');
            expect(defaultComponent.querySelector('strong')?.textContent).toBe('test-type');
        });

        test('should render custom no component when provided in development mode', () => {
            render(
                <FallbackComponent
                    UserNoComponent={CustomNoComponent}
                    contentlet={MOCK_DUMMY_CONTENTLET}
                />
            );
            const customComponent = screen.getByTestId('custom-no-component');
            expect(customComponent).toBeInTheDocument();
            expect(customComponent).toHaveTextContent('Custom Component for');
            expect(customComponent.querySelector('strong')?.textContent).toBe('test-type');
        });
    });
});
