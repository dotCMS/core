import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Chip, ChipModule } from 'primeng/chip';

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

    beforeEach(() => {
        spectator = createComponent({
            props: {
                locales: locales,
                defaultLocale: defaultLocale,
                currentLocale: defaultLocale,
                isLoading: false
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should display the list of locales', () => {
        const chipElements = spectator.queryAll(Chip);
        expect(chipElements.length).toBe(3);

        expect(chipElements[0].label).toBe('en-us');
        expect(chipElements[1].label).toBe('es-es');
        expect(chipElements[2].label).toBe('it-it');

        expect(chipElements[0].styleClass).toBe('p-chip-sm p-chip-filled default');
        expect(chipElements[1].styleClass).toBe('p-chip-sm p-chip-primary');
        expect(chipElements[2].styleClass).toBe('p-chip-sm p-chip-gray p-chip-dashed');
    });

    it('should show the skeleton on loading', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();

        const skeleton = spectator.queryAll('p-skeleton');
        expect(skeleton).toExist();
    });

    it('should emit switchLocale event when a locale chip is clicked', () => {
        const chipElement = spectator.query('p-chip');
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');

        spectator.click(chipElement);

        expect(switchLocaleSpy).not.toHaveBeenCalled();
    });

    it('should not emit switchLocale event when the current locale chip is clicked', () => {
        const chipElements = spectator.queryAll('p-chip');
        const switchLocaleSpy = jest.spyOn(spectator.component.switchLocale, 'emit');

        spectator.click(chipElements[1]);

        expect(switchLocaleSpy).toHaveBeenCalledWith(locales[1]);
    });
});
