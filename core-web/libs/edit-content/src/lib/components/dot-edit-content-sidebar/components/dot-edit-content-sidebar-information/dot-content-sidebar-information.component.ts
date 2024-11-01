import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotCopyButtonComponent,
    DotLinkComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

interface ContentSidebarInformation {
    contentlet: DotCMSContentlet;
    contentType: DotCMSContentType;
    loading: boolean;
    referencesPageCount: number;
}

@Component({
    selector: 'dot-content-sidebar-information',
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
        DotMessagePipe,
        DotNameFormatPipe
    ],
    templateUrl: './dot-content-sidebar-information.component.html',
    styleUrl: './dot-content-sidebar-information.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentSidebarInformationComponent {
    readonly VIEW_CONTENTLET_URL = `/api/content/id/`;

    $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    $jsonUrl = computed(() => `${this.VIEW_CONTENTLET_URL}/${this.$data().contentlet.identifier}`);
    protected readonly currentDate = new Date();
}
