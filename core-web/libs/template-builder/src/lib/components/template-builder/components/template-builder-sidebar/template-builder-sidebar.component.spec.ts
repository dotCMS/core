import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderSidebarComponent } from './template-builder-sidebar.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';
import { TemplateBuilderBoxComponent } from '../template-builder-box/template-builder-box.component';

describe('TemplateBuilderSidebarComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderSidebarComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderSidebarComponent,
        imports: [
            DropdownModule,
            FormsModule,
            HttpClientTestingModule,
            TemplateBuilderBoxComponent,
            DotMessagePipeModule
        ],
        providers: [
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            }
        ]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dotcms-template-builder-sidebar [sidebarProperties]="sidebarProperties"></dotcms-template-builder-sidebar>`,
            {
                hostProps: {
                    sidebarProperties: {
                        postion: 'left',
                        width: 'medium',
                        containers: []
                    }
                }
            }
        );

        spectator.detectChanges();
    });

    it('should emit widthChange when a width is selected in the dropdown', () => {
        const spy = jest.spyOn(spectator.component.sidebarWidthChange, 'emit');
        const dropdown = spectator.query(byTestId('select-sidebar-width'));

        spectator.dispatchFakeEvent(dropdown, 'onChange');

        expect(spy).toHaveBeenCalledWith('medium'); // Default in case is undefined
    });

    it('should have a TemplateBuilderBox Component', () => {
        expect(spectator.query('dotcms-template-builder-box')).toBeTruthy();
    });
});
