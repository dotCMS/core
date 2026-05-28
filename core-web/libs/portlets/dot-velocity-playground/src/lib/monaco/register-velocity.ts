import { dotVelocityLanguageDefinition } from '@dotcms/edit-content/custom-languages/velocity-monaco-language';

interface WindowWithMonaco extends Window {
    monaco?: {
        languages: {
            register: (language: {
                id: string;
                extensions?: string[];
                mimetypes?: string[];
            }) => void;
            setMonarchTokensProvider: (id: string, provider: unknown) => void;
            getLanguages?: () => Array<{ id: string }>;
        };
    };
}

export const VELOCITY_LANGUAGE_ID = 'velocity';

let registered = false;

export const ensureVelocityLanguageRegistered = (): void => {
    if (registered) return;

    const win = window as WindowWithMonaco;
    const monaco = win.monaco;
    if (!monaco) return;

    const already = monaco.languages
        .getLanguages?.()
        .some((lang) => lang.id === VELOCITY_LANGUAGE_ID);
    if (!already) {
        monaco.languages.register({
            id: VELOCITY_LANGUAGE_ID,
            extensions: ['.vtl'],
            mimetypes: ['text/x-velocity']
        });
        monaco.languages.setMonarchTokensProvider(
            VELOCITY_LANGUAGE_ID,
            dotVelocityLanguageDefinition
        );
    }

    registered = true;
};
