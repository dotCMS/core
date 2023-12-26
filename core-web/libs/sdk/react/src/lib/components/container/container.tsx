import { useContext } from 'react';

import { getContainersData } from '../../utils/utils';
import {
    GlobalContext,
    PageProviderContext,
} from '../page-provider/page-provider';

/* eslint-disable-next-line */
export interface ContainerProps {
    containerRef: PageProviderContext['layout']['body']['rows'][0]['columns'][0]['containers'][0];
}

export function Container({ containerRef }: ContainerProps) {
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, page, viewAs } =
        useContext<PageProviderContext>(GlobalContext);

    const containerData = getContainersData(containers, containerRef);
    const { acceptTypes, contentlets, maxContentlets, pageContainers, path } =
        containerData;

    const contentletsId = contentlets.map(
        (contentlet) => contentlet.identifier
    );

    const container = {
        acceptTypes,
        contentletsId,
        identifier: path ?? identifier,
        maxContentlets,
        uuid,
    };

    const containerPayload = {
        container,
        language_id: viewAs.language.id,
        pageContainers,
        pageId: page.identifier,
        personaTag: viewAs.persona?.keyTag,
    };

    return (
        <div
            data-dot="container"
            data-content={JSON.stringify(containerPayload)}
            className="flex flex-col gap-4"
        >
            {contentlets.map((contentlet) => {
                // const Component =
                //     contentComponents[contentlet.contentType] || NoContent;
                const Component = () => <h3>Contentlet</h3>;

                const contentletPayload = {
                    container,
                    contentlet: {
                        identifier: contentlet.identifier,
                        title: contentlet.widgetTitle || contentlet.title,
                        inode: contentlet.inode,
                    },
                    language_id: viewAs.language.id,
                    pageContainers,
                    pageId: page.identifier,
                    personaTag: viewAs.persona?.keyTag,
                };

                return (
                    <div
                        onPointerEnter={(e) => {
                            let target = e.target as HTMLElement;

                            if (target.dataset.dot !== 'contentlet') {
                                target = target.closest(
                                    '[data-dot="contentlet"]'
                                ) as HTMLElement;
                            }

                            if (!target) {
                                return;
                            }

                            const { x, y, width, height } =
                                target.getBoundingClientRect();

                            window.parent.postMessage(
                                {
                                    action: 'set-contentlet',
                                    payload: {
                                        x,
                                        y,
                                        width,
                                        height,
                                        payload: contentletPayload,
                                    },
                                },
                                '*'
                            );
                        }}
                        data-dot="contentlet"
                        data-content={JSON.stringify(contentletPayload)}
                        className="p-4 bg-slate-100"
                        key={contentlet.identifier}
                    >
                        {/* <Component {...contentlet} /> */}
                        <Component />
                    </div>
                );
            })}
        </div>
    );
}

export default Container;
