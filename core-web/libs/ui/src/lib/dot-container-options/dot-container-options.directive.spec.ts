import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Dropdown, DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import {
    containersMock,
    DotContainersServiceMock,
    MockDotMessageService,
    mockMatchMedia
} from '@dotcms/utils-testing';

import { DotContainerOptionsDirective } from './dot-container-options.directive';

@Component({
    selector: `dot-containers-dropdown-mock`,
    imports: [DropdownModule, DotContainerOptionsDirective],
    template: `
        <p-dropdown dotContainerOptions />
    `
})
class MockContainersDropdownComponent {}

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
        imports: [BrowserAnimationsModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: DotContainersService,
                useValue: new DotContainersServiceMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createHost(`<dot-containers-dropdown-mock />`);
        spectator.detectChanges();
        mockMatchMedia();
    });

    it('should set the group property of the dropdown to true', async () => {
        await spectator.fixture.whenStable();
        const dropdown = spectator.query(Dropdown);
        expect(dropdown.group).toBeTruthy();
    });

    it('should group containers by host', () => {
        const dropdown = spectator.query(Dropdown);
        expect(dropdown.options).toEqual(getGroupByHostContainersMock());
    });
});
