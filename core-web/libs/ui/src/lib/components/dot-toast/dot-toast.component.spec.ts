import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { MessageService } from 'primeng/api';

import { DotToastComponent } from './dot-toast.component';

describe('DotToastComponent', () => {
    let spectator: Spectator<DotToastComponent>;
    let messageService: MessageService;

    const createComponent = createComponentFactory({
        component: DotToastComponent,
        providers: [MessageService]
    });

    beforeEach(() => {
        spectator = createComponent();
        messageService = spectator.inject(MessageService);
    });

    const show = (summary: string, detail: string): void => {
        messageService.add({ severity: 'success', summary, detail });
        spectator.detectChanges();
    };

    describe('markup in messages', () => {
        // The reason this component exists: PrimeNG's default template escapes both fields, and
        // language keys such as `content-drive.add-dotasset-success-detail` bold the file name, so a
        // bare `<p-toast />` renders literal `<b>` tags to the user.
        it('should render markup in the detail as HTML, not as text', () => {
            show('Upload Complete', '<b>photo.png</b> was uploaded as <b>Asset</b>');

            const detail = spectator.query('.p-toast-detail');

            expect(detail?.querySelectorAll('b')).toHaveLength(2);
            expect(detail?.textContent).toBe('photo.png was uploaded as Asset');
            expect(detail?.textContent).not.toContain('<b>');
        });

        it('should render markup in the summary as HTML, not as text', () => {
            show('<b>Upload</b> Complete', 'detail');

            const summary = spectator.query('.p-toast-summary');

            expect(summary?.querySelector('b')?.textContent).toBe('Upload');
            expect(summary?.textContent).not.toContain('<b>');
        });

        // Details can carry backend error text, so the HTML binding must stay sanitized.
        it('should strip scripting from the detail', () => {
            show('Error', 'boom <script>alert(1)</script><img src="x" onerror="alert(1)">');

            const detail = spectator.query('.p-toast-detail');

            expect(detail?.querySelector('script')).toBeNull();
            expect(detail?.querySelector('img')?.getAttribute('onerror')).toBeNull();
            expect(detail?.textContent).toContain('boom');
        });
    });

    describe('severity icon', () => {
        it('should render the dotCMS icon for the message severity', () => {
            show('Upload Complete', 'detail');

            expect(spectator.query('.p-toast-message-icon dot-severity-icon')).toBeTruthy();
            expect(spectator.query('.p-toast-message-icon .pi-check')).toBeTruthy();
        });
    });

    describe('position', () => {
        it('should default to top-center', () => {
            expect(spectator.query('.p-toast-top-center')).toBeTruthy();
        });

        it('should forward the configured position to the toast', () => {
            spectator.setInput('position', 'bottom-right');
            spectator.detectChanges();

            expect(spectator.query('.p-toast-bottom-right')).toBeTruthy();
        });
    });
});
