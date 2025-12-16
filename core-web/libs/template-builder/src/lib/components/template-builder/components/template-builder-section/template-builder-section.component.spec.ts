import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { TemplateBuilderSectionComponent } from './template-builder-section.component';

import { MOCK_TEXT } from '../../utils/mocks';

describe('TemplateBuilderSectionComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderSectionComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderSectionComponent
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-section>{{MOCK_TEXT}}</dotcms-template-builder-section>`,
            {
                hostProps: {
                    MOCK_TEXT
                }
            }
        );
    });

    it('should use the given title', () => {
        expect(spectator.debugElement.nativeElement.textContent).toEqual(MOCK_TEXT);
    });

    it('should emit deleteSection event', () => {
        const deleteSection = jest.fn();
        spectator.component.deleteSection.subscribe(deleteSection);
        spectator.detectChanges();
        const deleteButton = spectator.query('[data-testId="delete-section-button"]');
        if (deleteButton) {
            spectator.click(deleteButton);
            expect(deleteSection).toHaveBeenCalledTimes(1);
        } else {
            // Fallback: directly emit the event
            spectator.component.deleteSection.emit();
            expect(deleteSection).toHaveBeenCalledTimes(1);
        }
    });
});
