import { useContext } from 'react';

import { PageContext } from '../../contexts/PageContext';
import { getContainersData } from '../../utils/utils';
import { PageProviderContext } from '../PageProvider/PageProvider';

const FAKE_CONTENLET = {
    identifier: 'TEMP_EMPTY_CONTENTLET',
    title: 'TEMP_EMPTY_CONTENTLET',
    contentType: 'TEMP_EMPTY_CONTENTLET_TYPE',
    inode: 'TEMPY_EMPTY_CONTENTLET_INODE',
    widgetTitle: 'TEMP_EMPTY_CONTENTLET',
    onNumberOfPages: 1
};

function EmptyContainer() {
    return (
        <div
            data-testid="empty-container"
            style={{
                width: '100%',
                backgroundColor: '#ECF0FD',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                color: '#030E32',
                height: '10rem'
            }}>
            This container is empty.
        </div>
    );
}

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

    const updatedContentlets =
        contentlets.length === 0 && isInsideEditor ? [FAKE_CONTENLET] : contentlets;

    const container = {
        acceptTypes,
        identifier: path ?? identifier,
        maxContentlets,
        variantId,
        uuid
    };

    const renderContentlets = updatedContentlets.map((contentlet) => {
        const ContentTypeComponent = components[contentlet.contentType] || NoContent;

        const Component =
            contentlet.identifier === 'TEMP_EMPTY_CONTENTLET'
                ? EmptyContainer
                : ContentTypeComponent;

        return isInsideEditor ? (
            <div
                data-dot-object="contentlet"
                data-dot-identifier={contentlet.identifier}
                data-dot-title={contentlet.widgetTitle || contentlet.title}
                data-dot-inode={contentlet.inode}
                data-dot-type={contentlet.contentType}
                data-dot-container={JSON.stringify(container)}
                key={contentlet.identifier}>
                <Component {...contentlet} />
            </div>
        ) : (
            <Component {...contentlet} key={contentlet.identifier} />
        );
    });

    return isInsideEditor ? (
        <div
            data-dot-object="container"
            data-dot-accept-types={acceptTypes}
            data-dot-identifier={path ?? identifier}
            data-max-contentlets={maxContentlets}
            data-uuid={uuid}>
            {renderContentlets}
        </div>
    ) : (
        // eslint-disable-next-line react/jsx-no-useless-fragment
        <>{renderContentlets}</>
    );
}
