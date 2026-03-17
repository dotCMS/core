import { Component, Input, ViewEncapsulation } from '@angular/core';
import { ReactiveFormsModule, UntypedFormGroup } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { PopoverModule } from 'primeng/popover';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLayoutPropertiesItemComponent } from './dot-layout-properties-item/dot-layout-properties-item.component';
import { DotLayoutSidebarComponent } from './dot-layout-property-sidebar/dot-layout-property-sidebar.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-layout-properties',
    templateUrl: './dot-layout-properties.component.html',
    styleUrls: ['./dot-layout-properties.component.scss'],
    encapsulation: ViewEncapsulation.None,
    imports: [
        DotLayoutPropertiesItemComponent,
        DotLayoutSidebarComponent,
        PopoverModule,
        ButtonModule,
        ReactiveFormsModule,
        DotMessagePipe
    ]
})
export class DotLayoutPropertiesComponent {
    @Input() group: UntypedFormGroup;
}
