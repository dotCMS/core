import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';
import { patchState, signalStore,  withComputed,  withMethods,  withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DEFAULT_FILE_TYPE, DEFAULT_MONACO_CONFIG } from '../dot-form-file-editor.conts';

type FileInfo = {
    name: string;
    content: string;
    mimeType: string;
    extension: string;
};

export interface FormFileEditorState {
    file: FileInfo | null;
    allowFileNameEdit: boolean;
    status: ComponentStatus;
    error: string | null;
    languageType: string;
    monacoOptions: MonacoEditorConstructionOptions;
}

const initialState: FormFileEditorState = {
    file: null,
    allowFileNameEdit: false,
    status: ComponentStatus.INIT,
    error: null,
    languageType: DEFAULT_FILE_TYPE,
    monacoOptions: DEFAULT_MONACO_CONFIG
};

export const FormFileEditorStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isUploading: computed(() => state.status() === ComponentStatus.LOADING),
        monacoConfig: computed<MonacoEditorConstructionOptions>(() => {
            const monacoOptions = state.monacoOptions();
            const language = state.languageType();

            return {
                ...monacoOptions,
                language: language
            };
        }),
    })),
    withMethods((store) => {
        return {
            setFile(file: FileInfo) {
                patchState(store, {file})
            },
            setMonacoOptions(monacoOptions: Partial<MonacoEditorConstructionOptions>) {
                const prevState = store.monacoOptions();

                patchState(store, {
                    monacoOptions: {
                        ...prevState,
                        ...monacoOptions
                    }
                });
            },
            uploadFile(): void {
                const fileInfo = store.file();

                if (!fileInfo) {
                    return;
                }

                const file = new File([fileInfo.content], fileInfo.name, {
                    type: fileInfo.mimeType
                });

                console.log(file);

                // implementation
            }
        }
    })
);