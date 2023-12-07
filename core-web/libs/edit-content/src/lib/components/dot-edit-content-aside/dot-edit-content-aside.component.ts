import { NgIf, NgSwitch, NgSwitchCase, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { EditContentFormData } from '../../models/dot-edit-content-form.interface';

@Component({
    selector: 'dot-edit-content-aside',
    standalone: true,
    templateUrl: './dot-edit-content-aside.component.html',
    styleUrls: ['./dot-edit-content-aside.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgIf,
        NgSwitch,
        NgSwitchCase,
        SlicePipe,
        DotApiLinkComponent,
        DotCopyButtonComponent,
        DotRelativeDatePipe,
        ChipModule,
        DotMessagePipe,
        SlicePipe
    ]
})
export class DotEditContentAsideComponent {
    @Input() asideData!: EditContentFormData;
}
