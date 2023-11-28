import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';
import { getContainersData } from '@/lib/utils';
import { contentComponents } from '@/components/content-types';
import NoContent from '@/components/content-types/noContent';
import ActionButton from './actionButton';

function Container({ containerRef }) {
    const { identifier, uuid } = containerRef;

    // Get the containers from the global context
    const { containers, page } = useContext(GlobalContext);

    const {
        inode,
        maxContentlets,
        acceptTypes,
        contentlets,
        contentletsId,
        pageContainers,
        path,
        container
    } = getContainersData(containers, containerRef);

    return (
        <>
            <ActionButton
                message={{
                    action: 'add-contentlet',
                    payload: {
                        pageID: page.identifier,
                        container: {
                            identifier: container.path ?? identifier,
                            uuid,
                            contentletsId,
                            acceptTypes
                        },
                        pageContainers
                    }
                }}>
                +
            </ActionButton>
            <div
                className="flex flex-col gap-4"
                data-dot-accept-types={acceptTypes}
                data-dot-object="container"
                data-dot-inode={inode}
                data-dot-identifier={identifier}
                data-dot-uuid={uuid}
                data-max-contentlets={maxContentlets}
                data-dot-can-add="CONTENT,FORM,WIDGET">
                {contentlets.map((contentlet) => {
                    const {
                        identifier,
                        inode,
                        contentType,
                        baseType,
                        title,
                        languageId,
                        dotContentTypeId
                    } = contentlet;

                    const Component = contentComponents[contentlet.contentType] || NoContent;

                    return (
                        <div
                            className="p-4 border border-gray-300"
                            key={contentlet.identifier}
                            data-dot-object="contentlet"
                            data-dot-inode={inode}
                            data-dot-identifier={identifier}
                            data-dot-type={contentType}
                            data-dot-basetype={baseType}
                            data-dot-lang={languageId}
                            data-dot-title={title}
                            data-dot-can-edit={true}
                            data-dot-content-type-id={dotContentTypeId}
                            data-dot-has-page-lang-version="true">
                            <div className="flex gap-2">
                                <ActionButton
                                    message={{
                                        action: 'edit-contentlet',
                                        payload: contentlet
                                    }}>
                                    Edit
                                </ActionButton>
                                <ActionButton
                                    message={{
                                        action: 'delete-contentlet',
                                        payload: {
                                            pageID: page.identifier,
                                            container: {
                                                identifier: container.path ?? identifier,
                                                uuid
                                            },
                                            pageContainers,
                                            contentletId: contentlet.identifier
                                        }
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
