import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import { createDotCMSVue, DOTCMS_CLIENT, useDotCMSClient } from './dotcms-client.plugin';

// vi.hoisted so the fake and spy exist before the hoisted vi.mock factory runs.
const { fakeClient, createDotCMSClient } = vi.hoisted(() => {
    const client = { page: {}, content: {} };

    return { fakeClient: client, createDotCMSClient: vi.fn((_config: unknown) => client) };
});

vi.mock('@dotcms/client', () => ({
    createDotCMSClient: (config: unknown) => createDotCMSClient(config)
}));

const CONFIG = { dotcmsUrl: 'https://demo.dotcms.com', authToken: 'token' };

describe('createDotCMSVue', () => {
    beforeEach(() => {
        createDotCMSClient.mockClear();
    });

    it('builds the client once from the given config', () => {
        const plugin = createDotCMSVue(CONFIG);

        expect(createDotCMSClient).toHaveBeenCalledTimes(1);
        expect(createDotCMSClient).toHaveBeenCalledWith(CONFIG);
        expect(plugin.client).toBe(fakeClient);
    });

    it('provides the client to components, retrievable via useDotCMSClient', () => {
        let injected: unknown;

        const Child = defineComponent({
            setup() {
                injected = useDotCMSClient();

                return () => h('div');
            }
        });

        mount(Child, { global: { plugins: [createDotCMSVue(CONFIG)] } });

        expect(injected).toBe(fakeClient);
    });

    it('exposes the same instance on .client and through injection', () => {
        const plugin = createDotCMSVue(CONFIG);
        let injected: unknown;

        const Child = defineComponent({
            inject: { c: { from: DOTCMS_CLIENT } },
            setup() {
                injected = useDotCMSClient();

                return () => h('div');
            }
        });

        mount(Child, { global: { plugins: [plugin] } });

        expect(injected).toBe(plugin.client);
    });
});

describe('useDotCMSClient without the plugin', () => {
    it('throws a helpful error when the plugin was not installed', () => {
        const Child = defineComponent({
            setup() {
                useDotCMSClient();

                return () => h('div');
            }
        });

        expect(() => mount(Child)).toThrow(/No dotCMS client found/);
    });
});
