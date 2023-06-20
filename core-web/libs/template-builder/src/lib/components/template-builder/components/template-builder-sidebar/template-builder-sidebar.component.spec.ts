import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderSidebarComponent } from './template-builder-sidebar.component';

describe('TemplateBuilderSidebarComponent', () => {
    let spectator: SpectatorHost<TemplateBuilderSidebarComponent>;

    const createHost = createHostFactory({
        component: TemplateBuilderSidebarComponent,
        imports: [CommonModule, DropdownModule, FormsModule, HttpClientTestingModule],
        providers: [
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
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
});
