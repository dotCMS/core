import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-nav-icon',
    templateUrl: './dot-nav-icon.component.html',
    styleUrls: ['./dot-nav-icon.component.scss']
})
export class DotNavIconComponent {
    @Input()
    icon: string;

    constructor() {}

    isFaIcon(icon: string): boolean {
        return icon.startsWith('fa-');
    }
}
