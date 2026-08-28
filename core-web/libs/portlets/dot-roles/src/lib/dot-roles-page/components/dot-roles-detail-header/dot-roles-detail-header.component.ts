import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
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
    readonly #messageService = inject(DotMessageService);

    protected readonly $icon = computed(() =>
        this.store.selectedRoleIsParent() ? 'folder' : 'shield_person'
    );

    protected onEditRole(): void {
        const role = this.store.selectedRole();
        if (!role) {
            return;
        }

        this.#dialogService.open(DotRolesEditComponent, {
            header: this.#messageService.get('roles.edit.title'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { role }
        });
    }
}
