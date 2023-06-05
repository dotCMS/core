import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgFor, NgStyle } from '@angular/common';
import { By } from '@angular/platform-browser';

import { TemplateBuilderBackgroundColumnsComponent } from './template-builder-background-columns.component';

describe('TemplateBuilderBackgroundColumnsComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderBackgroundColumnsComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderBackgroundColumnsComponent,
        imports: [NgFor, NgStyle]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-background-columns></dotcms-template-builder-background-columns>`
        );
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have 12 columns', () => {
        const columns = spectator.debugElement.queryAll(By.css('[data-testclass="column"]'));
        expect(columns.length).toEqual(12);
    });
});
