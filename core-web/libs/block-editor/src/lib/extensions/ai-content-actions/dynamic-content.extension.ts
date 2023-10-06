import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIContentActionsExtension } from './ai-content-actions.extension';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        DynamicContentExtension: {
            handleContentType: (contentType: string) => ReturnType;
        };
    }
}

export const DynamicContentExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'dynamicContentActions',

        addCommands() {
            return {
                handleContentType: (contentType: string) => () => {
                    if (contentType === 'image') {
                        AIContentActionsExtension(viewContainerRef, {
                            contentType: 'image'
                        });
                    } else {
                        AIContentActionsExtension(viewContainerRef, {
                            contentType: 'text'
                        });
                    }

                    return true;
                }
            };
        }
    });
};
