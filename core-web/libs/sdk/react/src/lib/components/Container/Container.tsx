import { useContext } from 'react';

import { PageContext } from '../../contexts/PageContext';
import { getContainersData } from '../../utils/utils';
import { PageProviderContext } from '../PageProvider/PageProvider';

function NoContent({ contentType }: { readonly contentType: string }) {
    return <div data-testid="no-component">No Component for {contentType}</div>;
}

export interface ContainerProps {
    readonly containerRef: PageProviderContext['layout']['body']['rows'][0]['columns'][0]['containers'][0];
}

export function Container({ containerRef }: ContainerProps) {
    const { isInsideEditor } = useContext(PageContext) as PageProviderContext;

    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, components } = useContext<PageProviderContext | null>(
        PageContext
    ) as PageProviderContext;

    const { acceptTypes, contentlets, maxContentlets, variantId, path } = getContainersData(
        containers,
        containerRef
    );

    const container = {
        acceptTypes,
        identifier: path ?? identifier,
        maxContentlets,
        variantId,
        uuid
    };

    const containerStyles = contentlets.length
        ? undefined
        : {
              width: '100%',
              backgroundColor: '#ECF0FD',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              color: '#030E32',
              height: '10rem'
          };

    const ContainerChildren = contentlets.map((contentlet) => {
        const Component = components[contentlet.contentType] || NoContent;

        return isInsideEditor ? (
            <div
                data-dot-object="contentlet"
                data-dot-identifier={contentlet.identifier}
                data-dot-basetype={contentlet.baseType}
                data-dot-title={contentlet.widgetTitle || contentlet.title}
                data-dot-inode={contentlet.inode}
                data-dot-type={contentlet.contentType}
                data-dot-container={JSON.stringify(container)}
                data-dot-on-number-of-pages={contentlet.onNumberOfPages}
                key={contentlet.identifier}>
                <Component {...contentlet} />
            </div>
        ) : (
            <Component {...contentlet} key={contentlet.identifier} />
        );
    });

    return isInsideEditor ? (
        <div
            data-testid="dot-container"
            data-dot-object="container"
            data-dot-accept-types={acceptTypes}
            data-dot-identifier={path ?? identifier}
            data-max-contentlets={maxContentlets}
            data-dot-uuid={uuid}
            style={containerStyles}>
            {ContainerChildren.length ? ContainerChildren : 'This container is empty.'}
        </div>
    ) : (
        // eslint-disable-next-line react/jsx-no-useless-fragment
        <>{ContainerChildren}</>
    );
}
