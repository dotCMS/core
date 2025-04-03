import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ContainerNotFoundComponent } from './container-not-found.component';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

describe('ContainerNotFoundComponent', () => {
    let spectator: Spectator<ContainerNotFoundComponent>;
    let dotcmsContextService: jest.Mocked<DotCMSStore>;
    let consoleSpy: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: ContainerNotFoundComponent,
        providers: [
            {
                provide: DotCMSStore,
                useValue: {
                    $isDevMode: jest.fn().mockReturnValue(true)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotcmsContextService = spectator.inject(DotCMSStore) as jest.Mocked<DotCMSStore>;
        spectator.component.identifier = 'test-123';

        jest.clearAllMocks();
    });

    it('should display error message in dev mode', () => {
        spectator.detectChanges();
        const element = spectator.query(byTestId('container-not-found'));
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('test-123');
    });

    it('should log error to console in dev mode', () => {
        consoleSpy = jest.spyOn(console, 'error');
        spectator.component.ngOnInit();
        expect(consoleSpy).toHaveBeenCalledWith('Container with identifier test-123 not found');
    });

    it('should not display anything in production mode', () => {
        jest.spyOn(dotcmsContextService, '$isDevMode').mockReturnValue(false);
        spectator.detectChanges();
        const element = spectator.query(byTestId('container-not-found'));
        expect(element).toBeFalsy();
    });

    it('should not log error in production mode', () => {
        jest.spyOn(dotcmsContextService, '$isDevMode').mockReturnValue(false);
        const consoleSpy = jest.spyOn(console, 'error');
        spectator.component.ngOnInit();
        expect(consoleSpy).not.toHaveBeenCalled();
    });
});
