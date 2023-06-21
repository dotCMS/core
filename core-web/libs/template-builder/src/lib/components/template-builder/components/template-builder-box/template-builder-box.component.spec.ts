import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgFor, NgIf } from '@angular/common';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';
import { DotContainersServiceMock, mockMatchMedia } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import { CONTAINERS_DATA_MOCK, DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

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
            NoopAnimationsModule,
            DotMessagePipeModule
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
            `<dotcms-template-builder-box [width]="width" [items]="items"> </dotcms-template-builder-box>`,
            {
                hostProps: {
                    width: 10,
                    items: CONTAINERS_DATA_MOCK
                }
            }
        );

        spectator.detectChanges();

        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
        mockMatchMedia();
    });

    it('should create the component', () => {
        expect(spectator).toBeTruthy();
    });

    it('should render with default variant', () => {
        expect(spectator.query(byTestId('template-builder-box'))).toHaveClass(
            'template-builder-box--large'
        );
    });

    it('should render with medium variant and update the class', () => {
        spectator.setInput('width', 3);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box'))).toHaveClass(
            'template-builder-box--medium'
        );
    });

    it('should render with small variant and update the class', () => {
        spectator.setInput('width', 1);
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('template-builder-box-small'))).toHaveClass(
            'template-builder-box--small'
        );
    });

    it('should render the first ng-template for large and medium variants', () => {
        spectator.setInput('width', 10);
        spectator.detectComponentChanges();
        const firstTemplate = spectator.query(byTestId('template-builder-box'));
        const secondTemplate = spectator.query(byTestId('template-builder-box-small'));
        expect(firstTemplate).toBeTruthy();
        expect(secondTemplate).toBeNull();
    });

    it('should show all buttons for small variant', () => {
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
        const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
        const addButton = spectator.debugElement.query(By.css('.p-dropdown'));
        spectator.click(addButton);
        const option = spectator.query('.p-dropdown-item');

        spectator.click(option);
        expect(addContainerMock).toHaveBeenCalled();
    });

    it('should trigger editClasses when click on palette button', () => {
        const editStyleMock = jest.spyOn(spectator.component.editClasses, 'emit');
        const paletteButton = spectator.query(byTestId('box-style-class-button'));

        spectator.dispatchFakeEvent(paletteButton, 'onClick');

        expect(editStyleMock).toHaveBeenCalled();
    });

    it('should trigger deleteContainer when click on container trash button', () => {
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
        const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));

        spectator.dispatchFakeEvent(deleteButton, 'onClick');

        spectator.detectChanges();

        const confirmDelete = spectator.query('.p-confirm-popup-accept');

        spectator.dispatchFakeEvent(confirmDelete, 'click');

        expect(deleteMock).toHaveBeenCalled();
    });

    it('should trigger deleteColumnRejected when clicking on deleteColumn button and click no', () => {
        const rejectDeleteMock = jest.spyOn(spectator.component.deleteColumnRejected, 'emit');

        const deleteButton = spectator.query(byTestId('btn-remove-item'));

        spectator.dispatchFakeEvent(deleteButton, 'onClick');

        spectator.detectChanges();

        const rejectButton = spectator.query('.p-confirm-popup-reject');

        spectator.dispatchFakeEvent(rejectButton, 'click');

        expect(rejectDeleteMock).toHaveBeenCalled();
    });
});
