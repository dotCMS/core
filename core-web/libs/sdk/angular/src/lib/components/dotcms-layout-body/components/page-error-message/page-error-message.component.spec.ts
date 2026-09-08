import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { Mocked, vi } from 'vitest';

import { PageErrorMessageComponent } from './page-error-message.component';

import { DotCMSStore } from '../../../../store/dotcms.store';

describe('PageErrorMessageComponent', () => {
    let spectator: Spectator<PageErrorMessageComponent>;
    let component: PageErrorMessageComponent;
    let dotcmsContextService: Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: PageErrorMessageComponent,
        mocks: [DotCMSStore],
        detectChanges: false
    });

    beforeEach(() => {
        dotcmsContextService = {
            isDevMode: vi.fn()
        } as unknown as Mocked<DotCMSStore>;

        spectator = createComponent({
            providers: [
                {
                    provide: DotCMSStore,
                    useValue: dotcmsContextService
                }
            ]
        });

        component = spectator.component;
        vi.spyOn(console, 'warn').mockImplementation();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display error message', () => {
        spectator.detectChanges();
        const errorMessage = spectator.query('[data-testid="error-message"]');
        expect(errorMessage).toBeTruthy();
        expect(errorMessage?.textContent).toContain('missing the required');
        expect(errorMessage?.textContent).toContain('layout.body');
    });

    it('should log warning message on initialization', () => {
        spectator.detectChanges();
        expect(console.warn).toHaveBeenCalledWith('Missing required layout.body property in page');
    });
});
