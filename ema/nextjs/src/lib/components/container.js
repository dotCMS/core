import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';
import { getContainersData } from '@/lib/utils';
import { contentComponents } from '@/components/content-types';
import NoContent from '@/components/content-types/noContent';

function Container({ containerRef }) {
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, page, viewAs } = useContext(GlobalContext);

    const containerData = getContainersData(containers, containerRef);
    const { acceptTypes, contentlets, maxContentlets, pageContainers, path } = containerData;

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

    return (
        <>
            <div
                data-dot="container"
                data-content={JSON.stringify(containerPayload)}
                className="flex flex-col gap-4">
                {contentlets.map((contentlet) => {
                    const Component = contentComponents[contentlet.contentType] || NoContent;

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
                            onPointerEnter={(e) => {
                                let target = e.target;

                                if (target.dataset.dot !== 'contentlet') {
                                    target = target.closest('[data-dot="contentlet"]');
                                }

                                const { x, y, width, height } = target.getBoundingClientRect();

                                window.parent.postMessage(
                                    {
                                        action: 'set-contentlet',
                                        payload: {
                                            x,
                                            y,
                                            width,
                                            height,
                                            payload: contentletPayload
                                        }
                                    },
                                    '*'
                                );
                            }}
                            data-dot="contentlet"
                            data-content={JSON.stringify(contentletPayload)}
                            className="p-4 bg-slate-100"
                            key={contentlet.identifier}>
                            <Component {...contentlet} />
                        </div>
                    );
                })}
            </div>
        </>
    );
}

export default Container;
