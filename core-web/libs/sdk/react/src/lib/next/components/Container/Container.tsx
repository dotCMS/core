import { useContext, useMemo } from 'react';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSColumnContainer, DotCMSContentlet } from '../../types';
import { getContainersData } from '../../utils/utils';
import { Contentlet } from '../Contentlet/Contentlet';

type ContainerProps = {
    container: DotCMSColumnContainer;
};

/**
 * Renders a Container with its content using information provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @export
 * @param {ContainerProps} { containerRef }
 * @return {JSX.Element} Rendered container with content
 */
export function Container({ container }: ContainerProps) {
    const { identifier, uuid } = container;
    const { dotCMSPageAsset, customComponents } = useContext(
        DotCMSRenderContext
    ) as DotCMSRenderContextI;

    const { acceptTypes, contentlets, maxContentlets, variantId, path } = getContainersData(
        dotCMSPageAsset.containers,
        container
    );

    const containerData = useMemo(
        () =>
            JSON.stringify({
                uuid,
                variantId,
                acceptTypes,
                maxContentlets,
                identifier: path ?? identifier
            }),
        [uuid, variantId, acceptTypes, maxContentlets, path, identifier]
    ); // Get Container

    const isEmpty = contentlets.length === 0;
    const emptyContainerStyle = {
        width: '100%',
        backgroundColor: '#ECF0FD',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        color: '#030E32',
        height: '10rem'
    };

    return (
        <div
            data-testid="dot-container"
            data-dot-object="container"
            data-dot-accept-types={acceptTypes}
            data-dot-identifier={path ?? identifier}
            data-max-contentlets={maxContentlets}
            data-dot-uuid={uuid}
            style={isEmpty ? emptyContainerStyle : {}}>
            {isEmpty
                ? 'This container is empty.'
                : contentlets.map((contentlet: DotCMSContentlet) => (
                      <Contentlet
                          key={contentlet.identifier}
                          contentlet={contentlet}
                          container={containerData}
                      />
                  ))}
        </div>
    );
}
