import { useContext } from 'react';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

import { PageContext } from '../../contexts/PageContext';
import { getContainersData } from '../../utils/utils';
import { PageProviderContext } from '../PageProvider/PageProvider';

const FAKE_CONTENLET = {
    identifier: 'TEMP_EMPTY_CONTENTLET',
    title: 'TEMP_EMPTY_CONTENTLET',
    contentType: 'TEMP_EMPTY_CONTENTLET_TYPE',
    inode: 'TEMPY_EMPTY_CONTENTLET_INODE',
    widgetTitle: 'TEMP_EMPTY_CONTENTLET'
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
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, components, isInsideEditor } = useContext<PageProviderContext | null>(
        PageContext
    ) as PageProviderContext;

    const { acceptTypes, contentlets, maxContentlets, path } = getContainersData(
        containers,
        containerRef
    );

    const updatedContentlets =
        contentlets.length === 0 && isInsideEditor ? [FAKE_CONTENLET] : contentlets;

    const container = {
        acceptTypes,
        identifier: path ?? identifier,
        maxContentlets,
        uuid
    };

    const containerPayload = {
        container
    };

    function onPointerEnterHandler(e: React.PointerEvent<HTMLDivElement>) {
        let target = e.target as HTMLElement;

        if (target.dataset.dot !== 'contentlet') {
            target = target.closest('[data-dot="contentlet"]') as HTMLElement;
        }

        if (!target) {
            return;
        }

        const { x, y, width, height } = target.getBoundingClientRect();

        const contentletPayload = JSON.parse(target.dataset.content ?? '{}');

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
            payload: {
                x,
                y,
                width,
                height,
                payload: contentletPayload
            }
        });
    }

    const renderContentlets = updatedContentlets.map((contentlet) => {
        const ContentTypeComponent = components[contentlet.contentType] || NoContent;

        const Component =
            contentlet.identifier === 'TEMP_EMPTY_CONTENTLET'
                ? EmptyContainer
                : ContentTypeComponent;

        const contentletPayload = {
            container,
            contentlet: {
                identifier: contentlet.identifier,
                title: contentlet.widgetTitle || contentlet.title,
                inode: contentlet.inode
            }
        };

        return isInsideEditor ? (
            <div
                onPointerEnter={onPointerEnterHandler}
                data-dot="contentlet"
                data-content={JSON.stringify(contentletPayload)}
                key={contentlet.identifier}>
                <Component {...contentlet} />
            </div>
        ) : (
            <Component {...contentlet} key={contentlet.identifier} />
        );
    });

    return isInsideEditor ? (
        <div data-dot="container" data-content={JSON.stringify(containerPayload)}>
            {renderContentlets}
        </div>
    ) : (
        // eslint-disable-next-line react/jsx-no-useless-fragment
        <>{renderContentlets}</>
    );
}
