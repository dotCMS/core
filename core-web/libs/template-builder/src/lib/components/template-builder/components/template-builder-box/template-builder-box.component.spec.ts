import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgIf } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import { CONTAINERS_DATA_MOCK } from '../../utils/mocks';

describe('TemplateBuilderBoxComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderBoxComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderBoxComponent,
        imports: [NgClass, NgIf, ButtonModule, ScrollPanelModule]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-box [size]="size" [items]="items"> </dotcms-template-builder-box>`,
            {
                hostProps: {
                    size: 10,
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
        spectator.setInput('size', 3);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box'))).toHaveClass(
            'template-builder-box--medium'
        );
    });

    it('should render with small size and update the class', () => {
        spectator.setInput('size', 1);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box-small'))).toHaveClass(
            'template-builder-box--small'
        );
    });

    it('should render the first ng-template for large and medium sizes', () => {
        spectator.setInput('size', 10);
        spectator.detectComponentChanges();
        const firstTemplate = spectator.query(byTestId('template-builder-box'));
        const secondTemplate = spectator.query(byTestId('template-builder-box-small'));
        expect(firstTemplate).toBeTruthy();
        expect(secondTemplate).toBeNull();
    });

    it('should show all buttons for small size', () => {
        spectator.setInput('size', 1);
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('btn-plus-small'));
        const paletteButton = spectator.query(byTestId('btn-palette-small'));
        const deleteButton = spectator.query(byTestId('btn-trash-small'));
        expect(addButton).toBeTruthy();
        expect(paletteButton).toBeTruthy();
        expect(deleteButton).toBeTruthy();
    });

    it('should trigger addContainer when click on plus button', () => {
        const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
        const addButton = spectator.query(byTestId('btn-plus'));

        spectator.dispatchFakeEvent(addButton, 'onClick');
        expect(addContainerMock).toHaveBeenCalled();
    });

    it('should trigger editStyle when click on palette button', () => {
        const editStyleMock = jest.spyOn(spectator.component.editStyle, 'emit');
        const paletteButton = spectator.query(byTestId('btn-palette'));

        spectator.dispatchFakeEvent(paletteButton, 'onClick');

        expect(editStyleMock).toHaveBeenCalled();
    });

    it('should trigger deleteContainer when click on container trash button', () => {
        const deleteContainerMock = jest.spyOn(spectator.component.deleteContainer, 'emit');
        const containerTrashButton = spectator.query(byTestId('btn-trash-container'));

        spectator.dispatchFakeEvent(containerTrashButton, 'onClick');

        expect(deleteContainerMock).toHaveBeenCalled();
    });

    it('should trigger deleteColumn when click on column trash button', () => {
        const deleteColumnMock = jest.spyOn(spectator.component.deleteColumn, 'emit');
        const columnTrashButton = spectator.query(byTestId('btn-trash-column'));

        spectator.dispatchFakeEvent(columnTrashButton, 'onClick');

        expect(deleteColumnMock).toHaveBeenCalled();
    });
});
