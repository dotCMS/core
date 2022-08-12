import { Extension } from '@tiptap/core';
import { DotConfigModel } from './models';

// Storage configuration in the editor under the name space dotConfig
// access through editor.storage.dotConfig
export const DotConfigExtension = (data: DotConfigModel) => {
    return Extension.create<unknown, DotConfigModel>({
        name: 'dotConfig',

        addStorage() {
            return {
                ...data
            };
        }
    });
};
