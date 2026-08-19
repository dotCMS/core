import {
    ChangeDetectionStrategy,
    Component,
    ViewEncapsulation,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';

import { SplitButtonModule } from 'primeng/splitbutton';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ActionHeaderDeleteOptions } from '../../../../shared/models/action-header/action-header-delete-options.model';
import { ActionHeaderOptions } from '../../../../shared/models/action-header/action-header-options.model';
import { ButtonAction } from '../../../../shared/models/action-header/button-action.model';
import { ButtonModel } from '../../../../shared/models/action-header/button.model';
import { DotActionButtonComponent } from '../../_common/dot-action-button/dot-action-button.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-action-header',
    templateUrl: 'action-header.component.html',
    imports: [SplitButtonModule, DotActionButtonComponent, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ActionHeaderComponent {
    private dotMessageService = inject(DotMessageService);
    private dotDialogService = inject(DotAlertConfirmService);

    selectedItems = input<unknown[]>([]);
    options = input<ActionHeaderOptions>();

    public dynamicOverflow = signal('visible');

    private wrappedCommands = new WeakSet();

    constructor() {
        effect(() => {
            const items = this.selectedItems();
            untracked(() => {
                this.hideDynamicOverflow(items);
            });
        });

        effect(() => {
            // Held in a local: TypeScript drops a property narrowing inside a callback, and
            // `untracked` takes one.
            const secondary = this.options()?.secondary;
            if (secondary) {
                untracked(() => {
                    this.setCommandWrapper(secondary);
                });
            }
        });
    }

    /**
     * Trigger button primary actions if is defined
     *
     * @memberof ActionHeaderComponent
     */
    handlePrimaryAction(): void {
        const opts = this.options();
        if (opts?.primary?.command) {
            opts.primary.command();
        }
    }

    private setCommandWrapper(options: ButtonAction[]): void {
        options.forEach((actionButton) => {
            actionButton.model
                .filter(
                    (model): model is ButtonModel & { deleteOptions: ActionHeaderDeleteOptions } =>
                        !!model.deleteOptions
                )
                .forEach((model) => {
                    if (
                        typeof model.command === 'function' &&
                        !this.wrappedCommands.has(model.command)
                    ) {
                        const callback = model.command;
                        model.command = ($event) => {
                            const originalEvent = $event;

                            this.dotDialogService.confirm({
                                accept: () => {
                                    callback(originalEvent);
                                },
                                header: model.deleteOptions.confirmHeader,
                                message: model.deleteOptions.confirmMessage,
                                footerLabel: {
                                    accept: this.dotMessageService.get(
                                        'contenttypes.action.delete'
                                    ),
                                    reject: this.dotMessageService.get('contenttypes.action.cancel')
                                }
                            });
                        };
                        this.wrappedCommands.add(model.command);
                    }
                });
        });
    }

    private hideDynamicOverflow(items: unknown[]): void {
        this.dynamicOverflow.set('');
        if (items.length) {
            setTimeout(() => {
                this.dynamicOverflow.set('visible');
            }, 300);
        }
    }
}
