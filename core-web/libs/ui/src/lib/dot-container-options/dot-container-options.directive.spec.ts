import { createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import {
    containersMock,
    DotContainersServiceMock,
    MockDotMessageService,
    mockMatchMedia
} from '@dotcms/utils-testing';

import { DotContainerOptionsDirective } from './dot-container-options.directive';
import { MockContainersDropdownComponent } from './mock-containers-dropdown.component';

describe('ContainerOptionsDirective', () => {
    let spectator: SpectatorHost<MockContainersDropdownComponent>;

    const createHost = createHostFactory({
        component: MockContainersDropdownComponent,
        imports: [BrowserAnimationsModule, DotContainerOptionsDirective, DropdownModule],
        providers: [
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        spectator = createHost(`<dot-containers-dropdown-mock></dot-containers-dropdown-mock>`);
        spectator.detectChanges();
        mockMatchMedia();
    });

    it('should add the options obtained from the service', async () => {
        const dropdownButton = spectator.query('.p-dropdown');
        spectator.click(dropdownButton);
        const options = spectator.debugElement.queryAll(By.css('.p-dropdown-item'));
        expect(options.length).toEqual(containersMock.length);
    });
});
