import { inject, type App, type InjectionKey } from 'vue';

import { createDotCMSClient } from '@dotcms/client';
import type { DotCMSClientConfig } from '@dotcms/types';

/**
 * The dotCMS client instance created by {@link createDotCMSClient}. Use this
 * type to annotate variables that hold the injected client.
 */
export type DotCMSClient = ReturnType<typeof createDotCMSClient>;

/**
 * Injection key for the dotCMS client.
 *
 * Advanced: exported so a consumer can `inject(DOTCMS_CLIENT)` directly (e.g.
 * outside a `setup()` where {@link useDotCMSClient} would throw), but prefer
 * {@link useDotCMSClient}.
 */
export const DOTCMS_CLIENT: InjectionKey<DotCMSClient> = Symbol('DotCMSClient');

/**
 * A Vue plugin returned by {@link createDotCMSVue}. Install it with `app.use`.
 */
export interface DotCMSVuePlugin {
    install(app: App): void;
    /** The client instance the plugin provides. Handy for use outside components. */
    client: DotCMSClient;
}

/**
 * Creates the dotCMS Vue plugin.
 *
 * Builds a single dotCMS client from `config` and provides it to the whole app
 * so components can retrieve it with {@link useDotCMSClient} — no module-level
 * singleton to import. This is the Vue analog of Angular's `provideDotCMSClient`.
 *
 * The created client is also exposed on the returned object as `.client`, so
 * non-component code (route loaders, plain modules) can share the same instance.
 *
 * @param config - the same configuration accepted by `createDotCMSClient`
 * @returns a Vue plugin to pass to `app.use()`
 *
 * @example
 * ```ts
 * import { createApp } from 'vue';
 * import { createDotCMSVue } from '@dotcms/vue';
 * import App from './App.vue';
 *
 * const app = createApp(App);
 *
 * app.use(
 *   createDotCMSVue({
 *     dotcmsUrl: 'https://demo.dotcms.com',
 *     authToken: 'your-auth-token',
 *     siteId: 'your-site-id',
 *     requestOptions: {
 *       // UVE needs fresh data so in-context edits are reflected immediately.
 *       cache: 'no-cache'
 *     }
 *   })
 * );
 *
 * app.mount('#app');
 * ```
 */
export function createDotCMSVue(config: DotCMSClientConfig): DotCMSVuePlugin {
    const client = createDotCMSClient(config);

    return {
        client,
        install(app: App) {
            app.provide(DOTCMS_CLIENT, client);
        }
    };
}

/**
 * Retrieves the dotCMS client provided by {@link createDotCMSVue}.
 *
 * Must be called from a component `setup` (or another composable). Throws if the
 * plugin was not installed with `app.use(createDotCMSVue(...))`, so a missing
 * provider fails loudly instead of returning `undefined`.
 *
 * @returns the dotCMS client instance
 *
 * @example
 * ```ts
 * import { useDotCMSClient } from '@dotcms/vue';
 *
 * const client = useDotCMSClient();
 * const { pageAsset } = await client.page.get({ path: '/' });
 * ```
 */
export function useDotCMSClient(): DotCMSClient {
    const client = inject(DOTCMS_CLIENT, null);

    if (!client) {
        throw new Error(
            '[useDotCMSClient] No dotCMS client found. ' +
                'Did you install the plugin with `app.use(createDotCMSVue({ ... }))`?'
        );
    }

    return client;
}
