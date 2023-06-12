import { createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { containersMock, DotContainersServiceMock } from '@dotcms/utils-testing';

import { ContainerOptionsDirective } from './container-options.directive';

import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    MockContainersDropdownComponent,
    mockMatchMedia
} from '../components/template-builder/utils/mocks';

mockMatchMedia();
describe('ContainerOptionsDirective', () => {
    let spectator: SpectatorHost<MockContainersDropdownComponent>;

    const createHost = createHostFactory({
        component: MockContainersDropdownComponent,
        imports: [BrowserAnimationsModule, ContainerOptionsDirective, DropdownModule],
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
            `<dotcms-containers-dropdown-mock></dotcms-containers-dropdown-mock>`
        );
        spectator.detectChanges();
    });

    it('should add the options obtained from the service', async () => {
        const dropdownButton = spectator.query('.p-dropdown');
        spectator.click(dropdownButton);
        const options = spectator.debugElement.queryAll(By.css('.p-dropdown-item'));
        expect(options.length).toEqual(containersMock.length);
    });
});
