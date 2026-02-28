import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderSidebarComponent } from './template-builder-sidebar.component';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    GRIDSTACK_DATA_MOCK,
    INITIAL_STATE_MOCK
} from '../../utils/mocks';
import { TemplateBuilderBoxComponent } from '../template-builder-box/template-builder-box.component';

describe('TemplateBuilderSidebarComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderSidebarComponent>;
    let store: DotTemplateBuilderStore;
    let boxComponent: TemplateBuilderBoxComponent;

    const createHost = createHostFactory({
        component: TemplateBuilderSidebarComponent,
        imports: [
            SelectModule,
            FormsModule,
            HttpClientTestingModule,
            TemplateBuilderBoxComponent,
            DotMessagePipe
        ],
        providers: [
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DotTemplateBuilderStore
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-sidebar [sidebarProperties]="sidebarProperties"></dotcms-template-builder-sidebar>`,
            {
                hostProps: {
                    sidebarProperties: {
                        location: 'left',
                        width: 'medium',
                        containers: []
                    }
                }
            }
        );

        store = spectator.inject(DotTemplateBuilderStore);

        boxComponent = spectator.query(TemplateBuilderBoxComponent);

        store.setState({
            ...INITIAL_STATE_MOCK,
            rows: GRIDSTACK_DATA_MOCK,
            layoutProperties: {
                header: true,
                footer: true,
                sidebar: {
                    location: 'left',
                    width: 'medium',
                    containers: []
                }
            }
        });

        spectator.detectChanges();
    });

    it('should emit widthChange when a width is selected in the dropdown', () => {
        const sidebarUpdateMock = jest.spyOn(store, 'updateSidebarWidth');
        const dropdown = spectator.query(byTestId('select-sidebar-width'));

        spectator.dispatchFakeEvent(dropdown, 'onChange');

        expect(sidebarUpdateMock).toHaveBeenCalledWith('medium'); // Default in case is undefined
    });

    it('should have a TemplateBuilderBox Component', () => {
        expect(spectator.query('dotcms-template-builder-box')).toBeTruthy();
    });

    it('should trigger addSidebarContainer when box component emits addContainer', () => {
        const sidebarAddContainerMock = jest.spyOn(store, 'addSidebarContainer');

        boxComponent.addContainer.emit();

        expect(sidebarAddContainerMock).toHaveBeenCalled();
    });

    it('should trigger deleteSidebarContainer when box component emits deleteContainer', () => {
        const sidebarAddContainerMock = jest.spyOn(store, 'deleteSidebarContainer');

        boxComponent.deleteContainer.emit();

        expect(sidebarAddContainerMock).toHaveBeenCalled();
    });
});
