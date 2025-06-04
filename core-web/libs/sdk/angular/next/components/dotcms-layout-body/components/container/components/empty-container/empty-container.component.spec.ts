import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';

import { EMPTY_CONTAINER_STYLE_ANGULAR } from '@dotcms/uve/internal';

import { EmptyContainerComponent } from './empty-container.component';

import { DotCMSStore } from '../../../../../../store/dotcms.store';
describe('EmptyContainerComponent', () => {
    let spectator: Spectator<EmptyContainerComponent>;
    let dotcmsContextService: jest.Mocked<DotCMSStore>;

    const createComponent = createComponentFactory({
        component: EmptyContainerComponent,
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
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should display empty container message in dev mode', () => {
        spectator.detectChanges();
        const element = spectator.query(byTestId('empty-container-message'));
        expect(element).toBeTruthy();
        expect(element?.textContent?.trim()).toBe('This container is empty.');
    });

    it('should have data-dot-object="empty-content"', () => {
        spectator.detectChanges();
        const element = spectator.query(byTestId('empty-container-message'));
        expect(element?.getAttribute('data-dot-object')).toBe('empty-content');
    });

    it('should not display anything in production mode', () => {
        jest.spyOn(dotcmsContextService, '$isDevMode').mockReturnValue(false);
        spectator.detectChanges();
        const element = spectator.query(byTestId('empty-container'));
        expect(element).toBeFalsy();
    });

    it('should apply empty container styles', () => {
        jest.spyOn(dotcmsContextService, '$isDevMode').mockReturnValue(true);

        spectator.detectChanges();
        const containerDiv = spectator.query(byTestId('empty-container'));
        expect(containerDiv).toHaveStyle(EMPTY_CONTAINER_STYLE_ANGULAR);
    });
});
