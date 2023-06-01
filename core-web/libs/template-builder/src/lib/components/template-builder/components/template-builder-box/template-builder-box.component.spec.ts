import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgIf } from '@angular/common';
import { Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import {
    TemplateBuilderBoxComponent,
    TemplateBuilderBoxSize
} from './template-builder-box.component';

import { DotContainer } from '../../models/models';
import { CONTAINERS_DATA_MOCK } from '../../utils/mocks';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'test-host-component'
})
class HostComponent {
    @Input()
    size: TemplateBuilderBoxSize;
    @Input()
    items: DotContainer[];

    editStyle(): void {
        /* */
    }

    addContainer(): void {
        /* */
    }

    deleteContainer(): void {
        /* */
    }

    deleteColumn(): void {
        /* */
    }
}
describe('TemplateBuilderBoxComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderBoxComponent, HostComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderBoxComponent,
        imports: [NgClass, NgIf, ButtonModule, ScrollPanelModule],
        host: HostComponent
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-box [size]="size" [items]="items" (editStyle)="editStyle()" (addContainer)="addContainer()" (deleteContainer)="deleteContainer()" (deleteColumn)="deleteColumn()"> </dotcms-template-builder-box>`,
            {
                hostProps: {
                    size: TemplateBuilderBoxSize.large,
                    items: CONTAINERS_DATA_MOCK
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

    it('should trigger addContainer when click on plus button', () => {
        jest.spyOn(spectator.hostComponent, 'addContainer');
        const addButton = spectator.query(byTestId('btn-plus'));

        addButton.dispatchEvent(new Event('onClick'));
        expect(spectator.hostComponent.addContainer).toHaveBeenCalled();
    });

    it('should trigger editStyle when click on palette button', () => {
        jest.spyOn(spectator.hostComponent, 'editStyle');
        const paletteButton = spectator.query(byTestId('btn-palette'));

        paletteButton.dispatchEvent(new Event('onClick'));
        expect(spectator.hostComponent.editStyle).toHaveBeenCalled();
    });

    it('should trigger deleteContainer when click on container trash button', () => {
        jest.spyOn(spectator.hostComponent, 'deleteContainer');
        const containerTrashButton = spectator.query(byTestId('btn-trash-container'));

        containerTrashButton.dispatchEvent(new Event('onClick'));
        expect(spectator.hostComponent.deleteContainer).toHaveBeenCalled();
    });

    it('should trigger deleteColumn when click on column trash button', () => {
        jest.spyOn(spectator.hostComponent, 'deleteColumn');
        const columnTrashButton = spectator.query(byTestId('btn-trash-column'));

        columnTrashButton.dispatchEvent(new Event('onClick'));
        expect(spectator.hostComponent.deleteColumn).toHaveBeenCalled();
    });
});
