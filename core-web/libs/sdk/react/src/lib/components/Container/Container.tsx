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
                backgroundColor: '#d1d4db',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                color: '#fff',
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
    const { containers, page, viewAs, components, isInsideEditor } =
        useContext<PageProviderContext | null>(PageContext) as PageProviderContext;

    const { acceptTypes, contentlets, maxContentlets, pageContainers, path } = getContainersData(
        containers,
        containerRef
    );

    const updatedContentlets = contentlets.length > 0 ? contentlets : [FAKE_CONTENLET];

    const contentletsId = updatedContentlets.map((contentlet) => contentlet.identifier);

    const container = {
        acceptTypes,
        contentletsId,
        identifier: path ?? identifier,
        maxContentlets,
        uuid
    };

    const containerPayload = {
        container,
        language_id: viewAs.language.id,
        pageContainers,
        pageId: page.identifier,
        personaTag: viewAs.persona?.keyTag
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
            },
            language_id: viewAs.language.id,
            pageContainers,
            pageId: page.identifier,
            personaTag: viewAs.persona?.keyTag
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
