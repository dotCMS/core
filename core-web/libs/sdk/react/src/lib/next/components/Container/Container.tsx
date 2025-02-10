import { useContext, useEffect, useMemo } from 'react';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSColumnContainer, DotCMSContentlet } from '../../types';
import {
    getContainersData,
    getContentletsInContainer,
    getDotContainerAttributes
} from '../../utils';
import { Contentlet } from '../Contentlet/Contentlet';

type ContainerProps = {
    container: DotCMSColumnContainer;
};

const EMPTY_CONTAINER_STYLE = {
    width: '100%',
    backgroundColor: '#ECF0FD',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    color: '#030E32',
    height: '10rem'
};

/**
 * Container component that renders DotCMS containers and their contentlets.
 * This component is responsible for:
 * - Rendering container content based on DotCMS Page API data
 * - Handling empty container states
 * - Providing proper data attributes for DotCMS functionality
 * - Managing container contentlets rendering
 *
 * @component
 * @param {ContainerProps} props - Component properties
 * @returns {JSX.Element} Rendered container with its contentlets or empty state message
 *
 * @example
 * ```tsx
 * <Container container={containerData} />
 * ```
 */
export function Container({ container }: ContainerProps) {
    const { dotCMSPageAsset, isDevMode } = useContext(DotCMSRenderContext) as DotCMSRenderContextI;
    const containerData = useMemo(
        () => getContainersData(dotCMSPageAsset, container),
        [dotCMSPageAsset, container]
    );

    const contentlets = useMemo(
        () => getContentletsInContainer(dotCMSPageAsset, container),
        [dotCMSPageAsset, container]
    );

    if (!containerData) {
        return <ContainerNoFound identifier={container.identifier} isDevMode={isDevMode} />;
    }

    const isEmpty = contentlets.length === 0;
    const dotAttributes = getDotContainerAttributes(containerData);

    return (
        <div {...dotAttributes} style={isEmpty ? EMPTY_CONTAINER_STYLE : {}}>
            {isEmpty
                ? 'This container is empty.'
                : contentlets.map((contentlet: DotCMSContentlet) => (
                      <Contentlet
                          key={contentlet.identifier}
                          contentlet={contentlet}
                          container={JSON.stringify(containerData)}
                      />
                  ))}
        </div>
    );
}

/**
 * Component to display when a container is not found in the system.
 * Only renders in development mode for debugging purposes.
 *
 * @component
 * @param {Object} props - Component properties
 * @param {string} props.identifier - Container identifier
 * @param {boolean} props.isDevMode - Whether the application is in development mode
 * @returns {JSX.Element | null} Message about missing container or null in production
 */
const ContainerNoFound = ({
    identifier,
    isDevMode
}: {
    identifier: string;
    isDevMode: boolean;
}) => {
    useEffect(() => console.error(`Container with identifier ${identifier} not found`));

    if (!isDevMode) {
        return null;
    }

    return (
        <div style={EMPTY_CONTAINER_STYLE}>
            This container with identifier {identifier} was not found.
        </div>
    );
};
