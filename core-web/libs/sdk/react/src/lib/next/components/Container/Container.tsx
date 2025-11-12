import { useContext, useMemo } from 'react';

import { DotCMSBasicContentlet, DotCMSColumnContainer } from '@dotcms/types';
import {
    getContainersData,
    getDotContainerAttributes,
    getContentletsInContainer
} from '@dotcms/uve/internal';

import { ContainerNotFound, EmptyContainer } from './ContainerFallbacks';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import useActiveContentlet from '../../hooks/useIsActive';
import { Contentlet } from '../Contentlet/Contentlet';

const ACTIVE_STYLES = {
    outline: '2px solid #5B8DEF',
    outlineOffset: '2px',
    boxShadow: '0 0 0 4px rgba(91, 141, 239, 0.15), 0 4px 12px rgba(0, 0, 0, 0.1)',
    transition: 'all 0.2s ease-in-out'
};

/**
 * @internal
 *
 * Props for the Container component
 * @interface DotCMSContainerRendererProps
 * @property {DotCMSColumnContainer} container - The container data to be rendered
 */
type DotCMSContainerRendererProps = {
    container: DotCMSColumnContainer;
};

/**
 * @internal
 *
 * Container component that renders DotCMS containers and their contentlets.
 * This component is responsible for:
 * - Rendering container content based on DotCMS Page API data
 * - Handling empty container states
 * - Providing proper data attributes for DotCMS functionality
 * - Managing container contentlets rendering
 *
 * @component
 * @param {DotCMSContainerRendererProps} props - Component properties
 * @returns {JSX.Element} Rendered container with its contentlets or empty state message
 *
 * @example
 * ```tsx
 * <Container container={containerData} />
 * ```
 */
export function Container({ container }: DotCMSContainerRendererProps) {
    const { pageAsset } = useContext(DotCMSPageContext);
    const activeContentlet = useActiveContentlet();

    const containerData = useMemo(
        () => getContainersData(pageAsset, container),
        [pageAsset, container]
    );
    const contentlets = useMemo(
        () => getContentletsInContainer(pageAsset, container),
        [pageAsset, container]
    );

    if (!containerData) {
        return <ContainerNotFound identifier={container.identifier} />;
    }

    const isEmpty = contentlets.length === 0;
    const dotAttributes = getDotContainerAttributes(containerData);

    if (isEmpty) {
        return <EmptyContainer {...dotAttributes} />;
    }

    return (
        <div {...dotAttributes}>
            {contentlets.map((contentlet: DotCMSBasicContentlet) => (
                <Contentlet
                    style={activeContentlet === contentlet.identifier ? ACTIVE_STYLES : {}}
                    key={contentlet.identifier}
                    contentlet={contentlet}
                    container={JSON.stringify(containerData)}
                />
            ))}
        </div>
    );
}
