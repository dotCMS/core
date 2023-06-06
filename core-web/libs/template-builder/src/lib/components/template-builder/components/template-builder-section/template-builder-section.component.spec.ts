import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { TemplateBuilderSectionComponent } from './template-builder-section.component';

const MOCK_TEXT = 'Header';

describe('TemplateBuilderSectionComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderSectionComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderSectionComponent
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-section [title]="title"></dotcms-template-builder-section>`,
            {
                hostProps: {
                    title: MOCK_TEXT
                }
            }
        );
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should use the given title', () => {
        const sectionElement = spectator.debugElement.query(
            By.css('[data-testId="templateBuilderSection"]')
        );
        expect(sectionElement.nativeElement.textContent).toEqual(MOCK_TEXT);
    });
});
