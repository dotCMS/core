import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotCopyButtonComponent,
    DotLinkComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';

@Component({
    selector: 'dot-content-aside-information',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        TooltipModule,
        ChipModule,
        SkeletonModule,
        DotCopyButtonComponent,
        DotLinkComponent,
        ContentletStatusPipe,
        DotRelativeDatePipe,
        DotMessagePipe
    ],
    templateUrl: './dot-content-aside-information.component.html',
    styleUrl: './dot-content-aside-information.component.scss'
})
export class DotContentAsideInformationComponent {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: DotCMSContentType;
    @Input() loading!: boolean;
    protected readonly currentDate = new Date();
}
