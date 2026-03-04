import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { ContentletStatusTagPipe } from '../../../../pipes/contentlet-status-tag.pipe';
import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

interface ContentSidebarInformation {
    contentlet: DotCMSContentlet;
    contentType: DotCMSContentType;
    loading: boolean;
    referencesPageCount: string;
}

/**
 * Component that displays the information of a contentlet in the sidebar.
 */
@Component({
    selector: 'dot-edit-content-sidebar-information',
    imports: [
        CommonModule,
        RouterLink,
        TooltipModule,
        SkeletonModule,
        TagModule,
        ContentletStatusTagPipe,
        DotRelativeDatePipe,
        DotMessagePipe,
        DotNameFormatPipe
    ],
    templateUrl: './dot-edit-content-sidebar-information.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col gap-2'
    }
})
export class DotEditContentSidebarInformationComponent {
    #dotMessageService = inject(DotMessageService);

    /**
     * Input that contains the data of the contentlet.
     */
    $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    /**
     * Computed that contains the url to the contentlet.
     */
    $jsonUrl = computed(() => `/api/v1/content/${this.$data().contentlet.identifier}`);

    /**
     * Computed that returns a tooltip message when creation date doesn't exist
     */
    $createdTooltipMessage = computed(() => {
        const { contentlet } = this.$data();

        return !contentlet?.creationDate
            ? this.#dotMessageService.get('edit.content.sidebar.information.no.created.yet')
            : null;
    });
}
