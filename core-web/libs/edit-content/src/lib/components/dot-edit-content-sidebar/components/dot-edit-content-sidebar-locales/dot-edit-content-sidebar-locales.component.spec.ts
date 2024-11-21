import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

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
        spectator = createComponent({
            props: {
                locales: locales,
                defaultLocale: defaultLocale,
                contentlet: contentlet
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should display the list of locales', () => {
        //spectator.detectChanges();

        const chipElements = spectator.queryAll(Chip);
        expect(chipElements.length).toBe(3);

        expect(chipElements[0].label).toBe('en-us');
        expect(chipElements[1].label).toBe('es-es');
        expect(chipElements[2].label).toBe('it-it');
    });

    it('should display the classes correctly', () => {
        const chipElements = spectator.queryAll(Chip);
        expect(chipElements.length).toBe(3);

        const chipDebugElements = spectator.debugElement.queryAll(By.css('p-chip'));

        expect(chipDebugElements[0].nativeElement.classList).toContain('translated');
        expect(chipDebugElements[0].nativeElement.classList).toContain('current');
        expect(chipDebugElements[0].nativeElement.classList).toContain('default');

        expect(chipDebugElements[1].nativeElement.classList).toContain('translated');
        expect(chipDebugElements[1].nativeElement.classList).not.toContain('current');
        expect(chipDebugElements[1].nativeElement.classList).not.toContain('default');

        expect(chipDebugElements[2].nativeElement.classList).not.toContain('translated');
        expect(chipDebugElements[2].nativeElement.classList).not.toContain('current');
        expect(chipDebugElements[2].nativeElement.classList).not.toContain('default');
    });
});
