import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveBaseTypeSelectorComponent } from './components/dot-content-drive-base-type-selector/dot-content-drive-base-type-selector.component';
import { DotContentDriveContentTypeFieldComponent } from './components/dot-content-drive-content-type-field/dot-content-drive-content-type-field.component';
import { DotContentDriveLanguageFieldComponent } from './components/dot-content-drive-language-field/dot-content-drive-language-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';
import { DotContentDriveWorkflowActionsComponent } from './components/dot-content-drive-workflow-actions/dot-content-drive-workflow-actions.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [
        ToolbarModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DotContentDriveTreeTogglerComponent,
        DotContentDriveBaseTypeSelectorComponent,
        DotContentDriveContentTypeFieldComponent,
        DotContentDriveSearchInputComponent,
        DotContentDriveLanguageFieldComponent,
        DotContentDriveWorkflowActionsComponent
    ],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

    $addNewDotAsset = output<void>({ alias: 'addNewDotAsset' });

    readonly $items = signal<MenuItem[]>([
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.folder'),
            command: () => {
                this.#store.setDialog({
                    type: DIALOG_TYPE.FOLDER,
                    header: this.#dotMessageService.get('content-drive.dialog.folder.header')
                });
            }
        },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.asset'),
            command: () => {
                this.$addNewDotAsset.emit();
            }
        }
    ]);

    readonly $treeExpanded = this.#store.isTreeExpanded;
}
