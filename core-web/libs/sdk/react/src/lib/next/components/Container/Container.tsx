import { useContext, useRef } from 'react';
import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext, DotCMSRenderContextI } from '../../contexts/DotCMSRenderContext';
import { DotCMSColumnContainer, DotCMSContentlet } from '../../types';
import { getContainersData } from '../../utils/utils';
import { useCheckVisibleContent } from '../../hooks/useCheckHaveContent';

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

    const containerData = JSON.stringify({
        uuid,
        variantId,
        acceptTypes,
        maxContentlets,
        identifier: path ?? identifier
    }); // Get Container

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
                      <ComponentRender
                          key={contentlet.identifier}
                          contentlet={contentlet}
                          components={customComponents}
                          container={containerData}
                      />
                  ))}
        </div>
    );
}

// MOVE TO ANOTHER FILE
function ComponentRender({ contentlet, container, components }: any) {
    const ref = useRef<HTMLDivElement | null>(null);
    const haveContent = useCheckVisibleContent(ref);

    const ContentTypeComponent = components[contentlet?.contentType];
    const DefaultComponent = components['CustomNoComponent'] || NoComponent;
    const FallbackComponent = true || isInsideEditor() ? DefaultComponent : EmptyContent;

    const Component = ContentTypeComponent || FallbackComponent;

    return (
        <div
            data-testid="dot-contentlet"
            data-dot-object="contentlet"
            data-dot-identifier={contentlet?.identifier}
            data-dot-basetype={contentlet?.baseType}
            data-dot-title={contentlet?.widgetTitle || contentlet?.title}
            data-dot-inode={contentlet?.inode}
            data-dot-type={contentlet?.contentType}
            data-dot-container={container}
            data-dot-on-number-of-pages={contentlet?.onNumberOfPages}
            key={contentlet?.identifier}
            ref={ref}
            style={{ minHeight: haveContent ? undefined : '4rem' }}>
            <Component {...contentlet} />
        </div>
    );
}

/**
 * Component to render when there is no component for the content type.
 *
 * @param {{ readonly contentType: string }} { contentType }
 * @return {*}
 */
function NoComponent({ contentType }: { readonly contentType: string }) {
    return <div data-testid="no-component">No Component for {contentType}</div>;
}

/**
 * Component to render when there is no content in the container.
 *
 * @return {*}
 */
function EmptyContent() {
    return null;
}
