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

/**
 * Component that displays the information of a contentlet in the sidebar.
 */
@Component({
    selector: 'dot-edit-content-sidebar-information',
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
    templateUrl: './dot-edit-content-sidebar-information.component.html',
    styleUrl: './dot-edit-content-sidebar-information.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarInformationComponent {
    /**
     * Input that contains the data of the contentlet.
     */
    $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    /**
     * Computed that contains the url to the contentlet.
     */
    $jsonUrl = computed(() => `/api/content/id/${this.$data().contentlet.identifier}`);
}
