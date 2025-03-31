import { useEffect } from 'react';

import { EMPTY_CONTAINER_STYLE } from '@dotcms/uve/internal';
import { DotContainerAttributes } from '@dotcms/uve/types';

import { useIsDevMode } from '../../hooks/useIsDevMode';

/**
 * @internal
 *
 * Component to display when a container is not found in the system.
 * Only renders in development mode for debugging purposes.
 *
 * @component
 * @param {Object} props - Component properties
 * @param {string} props.identifier - Container identifier
 * @returns {JSX.Element | null} Message about missing container or null in production
 */
export const ContainerNotFound = ({ identifier }: { identifier: string }) => {
    const isDevMode = useIsDevMode();

    useEffect(() => {
        if (!isDevMode) {
            return;
        }

        console.error(`Container with identifier ${identifier} not found`);
    }, [identifier, isDevMode]);

    if (!isDevMode) {
        return null;
    }

    return (
        <div data-testid="container-not-found" style={EMPTY_CONTAINER_STYLE}>
            This container with identifier {identifier} was not found.
        </div>
    );
};

/**
 * @internal
 *
 * Component to display when a container is empty.
 *
 * @param {DotContainerAttributes} dotAttributes
 * @return {*}
 */
export const EmptyContainer = (dotAttributes: DotContainerAttributes) => {
    const isDevMode = useIsDevMode();

    if (!isDevMode) {
        return null;
    }

    return (
        <div {...dotAttributes} style={EMPTY_CONTAINER_STYLE}>
            <span data-testid="empty-container-message">This container is empty.</span>
        </div>
    );
};
