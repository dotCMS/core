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

    describe('Dropdown list', () => {
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

        it('should set the options sorted by naame', () => {
            const dropdown = spectator.debugElement.query(By.css('p-dropdown'));
            const dropdownInstance = dropdown.componentInstance;

            expect(dropdownInstance.options).toEqual(sortedContainersMock);
        });
    });

    describe('Dropdown list group by host', () => {
        beforeEach(() => {
            spectator = createHost(
                `<dot-containers-dropdown-mock [groupByHost]="true"></dot-containers-dropdown-mock>`
            );
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
});
