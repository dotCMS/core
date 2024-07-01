import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

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

const sortedContainersMock = containersMock
    .map((container) => ({
        label: container.title,
        value: container,
        inactive: false
    }))
    .sort((a, b) => a.label.localeCompare(b.label));

function getGroupByHostContainersMock() {
    const containerobj = sortedContainersMock.reduce((acc, option) => {
        const { hostname } = option.value.parentPermissionable;

        if (!acc[hostname]) {
            acc[hostname] = { items: [] };
        }

        acc[hostname].items.push(option);

        return acc;
    }, {});

    return Object.keys(containerobj).map((key) => {
        return {
            label: key,
            items: containerobj[key].items
        };
    });
}

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

    it('should set the group property of the dropdown to true', () => {
        // get the dropdown component
        const dropdown = spectator.debugElement.query(By.css('p-dropdown'));
        // Get the dropdown component instance
        const dropdownInstance = dropdown.componentInstance;

        expect(dropdownInstance.group).toBeTruthy();
    });

    it('should group containers by host', () => {
        const dropdown = spectator.debugElement.query(By.css('p-dropdown'));
        const dropdownInstance = dropdown.componentInstance;

        expect(dropdownInstance.options).toEqual(getGroupByHostContainersMock());
    });
});
