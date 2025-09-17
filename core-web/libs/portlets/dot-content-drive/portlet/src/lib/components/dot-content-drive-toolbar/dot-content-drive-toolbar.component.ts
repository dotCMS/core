import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

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

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [
        ToolbarModule,
        ButtonModule,
        DotContentDriveTreeTogglerComponent,
        DotContentDriveBaseTypeSelectorComponent,
        DotContentDriveContentTypeFieldComponent,
        DotContentDriveSearchInputComponent,
        DotMessagePipe,
        MenuModule,
        DotContentDriveLanguageFieldComponent
    ],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

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
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.image')
        },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.video')
        },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.page')
        },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.content-item')
        }
    ]);

    readonly $treeExpanded = this.#store.isTreeExpanded;
}
