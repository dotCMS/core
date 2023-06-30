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
});
