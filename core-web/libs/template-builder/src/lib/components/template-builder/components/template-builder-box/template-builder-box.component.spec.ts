import { expect } from '@jest/globals';
import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgFor, NgIf } from '@angular/common';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock, mockMatchMedia } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import {
    CONTAINER_MAP_MOCK,
    CONTAINERS_DATA_MOCK,
    DOT_MESSAGE_SERVICE_TB_MOCK
} from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

// todo check for assertions
describe('TemplateBuilderBoxComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderBoxComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderBoxComponent,
        imports: [
            NgClass,
            NgIf,
            NgFor,
            ButtonModule,
            ScrollPanelModule,
            RemoveConfirmDialogComponent,
            NoopAnimationsModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-box [width]="width" [items]="items" [containerMap]="containerMap"> </dotcms-template-builder-box>`,
            {
                hostProps: {
                    width: 10,
                    items: CONTAINERS_DATA_MOCK,
                    containerMap: CONTAINER_MAP_MOCK,
                    showEditStyleButton: true
                }
            }
        );

        spectator.detectChanges();

        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
        mockMatchMedia();
    });

    it('should create the component', () => {
        expect.assertions(1);
        expect(spectator).toBeTruthy();
    });

    it('should render with default variant', () => {
        expect.assertions(1);
        expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
            'template-builder-box--large'
        );
    });

    it('should render with medium variant and update the class', () => {
        expect.assertions(1);
        spectator.setInput('width', 3);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
            'template-builder-box--medium'
        );
    });

    it('should render with small variant and update the class', () => {
        expect.assertions(1);
        spectator.setInput('width', 1);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box-small')).classList).toContain(
            'template-builder-box--small'
        );
    });

    it('should render the first ng-template for large and medium variants', () => {
        expect.assertions(2);
        spectator.setInput('width', 10);
        spectator.detectComponentChanges();
        const firstTemplate = spectator.query(byTestId('template-builder-box'));
        const secondTemplate = spectator.query(byTestId('template-builder-box-small'));
        expect(firstTemplate).toBeTruthy();
        expect(secondTemplate).toBeNull();
    });

    it('should only show the specified actions on actions input', () => {
        expect.assertions(1);
        spectator.setInput('actions', ['add', 'delete']); // Here we hide the edit button
        spectator.detectComponentChanges();

        const paletteButton = spectator.query(byTestId('box-style-class-button'));

        expect(paletteButton).toBeFalsy();
    });

    it('should show all buttons for small variant', () => {
        expect.assertions(3);
        spectator.setInput('width', 1);
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('btn-plus-small'));
        const paletteButton = spectator.query(byTestId('box-style-class-button-small'));
        const deleteButton = spectator.query(byTestId('btn-remove-item'));
        expect(addButton).toBeTruthy();
        expect(paletteButton).toBeTruthy();
        expect(deleteButton).toBeTruthy();
    });

    it('should trigger addContainer when click on plus button', () => {
        expect.assertions(1);
        const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
        const addButton = spectator.debugElement.query(By.css('.p-dropdown'));
        spectator.click(addButton);
        const option = spectator.query('.p-dropdown-item');

        spectator.click(option);
        expect(addContainerMock).toHaveBeenCalled();
    });

    it('should trigger editClasses when click on palette button', () => {
        expect.assertions(1);
        const editStyleMock = jest.spyOn(spectator.component.editClasses, 'emit');
        const paletteButton = spectator.query(byTestId('box-style-class-button'));

        spectator.dispatchFakeEvent(paletteButton, 'onClick');

        expect(editStyleMock).toHaveBeenCalled();
    });

    it('should trigger deleteContainer when click on container trash button', () => {
        expect.assertions(1);
        const deleteContainerMock = jest.spyOn(spectator.component.deleteContainer, 'emit');
        const containerTrashButton = spectator.query(byTestId('btn-trash-container'));
        const removeContainerButton = containerTrashButton.querySelector(
            '[data-testId="btn-remove-item"]'
        );

        spectator.dispatchFakeEvent(removeContainerButton, 'onClick');
        spectator.detectChanges();
        const confirmButton = spectator.query('.p-confirm-popup-accept');
        spectator.click(confirmButton);

        expect(deleteContainerMock).toHaveBeenCalled();
    });

    it('should trigger deleteColumn when clicking on deleteColumn button and click yes', () => {
        expect.assertions(1);
        const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));

        spectator.dispatchFakeEvent(deleteButton, 'onClick');

        spectator.detectChanges();

        const confirmDelete = spectator.query('.p-confirm-popup-accept');

        spectator.dispatchFakeEvent(confirmDelete, 'click');

        expect(deleteMock).toHaveBeenCalled();
    });

    it('should trigger deleteColumnRejected when clicking on deleteColumn button and click no', () => {
        expect.assertions(1);
        const rejectDeleteMock = jest.spyOn(spectator.component.deleteColumnRejected, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));

        spectator.dispatchFakeEvent(deleteButton, 'onClick');

        spectator.detectChanges();

        const rejectButton = spectator.query('.p-confirm-popup-reject');

        spectator.dispatchFakeEvent(rejectButton, 'click');

        expect(rejectDeleteMock).toHaveBeenCalled();
    });

    it('should use titles from container map', (done) => {
        const displayedContainerTitles = spectator
            .queryAll(byTestId('container-title'))
            .map((element) => element.textContent.trim());
        const containerMapTitles = Object.values(CONTAINER_MAP_MOCK).map(
            (container) => container.title
        );

        displayedContainerTitles.forEach((title) => {
            if (!containerMapTitles.includes(title)) {
                throw new Error(`title: "${title} not included the container map is displayed`);
            }
        });

        done();
    });
});
