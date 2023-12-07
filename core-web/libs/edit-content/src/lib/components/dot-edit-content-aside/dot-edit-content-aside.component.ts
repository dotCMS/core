import { NgIf, NgSwitch, NgSwitchCase, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { ContentletStatusPipe } from '../../pipes/contentlet-status.pipe';

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
        SlicePipe,
        ContentletStatusPipe
    ]
})
export class DotEditContentAsideComponent {
    @Input() contentLet!: DotCMSContentlet;
    @Input() contentType!: string;
}
