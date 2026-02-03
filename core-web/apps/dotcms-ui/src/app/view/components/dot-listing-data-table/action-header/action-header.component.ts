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

import { ActionHeaderOptions } from '../../../../shared/models/action-header/action-header-options.model';
import { ButtonAction } from '../../../../shared/models/action-header/button-action.model';
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
                this.hideDinamycOverflow(items);
            });
        });

        effect(() => {
            const opts = this.options();
            if (opts?.secondary) {
                untracked(() => {
                    this.setCommandWrapper(opts.secondary);
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
                .filter((model) => model.deleteOptions)
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
                                header: model.deleteOptions?.confirmHeader,
                                message: model.deleteOptions?.confirmMessage,
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

    private hideDinamycOverflow(items: unknown[]): void {
        this.dynamicOverflow.set('');
        if (items.length) {
            setTimeout(() => {
                this.dynamicOverflow.set('visible');
            }, 300);
        }
    }
}
