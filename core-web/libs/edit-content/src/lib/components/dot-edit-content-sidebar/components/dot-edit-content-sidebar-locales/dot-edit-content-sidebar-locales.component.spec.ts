import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Chip, ChipModule } from 'primeng/chip';

import { DotCMSContentlet } from '@dotcms/angular';
import { DotLanguage } from '@dotcms/dotcms-models';

import { DotEditContentSidebarLocalesComponent } from './dot-edit-content-sidebar-locales.component';

describe('DotEditContentSidebarLocalesComponent', () => {
    let spectator: Spectator<DotEditContentSidebarLocalesComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarLocalesComponent,
        imports: [ChipModule]
    });

    const locales: DotLanguage[] = [
        {
            id: 1,
            isoCode: 'en-us',
            translated: true
        },
        {
            id: 2,
            isoCode: 'es-es',
            translated: true
        },
        {
            id: 3,
            isoCode: 'it-it',
            translated: false
        }
    ] as DotLanguage[];
    const defaultLocale: DotLanguage = locales[0];
    const contentlet: DotCMSContentlet = { languageId: 1 } as DotCMSContentlet;

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });

        spectator.setInput('defaultLocale', defaultLocale);
        spectator.setInput('contentlet', contentlet);
        spectator.setInput('locales', locales);
    });

    it('should display the list of locales', () => {
        spectator.detectChanges();

        const chipElements = spectator.queryAll(Chip);
        expect(chipElements.length).toBe(3);

        expect(chipElements[0].label).toBe('en-us');
        expect(chipElements[0].label).toBe('es-es');
        expect(chipElements[0].label).toBe('it-it');
    });
});
