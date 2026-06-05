import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotNameFormatPipe } from '../../../../pipes/name-format.pipe';

interface ContentSidebarInformation {
    contentlet: DotCMSContentlet | null;
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
        RouterLink,
        TooltipModule,
        DotRelativeDatePipe,
        DotMessagePipe,
        DotNameFormatPipe,
        DotCopyButtonComponent,
        DatePipe
    ],
    templateUrl: './dot-edit-content-sidebar-information.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col gap-2'
    }
})
export class DotEditContentSidebarInformationComponent {
    /** The sidebar data including the contentlet, content type, loading state, and references count. */
    readonly $data = input.required<ContentSidebarInformation>({ alias: 'data' });

    /** URL to fetch the contentlet as JSON via the REST API. */
    readonly $jsonUrl = computed(
        () => `/api/v1/content/${this.$data().contentlet?.identifier ?? ''}`
    );

    /** Initials of the last modifier, shown in the "Modified by" avatar chip. */
    readonly $modifiedByInitials = computed(() => {
        const name = String(this.$data().contentlet?.modUserName ?? '').trim();
        const parts = name.split(/\s+/).filter(Boolean);
        if (!parts.length) {
            return '?';
        }

        const first = parts[0].charAt(0);
        const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';

        return (first + last).toUpperCase();
    });
}
