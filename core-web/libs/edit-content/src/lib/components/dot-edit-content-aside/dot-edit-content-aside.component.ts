import { NgIf, NgSwitch, NgSwitchCase, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import { DotApiLinkComponent, DotCopyButtonComponent, DotRelativeDatePipe } from '@dotcms/ui';

import { EditContentFormData } from '../../models/dot-edit-content-form.interface';

@Component({
    selector: 'dot-edit-content-aside',
    standalone: true,
    imports: [
        NgIf,
        NgSwitch,
        NgSwitchCase,
        SlicePipe,
        DotApiLinkComponent,
        DotCopyButtonComponent,
        DotRelativeDatePipe,
        ChipModule
    ],
    templateUrl: './dot-edit-content-aside.component.html',
    styleUrls: ['./dot-edit-content-aside.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentAsideComponent {
    @Input() asideData!: EditContentFormData;
}
