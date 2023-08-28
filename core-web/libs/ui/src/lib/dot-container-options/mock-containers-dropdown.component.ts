import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-containers-dropdown-mock',
    template: ` <p-dropdown [groupByHost]="groupByHost" dotContainerOptions></p-dropdown>`
})
export class MockContainersDropdownComponent {
    @Input() groupByHost = false;
}
