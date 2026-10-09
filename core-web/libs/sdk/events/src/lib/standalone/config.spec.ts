import { describe, expect, it, vi } from 'vitest';

import { readScriptConfig } from './config';

const ORIGIN = 'https://site.test';

const tag = (attributes: Record<string, string>): HTMLScriptElement => {
    const script = document.createElement('script');

    for (const [name, value] of Object.entries(attributes)) {
        script.setAttribute(name, value);
    }

    return script;
};

describe('readScriptConfig', () => {
    it('returns null without a script or a site auth', () => {
        expect(readScriptConfig(null, ORIGIN)).toBeNull();
        expect(readScriptConfig(tag({ 'data-analytics-debug': 'true' }), ORIGIN)).toBeNull();
        expect(readScriptConfig(tag({ 'data-analytics-auth': '  ' }), ORIGIN)).toBeNull();
    });

    it('reads the attributes the dotCMS template prints', () => {
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-debug': 'true',
            'data-analytics-auto-page-view': 'false',
            'data-analytics-impressions': 'true',
            'data-analytics-clicks': 'true',
            'data-analytics-config': ''
        });

        expect(readScriptConfig(script, ORIGIN)).toEqual({
            dotcmsUrl: ORIGIN,
            siteAuth: 'site-auth',
            debug: true,
            autoPageView: false,
            impressions: true,
            clicks: true
        });
    });

    it('leaves the options to their defaults when the template prints them empty', () => {
        // The template prints every placeholder, empty when the app leaves the setting unset
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-debug': '',
            'data-analytics-auto-page-view': '',
            'data-analytics-impressions': '',
            'data-analytics-clicks': '',
            'data-analytics-config': ''
        });

        expect(readScriptConfig(script, ORIGIN)).toEqual({
            dotcmsUrl: ORIGIN,
            siteAuth: 'site-auth'
        });
    });

    it("leaves experiments on, as in any app: this script replaces dotCMS's own", () => {
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-config': '{"experiments": false}'
        });

        expect(readScriptConfig(script, ORIGIN)?.experiments).toBeUndefined();
    });

    it('takes the options the advanced config sets and ignores everything else', () => {
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-config': JSON.stringify({
                queue: { eventBatchSize: 5, flushInterval: 'soon' },
                impressions: { dwellMs: 500, color: 'red' },
                logLevel: 'info',
                siteAuth: 'another-site',
                server: 'https://elsewhere.test'
            })
        });

        expect(readScriptConfig(script, ORIGIN)).toEqual({
            dotcmsUrl: ORIGIN,
            siteAuth: 'site-auth',
            queue: { eventBatchSize: 5 },
            impressions: { dwellMs: 500 },
            logLevel: 'info'
        });
    });

    it('lets an attribute with a value override the advanced config', () => {
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-impressions': 'false',
            'data-analytics-config': '{"impressions": true, "debug": true}'
        });
        const config = readScriptConfig(script, ORIGIN);

        expect(config?.impressions).toBe(false);
        expect(config?.debug).toBe(true);
    });

    it('reads single-quoted JSON, the way users type it in the app', () => {
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-config': "{'queue': false, 'logLevel': 'warn'}"
        });
        const config = readScriptConfig(script, ORIGIN);

        expect(config?.queue).toBe(false);
        expect(config?.logLevel).toBe('warn');
    });

    it('ignores an advanced config that is not JSON, with a warning', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        const script = tag({
            'data-analytics-auth': 'site-auth',
            'data-analytics-config': '{queue: nope'
        });

        expect(readScriptConfig(script, ORIGIN)).toEqual({
            dotcmsUrl: ORIGIN,
            siteAuth: 'site-auth'
        });
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('data-analytics-config'));
        warn.mockRestore();
    });
});
