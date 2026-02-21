import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ContainerNotFoundComponent } from './container-not-found.component';

import { DotCMSStore } from '../../../../../../store/dotcms.store';

describe('ContainerNotFoundComponent', () => {
    let spectator: Spectator<ContainerNotFoundComponent>;
    let dotcmsContextService: jest.Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: ContainerNotFoundComponent,
        detectChanges: false,
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
        jest.clearAllMocks();
    });

    it('should display error message in dev mode', () => {
        jest.spyOn(console, 'error').mockImplementation();
        spectator.setInput('identifier', 'test-123');
        spectator.detectChanges();
        const element = spectator.query(byTestId('container-not-found'));
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('test-123');
    });

    it('should log error to console in dev mode', () => {
        spectator.setInput('identifier', 'test-123');
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
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
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        spectator.component.ngOnInit();
        expect(consoleSpy).not.toHaveBeenCalled();
    });
});
