import { Spectator, byTestId, createComponentFactory, mockProvider } from '@openng/spectator/jest';

import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { DotUveStyleEditorEmptyStateComponent } from './dot-uve-style-editor-empty-state.component';

const messagesMock = {
    'uve.palette.style-editor.empty-state.title': 'Style editor',
    'uve.palette.style-editor.empty-state.message':
        'Customize your components. <a href="#">Learn more</a>',
    'uve.palette.style-editor.empty-state.cta': 'Define styles'
};

describe('DotUveStyleEditorEmptyStateComponent', () => {
    let spectator: Spectator<DotUveStyleEditorEmptyStateComponent>;
    let router: Router;

    const createComponent = createComponentFactory({
        component: DotUveStyleEditorEmptyStateComponent,
        imports: [DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(messagesMock)
            },
            mockProvider(Router, {
                navigate: jest.fn().mockResolvedValue(true)
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        router = spectator.inject(Router);
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should expose the content type variable input via setInput', () => {
        spectator.setInput('contentTypeVar', 'Blog');
        spectator.detectChanges();

        expect(spectator.component.$contentTypeVar()).toBe('Blog');
    });

    describe('layout and copy', () => {
        it('should set the root host data-testid for the empty state', () => {
            expect(spectator.element.getAttribute('data-testid')).toBe(
                'uve-style-editor-empty-state'
            );
        });

        it('should render the tune material symbol', () => {
            const icon = spectator.query(byTestId('uve-style-editor-empty-state-icon'));
            expect(icon).toBeTruthy();
            expect(icon?.classList.contains('material-symbols-outlined')).toBe(true);
            expect(icon?.textContent?.trim()).toBe('tune');
        });

        it('should render the title from DotMessageService', () => {
            const title = spectator.query(byTestId('uve-style-editor-empty-state-title'));
            expect(title?.textContent?.trim()).toBe('Style editor');
        });

        it('should render the message HTML from DotMessageService', () => {
            const message = spectator.query(byTestId('uve-style-editor-empty-state-message'));
            expect(message?.innerHTML).toContain('Customize your components');
            expect(message?.querySelector('a')).toBeTruthy();
        });
    });

    describe('CTA button', () => {
        it('should not render the CTA when contentTypeVar is empty', () => {
            spectator.setInput('contentTypeVar', '');
            spectator.detectChanges();

            expect(spectator.query(byTestId('uve-style-editor-empty-state-cta'))).toBeFalsy();
        });

        it('should not render the CTA when contentTypeVar is the empty-container placeholder', () => {
            spectator.setInput('contentTypeVar', TEMP_EMPTY_CONTENTLET_TYPE);
            spectator.detectChanges();

            expect(spectator.query(byTestId('uve-style-editor-empty-state-cta'))).toBeFalsy();
        });

        it('should render the CTA when contentTypeVar is set', () => {
            spectator.setInput('contentTypeVar', 'Blog');
            spectator.detectChanges();

            const cta = spectator.query(byTestId('uve-style-editor-empty-state-cta'));
            expect(cta).toBeTruthy();
            expect(cta?.textContent?.trim()).toContain('Define styles');
        });

        it('should navigate to the style editor route when the CTA is clicked', () => {
            spectator.setInput('contentTypeVar', 'Blog');
            spectator.detectChanges();

            const cta = spectator.query(byTestId('uve-style-editor-empty-state-cta'));
            const nativeButton = cta?.querySelector('button');
            expect(nativeButton).toBeTruthy();
            spectator.click(nativeButton as HTMLElement);

            expect(router.navigate).toHaveBeenCalledTimes(1);
            expect(router.navigate).toHaveBeenCalledWith([
                '/content-types-angular/edit',
                'Blog',
                'style-editor'
            ]);
        });

        it('should navigate using the current content type variable when navigateToStyleEditor runs', () => {
            spectator.setInput('contentTypeVar', 'Product');
            spectator.detectChanges();

            spectator.component.navigateToStyleEditor();

            expect(router.navigate).toHaveBeenCalledWith([
                '/content-types-angular/edit',
                'Product',
                'style-editor'
            ]);
        });
    });
});
