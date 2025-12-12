import { Component, Input } from '@angular/core';

import { DotIconComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-nav-icon',
    templateUrl: './dot-nav-icon.component.html',
    styleUrls: ['./dot-nav-icon.component.scss'],
    imports: [DotIconComponent]
})
export class DotNavIconComponent {
    @Input()
    icon: string;

    isFaIcon(icon: string): boolean {
        return icon.startsWith('fa-');
    }
}
