import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Card, CardModule } from 'primeng/card';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotAssetCardComponent } from './dot-asset-card.component';

import { DotContentletStatusBadgeComponent } from '../../../dot-contentlet-status-badge/dot-contentlet-status-badge.component';

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
        imports: [CardModule, DotContentletStatusBadgeComponent]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet
            }
        });
    });

    it('should use the dot-content-thumbnail', () => {
        const card = spectator.query(Card);
        const thumbnail = spectator.query('dot-content-thumbnail');
        expect(thumbnail).toBeTruthy();
        expect(card).toBeDefined();
    });

    it('should display the contentlet file name and language', () => {
        spectator.detectChanges();
        const title = spectator.query('[data-testId="dot-card-title"]');
        const language = spectator.query('[data-testId="dot-card-language"]');

        expect(title?.textContent?.trim()).toBe(contentlet.fileName);
        const languageContent = contentlet.language as string;
        expect(language?.textContent?.trim()).toBe(languageContent);
    });

    it('should display the contentlet title when the fileName property is empty', () => {
        spectator.setInput('contentlet', {
            ...contentlet,
            fileName: ''
        });

        spectator.detectChanges();

        const title = spectator.query('[data-testId="dot-card-title"]');
        expect(title?.textContent?.trim()).toBe(contentlet.title);
    });
});
