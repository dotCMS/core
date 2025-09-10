import { useContext, useMemo } from 'react';

import { DotCMSBasicContentlet, DotCMSColumnContainer } from '@dotcms/types';
import {
    getContainersData,
    getDotContainerAttributes,
    getContentletsInContainer
} from '@dotcms/uve/internal';

import { ContainerNotFound, EmptyContainer } from './ContainerFallbacks';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { Contentlet } from '../Contentlet/Contentlet';

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
                    key={contentlet.identifier}
                    contentlet={contentlet}
                    container={JSON.stringify(containerData)}
                />
            ))}
        </div>
    );
}
