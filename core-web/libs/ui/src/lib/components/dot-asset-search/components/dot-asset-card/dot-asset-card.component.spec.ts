import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Card, CardModule } from 'primeng/card';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotAssetCardComponent } from './dot-asset-card.component';

const contentlet: DotCMSContentlet = {
    ...EMPTY_CONTENTLET,
    title: 'test title',
    fileName: 'test fileName',
    language: 'en'
};

describe('DotAssetCardComponent', () => {
    let spectator: Spectator<DotAssetCardComponent>;

    const createComponent = createComponentFactory({
        component: DotAssetCardComponent,
        imports: [CardModule]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet
            }
        });
    });

    it('should use the dot-contentlet-thumbnail', () => {
        const card = spectator.query(Card);
        const thumbnail = spectator.query('dot-contentlet-thumbnail');
        expect(thumbnail).toBeDefined();
        expect(card).toBeDefined();
    });

    it('should display the contentlet file name and language', () => {
        const title = spectator.query('[data-testId="dot-card-title"]');
        const language = spectator.query('[data-testId="dot-card-language"]');

        expect(title.innerHTML.trim()).toBe(contentlet.fileName);
        const languageContent = contentlet.language as string;
        expect(language.innerHTML.trim()).toBe(languageContent);
    });

    it('should display the contentlet title when the fileName property is empty', () => {
        spectator.setInput('contentlet', {
            ...contentlet,
            fileName: ''
        });

        spectator.detectChanges();

        const title = spectator.query('[data-testId="dot-card-title"]');
        expect(title.innerHTML.trim()).toBe(contentlet.title);
    });
});
