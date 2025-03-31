import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { EmptyContainerComponent } from './empty-container.component';

import { DotCMSContextService } from '../../../../../../services/dotcms-context/dotcms-context.service';

describe('EmptyContainerComponent', () => {
    let spectator: Spectator<EmptyContainerComponent>;
    let dotcmsContextService: jest.Mocked<DotCMSContextService>;

    const createComponent = createComponentFactory({
        component: EmptyContainerComponent,
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
        spectator.component.dotAttributes = {
            'data-dot-object': 'container',
            'data-dot-inode': '123',
            'data-dot-identifier': 'test-container'
        };
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should display empty container message in dev mode', () => {
        spectator.detectChanges();
        const element = spectator.query('[data-testid="empty-container-message"]');
        expect(element).toBeTruthy();
        expect(element?.textContent).toBe('This container is empty.');
    });

    it('should apply dot attributes to container div', () => {
        spectator.detectChanges();
        const containerDiv = spectator.query('div');
        expect(containerDiv?.getAttribute('data-dot-object')).toBe('container');
        expect(containerDiv?.getAttribute('data-dot-inode')).toBe('123');
        expect(containerDiv?.getAttribute('data-dot-identifier')).toBe('test-container');
    });

    it('should not display anything in production mode', () => {
        jest.spyOn(dotcmsContextService, 'isDevMode').mockReturnValue(false);
        spectator.detectChanges();
        const element = spectator.query('div');
        expect(element).toBeFalsy();
    });

    it('should apply empty container styles', () => {
        spectator.detectChanges();
        const containerDiv = spectator.query('div');
        expect(containerDiv).toHaveStyle({
            border: '1px dashed #ccc',
            padding: '1rem',
            margin: '1rem 0',
            backgroundColor: '#f5f5f5'
        });
    });
});
