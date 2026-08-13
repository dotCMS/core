import { Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesEditComponent } from '../../../dot-roles-edit/dot-roles-edit.component';
import { DotRolesStore } from '../../store/dot-roles.store';

@Component({
    selector: 'dot-roles-detail-header',
    standalone: true,
    imports: [ButtonModule, DynamicDialogModule, DotMessagePipe],
    providers: [DialogService],
    templateUrl: './dot-roles-detail-header.component.html',
    host: { class: 'block' }
})
export class DotRolesDetailHeaderComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #dialogService = inject(DialogService);

    protected readonly $icon = computed(() => {
        const role = this.store.selectedRole();
        if (!role) {
            return 'shield';
        }
        // Parents (with children) get a folder icon; leaves get a shield.
        return (role.children?.length ?? 0) > 0 ? 'folder' : 'shield';
    });

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
