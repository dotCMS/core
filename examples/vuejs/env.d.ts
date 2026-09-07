/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_DOTCMS_HOST: string;
    readonly VITE_DOTCMS_AUTH_TOKEN: string;
    readonly VITE_DOTCMS_SITE_ID: string;
    readonly VITE_DOTCMS_MODE: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
