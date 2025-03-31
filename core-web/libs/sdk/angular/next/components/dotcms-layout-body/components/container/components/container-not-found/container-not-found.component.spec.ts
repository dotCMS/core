import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ContainerNotFoundComponent } from './container-not-found.component';

import { DotCMSContextService } from '../../../../../../services/dotcms-context/dotcms-context.service';

describe('ContainerNotFoundComponent', () => {
    let spectator: Spectator<ContainerNotFoundComponent>;
    let dotcmsContextService: jest.Mocked<DotCMSContextService>;

    const createComponent = createComponentFactory({
        component: ContainerNotFoundComponent,
        providers: [
            {
                provide: DotCMSContextService,
                useValue: {
                    isDevMode: true
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotcmsContextService = spectator.inject(
            DotCMSContextService
        ) as jest.Mocked<DotCMSContextService>;
        spectator.component.identifier = 'test-123';
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should display error message in dev mode', () => {
        spectator.detectChanges();
        const element = spectator.query('[data-testid="container-not-found"]');
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('test-123');
    });

    it('should log error to console in dev mode', () => {
        const consoleSpy = jest.spyOn(console, 'error');
        spectator.detectChanges();
        expect(consoleSpy).toHaveBeenCalledWith('Container with identifier test-123 not found');
    });

    it('should not display anything in production mode', () => {
        jest.spyOn(dotcmsContextService, 'isDevMode').mockReturnValue(false);
        spectator.detectChanges();
        const element = spectator.query('[data-testid="container-not-found"]');
        expect(element).toBeFalsy();
    });

    it('should not log error in production mode', () => {
        jest.spyOn(dotcmsContextService, 'isDevMode').mockReturnValue(false);
        const consoleSpy = jest.spyOn(console, 'error');
        spectator.detectChanges();
        expect(consoleSpy).not.toHaveBeenCalled();
    });
});
