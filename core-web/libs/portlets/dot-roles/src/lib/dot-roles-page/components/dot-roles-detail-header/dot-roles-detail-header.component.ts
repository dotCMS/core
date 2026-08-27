import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesEditComponent } from '../../../dot-roles-edit/dot-roles-edit.component';
import { DotRolesStore } from '../../store/dot-roles.store';

@Component({
    selector: 'dot-roles-detail-header',
    standalone: true,
    imports: [ButtonModule, DynamicDialogModule, SkeletonModule, DotMessagePipe],
    providers: [DialogService],
    templateUrl: './dot-roles-detail-header.component.html',
    host: { class: 'block' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRolesDetailHeaderComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #dialogService = inject(DialogService);

    protected readonly $icon = computed(() =>
        this.store.selectedRoleIsParent() ? 'folder' : 'shield_person'
    );

    protected onEditRole(): void {
        const role = this.store.selectedRole();
        if (!role) {
            return;
        }

        this.#dialogService.open(DotRolesEditComponent, {
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { role }
        });
    }
}
