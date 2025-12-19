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

const HOST_TEMPLATE = `<dotcms-template-builder-box
    [width]="width"
    [actions]="actions"
    [items]="items"
    [containerMap]="containerMap">
</dotcms-template-builder-box>`;

const DEFAULT_HOST_PROPS = {
    actions: ['add', 'delete', 'edit'],
    width: 10,
    items: CONTAINERS_DATA_MOCK,
    containerMap: CONTAINER_MAP_MOCK
};

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
            spectator = createHost(HOST_TEMPLATE, { hostProps: DEFAULT_HOST_PROPS });
            expect(spectator).toBeTruthy();
        });

        it('should render with large variant (default)', () => {
            spectator = createHost(HOST_TEMPLATE, { hostProps: DEFAULT_HOST_PROPS });
            expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
                'template-builder-box--large'
            );
        });

        it('should render with medium variant', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, width: 3 }
            });
            expect(spectator.query(byTestId('template-builder-box')).classList).toContain(
                'template-builder-box--medium'
            );
        });

        it('should render with small variant', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, width: 1 }
            });
            expect(spectator.query(byTestId('template-builder-box-small')).classList).toContain(
                'template-builder-box--small'
            );
        });

        it('should render large template for large and medium variants', () => {
            spectator = createHost(HOST_TEMPLATE, { hostProps: DEFAULT_HOST_PROPS });
            expect(spectator.query(byTestId('template-builder-box'))).toBeTruthy();
            expect(spectator.query(byTestId('template-builder-box-small'))).toBeNull();
        });
    });

    describe('Actions visibility', () => {
        it('should hide edit button when not in actions', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, actions: ['add', 'delete'] }
            });
            expect(spectator.query(byTestId('box-style-class-button'))).toBeFalsy();
        });

        it('should show all buttons for small variant', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, width: 1 }
            });
            expect(spectator.query(byTestId('btn-plus-small'))).toBeTruthy();
            expect(spectator.query(byTestId('box-style-class-button-small'))).toBeTruthy();
            expect(spectator.query(byTestId('btn-remove-item'))).toBeTruthy();
        });
    });

    describe('Container operations', () => {
        beforeEach(() => {
            spectator = createHost(HOST_TEMPLATE, { hostProps: DEFAULT_HOST_PROPS });
        });

        it('should trigger addContainer when selecting from dropdown', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[0]
            });
            expect(addContainerMock).toHaveBeenCalled();
        });

        it('should emit addContainer with identifier when source is DB', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[0]
            });
            expect(addContainerMock).toHaveBeenCalledWith(containersMock[0]);
        });

        it('should emit addContainer with path as identifier when source is FILE', () => {
            const addContainerMock = jest.spyOn(spectator.component.addContainer, 'emit');
            spectator.triggerEventHandler("[data-testId='btn-plus']", 'onChange', {
                value: containersMock[2]
            });
            expect(addContainerMock).toHaveBeenCalledWith({
                ...containersMock[2],
                identifier: containersMock[2].path
            });
        });

        it('should trigger editClasses when clicking palette button', () => {
            const editStyleMock = jest.spyOn(spectator.component.editClasses, 'emit');
            spectator.dispatchFakeEvent(spectator.query(byTestId('box-style-class-button')), 'onClick');
            expect(editStyleMock).toHaveBeenCalled();
        });

        it('should display titles from container map', () => {
            const displayedTitles = spectator
                .queryAll(byTestId('container-title'))
                .map((el) => el.textContent.trim());
            const mapTitles = Object.values(CONTAINER_MAP_MOCK).map((c) => c.title);

            displayedTitles.forEach((title) => {
                expect(mapTitles).toContain(title);
            });
        });
    });

    describe('Delete operations', () => {
        beforeEach(() => {
            spectator = createHost(HOST_TEMPLATE, { hostProps: DEFAULT_HOST_PROPS });
        });

        it('should trigger deleteContainer when confirm is accepted', () => {
            const deleteContainerMock = jest.spyOn(spectator.component.deleteContainer, 'emit');
            spectator.triggerEventHandler(
                "[data-testId='btn-trash-container']",
                'deleteConfirmed',
                null
            );
            expect(deleteContainerMock).toHaveBeenCalled();
        });

        it('should trigger deleteColumn when confirm is accepted', () => {
            const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');
            spectator.triggerEventHandler(
                "[data-testId='btn-delete-column']",
                'deleteConfirmed',
                null
            );
            expect(deleteMock).toHaveBeenCalled();
        });

        it('should trigger deleteColumnRejected when confirm is rejected', () => {
            const rejectMock = jest.spyOn(spectator.component.deleteColumnRejected, 'emit');
            spectator.triggerEventHandler(
                "[data-testId='btn-delete-column']",
                'deleteRejected',
                null
            );
            expect(rejectMock).toHaveBeenCalled();
        });
    });

    describe('Delete with empty containers', () => {
        it('should skip confirmation when there are no containers', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, items: [] }
            });
            const deleteMock = jest.spyOn(spectator.component.deleteColumn, 'emit');

            spectator.dispatchFakeEvent(spectator.query(byTestId('btn-remove-item')), 'onClick');
            spectator.detectChanges();

            expect(deleteMock).toHaveBeenCalled();
        });
    });

    describe('Dialog', () => {
        it('should open dialog when clicking plus button on small variant', async () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, width: 1 }
            });

            expect(spectator.query(byTestId('template-builder-box-small'))).toExist();

            const plusButton = spectator.query(byTestId('btn-plus-small'));
            spectator.dispatchFakeEvent(plusButton, 'onClick');

            await spectator.fixture.whenStable();
            spectator.detectChanges();

            const dialog = spectator.query(byTestId('edit-box-dialog'));
            expect(dialog?.querySelector("[data-testId='template-builder-box']")).toExist();
            expect(spectator.component.dialogVisible).toBeTruthy();
        });

        it('should not open dialog on large variant', () => {
            spectator = createHost(HOST_TEMPLATE, {
                hostProps: { ...DEFAULT_HOST_PROPS, width: 5 }
            });

            expect(spectator.query(byTestId('btn-plus'))).toExist();
            expect(spectator.component.dialogVisible).toBeFalsy();
        });
    });
});
