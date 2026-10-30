import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-nav-icon',
    templateUrl: './dot-nav-icon.component.html',
    styleUrls: ['./dot-nav-icon.component.scss'],
    standalone: false
})
export class DotNavIconComponent {
    @Input()
    icon: string;

    isFaIcon(icon: string): boolean {
        return icon.startsWith('fa-');
    }
}
