import { render, screen } from '@testing-library/react';
import React from 'react';

import { FallbackComponent } from '../components/FallbackComponent/FallbackComponent';
import { DotCMSContentlet } from '../types';

const dummyContentlet = { contentType: 'test-type' } as unknown as DotCMSContentlet;

// A custom component for testing the "UserNoComponent" prop
const CustomNoComponent: React.FC<DotCMSContentlet> = ({ contentType }) => (
    <div data-testid="custom-no-component">
        Custom Component for <strong>{contentType}</strong>.
    </div>
);

describe('FallbackComponent', () => {
    test('renders nothing when not in development mode', () => {
        const { container } = render(
            <FallbackComponent
                UserNoComponent={CustomNoComponent}
                contentlet={dummyContentlet}
                isDevMode={false}
            />
        );
        expect(container.firstChild).toBeNull();
    });

    test('renders default NoComponent when in development mode without a custom component', () => {
        // We use "undefined" as the custom component to force the default component behavior
        render(
            <FallbackComponent
                UserNoComponent={undefined as unknown as React.ComponentType<DotCMSContentlet>}
                contentlet={dummyContentlet}
                isDevMode={true}
            />
        );
        const defaultComponent = screen.getByTestId('no-component');
        expect(defaultComponent).toBeInTheDocument();
        expect(defaultComponent).toHaveTextContent('No Component for');
        expect(defaultComponent.querySelector('strong')?.textContent).toBe('test-type');
    });

    test('renders custom component when provided in development mode', () => {
        render(
            <FallbackComponent
                UserNoComponent={CustomNoComponent}
                contentlet={dummyContentlet}
                isDevMode={true}
            />
        );
        const customComponent = screen.getByTestId('custom-no-component');
        expect(customComponent).toBeInTheDocument();
        expect(customComponent).toHaveTextContent('Custom Component for');
        expect(customComponent.querySelector('strong')?.textContent).toBe('test-type');
    });
});
