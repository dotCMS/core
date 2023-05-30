import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgIf } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import {
    TemplateBuilderBoxComponent,
    TemplateBuilderBoxSize
} from './template-builder-box.component';

describe('TemplateBuilderBoxComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderBoxComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderBoxComponent,
        imports: [NgClass, NgIf, ButtonModule, CardModule, ScrollPanelModule]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-box [size]="size"> </dotcms-template-builder-box>`,
            {
                hostProps: {
                    size: 'large'
                }
            }
        );
    });

    it('should create the component', () => {
        expect(spectator).toBeTruthy();
    });

    it('should render with default size', () => {
        expect(spectator.query(byTestId('template-builder-box'))).toHaveClass(
            'template-builder-box--large'
        );
    });

    it('should render with medium size and update the class', () => {
        spectator.setInput('size', TemplateBuilderBoxSize.medium);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box'))).toHaveClass(
            'template-builder-box--medium'
        );
    });

    it('should render with small size and update the class', () => {
        spectator.setInput('size', TemplateBuilderBoxSize.small);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box-small'))).toHaveClass(
            'template-builder-box--small'
        );
    });

    it('should render the first ng-template for large and medium sizes', () => {
        spectator.setInput('size', TemplateBuilderBoxSize.large);
        spectator.detectComponentChanges();
        const firstTemplate = spectator.query(byTestId('template-builder-box'));
        const secondTemplate = spectator.query(byTestId('template-builder-box-small'));
        expect(firstTemplate).toBeTruthy();
        expect(secondTemplate).toBeNull();
    });

    it('should show all buttons for small size', () => {
        spectator.setInput('size', TemplateBuilderBoxSize.small);
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('btn-plus-small'));
        const paletteButton = spectator.query(byTestId('btn-palette-small'));
        const deleteButton = spectator.query(byTestId('btn-trash-small'));
        expect(addButton).toBeTruthy();
        expect(paletteButton).toBeTruthy();
        expect(deleteButton).toBeTruthy();
    });
});
