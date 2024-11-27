import { ChangeDetectionStrategy, Component, inject, model } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import { Content, ExistingContentStore } from './store/existing-content.store';

@Component({
    selector: 'dot-select-existing-content',
    standalone: true,
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule
    ],
    templateUrl: './dot-select-existing-content.component.html',
    styleUrls: ['./dot-select-existing-content.component.scss'],
    providers: [ExistingContentStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectExistingContentComponent {
    /**
     * A readonly instance of the ExistingContentStore injected into the component.
     * This store is used to manage the state and actions related to the existing content.
     */
    readonly store = inject(ExistingContentStore);

    $visible = model(false, { alias: 'visible' });

    $selectedItems = model<Content[]>([]);

    closeDialog() {
        this.$visible.set(false);
    }
}
