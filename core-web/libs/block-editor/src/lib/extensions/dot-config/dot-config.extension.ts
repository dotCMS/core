import { Extension } from '@tiptap/core';

import { DotConfigModel } from './models';

// v3 made `editor.storage` strictly typed via module augmentation; declare
// `dotConfig` here so callers like `editor.storage.dotConfig.lang` typecheck.
//
// Declared required, not optional: `DotBlockEditorComponent.getEditorExtensions()` registers
// `DotConfigExtension` unconditionally as the first extension, so any editor this library
// builds has the storage populated. Marking it optional only pushed a `!` or `?.` onto every
// read — and the existing readers were already split between the two.
declare module '@tiptap/core' {
    interface Storage {
        dotConfig: DotConfigModel;
    }
}

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
