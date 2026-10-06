import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { Mocked, vi } from 'vitest';

import { ContainerNotFoundComponent } from './container-not-found.component';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

describe('ContainerNotFoundComponent', () => {
    let spectator: Spectator<ContainerNotFoundComponent>;
    let dotcmsContextService: Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: ContainerNotFoundComponent,
        detectChanges: false,
        providers: [
            {
                provide: DotCMSStore,
                useValue: {
                    $isDevMode: vi.fn().mockReturnValue(true)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotcmsContextService = spectator.inject(DotCMSStore) as Mocked<DotCMSStore>;
        vi.clearAllMocks();
    });

    it('should display error message in dev mode', () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        spectator.setInput('identifier', 'test-123');
        spectator.detectChanges();
        const element = spectator.query(byTestId('container-not-found'));
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('test-123');
    });

    it('should log error to console in dev mode', () => {
        spectator.setInput('identifier', 'test-123');
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        spectator.component.ngOnInit();
        expect(consoleSpy).toHaveBeenCalledWith('Container with identifier test-123 not found');
    });

    it('should not display anything in production mode', () => {
        dotcmsContextService.$isDevMode.mockReturnValue(false);
        spectator.detectChanges();
        const element = spectator.query(byTestId('container-not-found'));
        expect(element).toBeFalsy();
    });

    it('should not log error in production mode', () => {
        dotcmsContextService.$isDevMode.mockReturnValue(false);
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        spectator.component.ngOnInit();
        expect(consoleSpy).not.toHaveBeenCalled();
    });
});
