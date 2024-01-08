import { useContext } from 'react';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

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
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, page, viewAs, components } = useContext<PageProviderContext | null>(
        PageContext
    ) as PageProviderContext;

    const { acceptTypes, contentlets, maxContentlets, pageContainers, path } = getContainersData(
        containers,
        containerRef
    );

    const contentletsId = contentlets.map((contentlet) => contentlet.identifier);

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

    return (
        <div data-dot="container" data-content={JSON.stringify(containerPayload)}>
            {contentlets.map((contentlet) => {
                const Component = components[contentlet.contentType] || NoContent;

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

                return (
                    <div
                        onPointerEnter={onPointerEnterHandler}
                        data-dot="contentlet"
                        data-content={JSON.stringify(contentletPayload)}
                        key={contentlet.identifier}>
                        <Component {...contentlet} />
                    </div>
                );
            })}
        </div>
    );
}
