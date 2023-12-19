import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';
import { getContainersData } from '@/lib/utils';
import { contentComponents } from '@/components/content-types';
import NoContent from '@/components/content-types/noContent';
import ActionButton from './actionButton';

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
            <ActionButton
                message={{
                    action: 'add-contentlet',
                    payload: containerPayload
                }}>
                +
            </ActionButton>
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
                            title: contentlet.title,
                            inode: contentlet.inode
                        },
                        language_id: viewAs.language.id,
                        pageContainers,
                        pageId: page.identifier,
                        personaTag: viewAs.persona?.keyTag
                    };

                    return (
                        <div
                            data-dot="contentlet"
                            data-content={JSON.stringify(contentletPayload)}
                            className="p-4 bg-slate-100"
                            key={contentlet.identifier}>
                            <div className="flex gap-2">
                                <ActionButton
                                    message={{
                                        action: 'edit-contentlet',
                                        payload: contentletPayload
                                    }}>
                                    Edit
                                </ActionButton>
                                <ActionButton
                                    message={{
                                        action: 'delete-contentlet',
                                        payload: contentletPayload
                                    }}>
                                    Delete
                                </ActionButton>
                            </div>
                            <Component {...contentlet} />
                        </div>
                    );
                })}
            </div>
        </>
    );
}

export default Container;
