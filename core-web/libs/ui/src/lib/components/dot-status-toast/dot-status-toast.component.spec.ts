import { Spectator, createComponentFactory } from '@openng/spectator/vitest';

import { MessageService } from 'primeng/api';

import { DotStatusToastComponent, STATUS_TOAST_KEY } from './dot-status-toast.component';

describe('DotStatusToastComponent', () => {
    let spectator: Spectator<DotStatusToastComponent>;
    let messageService: MessageService;

    const createComponent = createComponentFactory({
        component: DotStatusToastComponent,
        providers: [MessageService]
    });

    const raise = (message: Parameters<MessageService['add']>[0]) => {
        messageService.add({ key: STATUS_TOAST_KEY, ...message });
        spectator.detectChanges();
    };

    const box = () => spectator.query('.p-toast-message');

    beforeEach(() => {
        spectator = createComponent();
        messageService = spectator.inject(MessageService);
    });

    it('should show the outcome and nothing else', () => {
        // The whole point of this outlet: one short line. An outcome that needs a paragraph is a
        // report, and reports belong in dot-toast, which is sized for them.
        raise({ severity: 'success', summary: 'Uploaded' });

        expect(spectator.query('[data-testid="status-toast-summary"]')?.textContent?.trim()).toBe(
            'Uploaded'
        );
    });

    it('should carry the severity so the theme can colour it', () => {
        // Colour comes from the severity through the dotCMS PrimeNG preset — never hardcoded, and
        // never the dark pill from the prototype, which is not a pattern in this system.
        raise({ severity: 'success', summary: 'Uploaded' });

        expect(box()?.getAttribute('data-pc-severity') ?? box()?.className).toContain('success');
    });

    it('should render nothing a caller can close by hand', () => {
        // A status is not a message the reader has to deal with. It reports something already
        // under way and goes when that finishes, so a close button asks the reader to tidy up
        // after a thing they did not start and cannot affect -- and the row of controls it sat in
        // was most of what made this feel like a panel rather than a status.
        raise({ severity: 'success', summary: 'Uploaded' });

        expect(spectator.query('[data-testid="status-toast-close"]')).toBeNull();
    });

    it('should ignore a detail line rather than growing to fit it', () => {
        // A caller that passes one is using the wrong outlet. Dropping it keeps this toast the
        // size it promises to be instead of quietly turning into the wide one.
        raise({ severity: 'success', summary: 'Uploaded', detail: 'a paragraph nobody asked for' });

        expect(spectator.query('[data-testid="status-toast-summary"]')?.textContent?.trim()).toBe(
            'Uploaded'
        );
        expect(spectator.fixture.nativeElement.textContent).not.toContain('a paragraph');
    });

    it('should show a spinner while a run is still going', () => {
        // The in-flight half of the same story, so "Uploading…" and "Uploaded" are one surface
        // rather than an indicator in the toolbar and a toast somewhere else.
        raise({ severity: 'secondary', summary: 'Uploading…', icon: 'pi pi-spin pi-spinner' });

        expect(spectator.query('.pi-spinner')).toBeTruthy();
    });

    it('should size itself to its text rather than to a fixed width', () => {
        // The complaint this exists to answer: one short word sat in a 350px slab. Asserted as a
        // rule rather than a pixel count, which would pin the font metrics of whatever runs it.
        raise({ severity: 'success', summary: 'Uploaded' });

        expect(getComputedStyle(box() as Element).width).not.toBe('350px');
    });

    it('should ignore messages that are not addressed to it', () => {
        // A portlet provides ONE MessageService, and an outlet with no key renders every message
        // on it — which showed each status twice, wide at the top and compact at the bottom.
        messageService.add({ severity: 'success', summary: 'for the other outlet' });
        spectator.detectChanges();

        expect(spectator.query('[data-testid="status-toast-summary"]')).toBeNull();
    });

    it('should keep the markup the run label carries', () => {
        // The toolbar's label bolds the action and its target. Interpolating would print the tags.
        raise({ severity: 'info', summary: 'Applying <b>Upload</b> to <b>demo.dotcms.com</b>' });

        expect(spectator.query('[data-testid="status-toast-summary"] b')?.textContent).toBe(
            'Upload'
        );
    });
});
