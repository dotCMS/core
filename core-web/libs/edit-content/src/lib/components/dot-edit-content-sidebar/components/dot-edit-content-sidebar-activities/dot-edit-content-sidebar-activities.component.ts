import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { Activity } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarActivitiesSkeletonComponent } from '../dot-edit-content-sidebar-activities-skeleton/dot-edit-content-sidebar-activities-skeleton.component';

/**
 * Component that displays a list of activities in the content sidebar
 */
@Component({
    selector: 'dot-edit-content-sidebar-activities',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextareaModule,
        DotMessagePipe,
        SkeletonModule,
        AvatarModule,
        DotRelativeDatePipe,
        DotEditContentSidebarActivitiesSkeletonComponent,
        DotGravatarDirective
    ],
    templateUrl: './dot-edit-content-sidebar-activities.component.html',
    styleUrls: ['./dot-edit-content-sidebar-activities.component.scss'],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarActivitiesComponent {
    $activities = input<Activity[]>([], { alias: 'activities' });
    $state = input<ComponentStatus>(ComponentStatus.LOADING, { alias: 'state' });

    $isLoading = computed(() => this.$state() === ComponentStatus.LOADING);
}
