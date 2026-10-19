import { beforeEach, describe, expect, it, vi } from 'vitest';

// The SDK is replaced by a spy: the spec checks what the script hands it
const initEvents = vi.hoisted(() => vi.fn());

vi.mock('./lib/events', () => ({ initEvents }));

describe('standalone script', () => {
    beforeEach(() => {
        vi.resetModules();
        initEvents.mockClear();
        document.head.innerHTML = '';
    });

    it('starts events from the script tag dotCMS injects', async () => {
        const script = document.createElement('script');
        script.setAttribute('data-analytics-auth', 'site-auth');
        script.setAttribute('data-analytics-clicks', 'true');
        document.head.appendChild(script);

        await import('./standalone');

        // Traditional pages carry dotCMS's contentlet wrappers, not the experiment markup
        expect(initEvents).toHaveBeenCalledWith(
            { dotcmsUrl: window.location.origin, siteAuth: 'site-auth', clicks: true },
            'contentlets'
        );
    });

    it('does not start without data-analytics-auth, and says why', async () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

        await import('./standalone');

        expect(initEvents).not.toHaveBeenCalled();
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('data-analytics-auth'));
        warn.mockRestore();
    });
});
