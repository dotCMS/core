import { useEffect } from 'react';

import { useIsDevMode } from '../../../hooks/useIsDevMode';

/**
 * Error message component for when the page body is missing
 *
 * @return {JSX.Element} Error message component
 */
export const ErrorMessage = () => {
    const isDevMode = useIsDevMode();

    useEffect(() => {
        console.warn('Missing required layout.body property in page');
    }, []);

    return (
        isDevMode && (
            <div
                data-testid="error-message"
                style={{ padding: '1rem', border: '1px solid #e0e0e0', borderRadius: '4px' }}>
                <p style={{ margin: '0 0 0.5rem', color: '#666' }}>
                    The <code>page</code> is missing the required <code>layout.body</code> property.
                </p>
                <p style={{ margin: 0, color: '#666' }}>
                    Make sure the page asset is properly loaded and includes a layout configuration.
                </p>
            </div>
        )
    );
};
