import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { NgClass, NgFor, NgIf } from '@angular/common';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { DialogModule } from 'primeng/dialog';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { containersMock, DotContainersServiceMock, mockMatchMedia } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import {
    CONTAINER_MAP_MOCK,
    CONTAINERS_DATA_MOCK,
    DOT_MESSAGE_SERVICE_TB_MOCK
} from '../../utils/mocks';
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
            DialogModule,
            DotMessagePipe
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            ConfirmationService
        ]
    });

    beforeEach(() => {
        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());
        mockMatchMedia();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Variant rendering', () => {
        it('should create the component', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            expect(spectator).toBeTruthy();
        });

        it('should render with default variant', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
                'template-builder-box--large'
            );
        });

        it('should render with medium variant and update the class', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 3,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
                'template-builder-box--medium'
            );
        });

        it('should render with small variant and update the class', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 1,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            expect(spectator.query(byTestId('template-builder-box-small')).classList).toContain(
                'template-builder-box--small'
            );
        });

        it('should render the first ng-template for large and medium variants', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            const firstTemplate = spectator.query(byTestId('template-builder-box'));
            const secondTemplate = spectator.query(byTestId('template-builder-box-small'));
            expect(firstTemplate).toBeTruthy();
            expect(secondTemplate).toBeNull();
        });
    });

    describe('Actions', () => {
        it('should only show the specified actions on actions input', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete'], // Here we hide the edit button
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );

            const paletteButton = spectator.query(byTestId('box-style-class-button'));
            expect(paletteButton).toBeFalsy();
        });

        it('should show all buttons for small variant', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 1,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );

            const addButton = spectator.query(byTestId('btn-plus-small'));
            const paletteButton = spectator.query(byTestId('box-style-class-button-small'));
            const deleteButton = spectator.query(byTestId('btn-remove-item'));
            expect(addButton).toBeTruthy();
            expect(paletteButton).toBeTruthy();
            expect(deleteButton).toBeTruthy();
        });
    });

    describe('Container operations', () => {
        beforeEach(() => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
        });

        it('should trigger addContainer when selecting from dropdown', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[0]
            });
            expect(addContainerMock).toHaveBeenCalled();
        });

        it('should emit addContainer with a identifier as identifier when source is DB', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');

            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[0]
            });

            expect(addContainerMock).toHaveBeenCalledWith(containersMock[0]);
        });

        it('should emit addContainer with a path as identifier when source is FILE', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');

            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[2]
            });

            expect(addContainerMock).toHaveBeenCalledWith({
                ...containersMock[2],
                identifier: containersMock[2].path
            });
        });

        it('should trigger editClasses when click on palette button', () => {
            const editStyleMock = jest.spyOn(spectator.component.editClasses, 'emit');
            const paletteButton = spectator.query(byTestId('box-style-class-button'));

            spectator.dispatchFakeEvent(paletteButton, 'onClick');

            expect(editStyleMock).toHaveBeenCalled();
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

    describe('Delete operations', () => {
        it('should trigger deleteContainer when confirm is accepted', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            const deleteContainerMock = jest.spyOn(spectator.component.deleteContainer, 'emit');

            // Trigger the deleteConfirmed output event on the container's remove dialog using CSS selector
            spectator.triggerEventHandler(
                "[data-testId='btn-trash-container']",
                'deleteConfirmed',
                null
            );

            expect(deleteContainerMock).toHaveBeenCalled();
        });

        it('should trigger deleteColumn when confirm is accepted', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');

            // Trigger deleteConfirmed on the column delete dialog
            spectator.triggerEventHandler(
                "[data-testId='btn-delete-column']",
                'deleteConfirmed',
                null
            );

            expect(deleteMock).toHaveBeenCalled();
        });

        it('should trigger deleteColumnRejected when confirm is rejected', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            const rejectDeleteMock = jest.spyOn(spectator.component.deleteColumnRejected, 'emit');

            // Trigger deleteRejected on the column delete dialog
            spectator.triggerEventHandler(
                "[data-testId='btn-delete-column']",
                'deleteRejected',
                null
            );

            expect(rejectDeleteMock).toHaveBeenCalled();
        });

        it('should trigger deleteColumn when clicking on deleteColumn button and there are no containers', () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 10,
                        items: [],
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );
            const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');

            const deleteButton = spectator.query(byTestId('btn-remove-item'));
            spectator.dispatchFakeEvent(deleteButton, 'onClick');
            spectator.detectChanges();

            expect(deleteMock).toHaveBeenCalled();
        });
    });

    describe('Dialog', () => {
        it('should open dialog when click on edit button', async () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 1,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );

            const templateBuilderSmallBox = spectator.query(byTestId('template-builder-box-small'));
            const plusButton = spectator.query(byTestId('btn-plus-small'));
            expect(templateBuilderSmallBox).toExist();
            expect(plusButton).toExist();

            // Use dispatchFakeEvent() to simulate a click event
            spectator.dispatchFakeEvent(plusButton, 'onClick');

            // Use whenStable() to wait for asynchronous tasks to complete
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            const dialog = spectator.query(byTestId('edit-box-dialog'));
            const templateBuilderBox = dialog?.querySelector(
                "[data-testId='template-builder-box']"
            );

            expect(templateBuilderBox).toExist();
            expect(spectator.component.dialogVisible).toBeTruthy();
        });

        it('should not open dialog when the size is large', async () => {
            spectator = createHost(
                `<dotcms-template-builder-box [width]="width" [actions]="actions" [items]="items" [containerMap]="containerMap"></dotcms-template-builder-box>`,
                {
                    hostProps: {
                        actions: ['add', 'delete', 'edit'],
                        width: 5,
                        items: CONTAINERS_DATA_MOCK,
                        containerMap: CONTAINER_MAP_MOCK
                    }
                }
            );

            const plusButton = spectator.query(byTestId('btn-plus'));
            expect(plusButton).toExist();

            // The large variant uses p-select which doesn't trigger dialog
            expect(spectator.component.dialogVisible).toBeFalsy();
        });
    });
});
