import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    inject,
    OnInit,
    Output
} from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

interface ActionOption {
    label: string;
    icon: string;
    selectedOption: boolean;
    callback: () => void;
}

export enum ACTIONS {
    ACCEPT = 'ACCEPT',
    DELETE = 'DELETE',
    REGENERATE = 'REGENERATE'
}

@Component({
    selector: 'dot-ai-content-actions',
    templateUrl: './ai-content-actions.component.html',
    styleUrls: ['./ai-content-actions.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AIContentActionsComponent implements OnInit {
    @Output() actionEmitter = new EventEmitter<ACTIONS>();
    actionOptions!: ActionOption[];
    tooltipContent = 'Describe the size, color palette, style, mood, etc.';
    private dotMessageService: DotMessageService = inject(DotMessageService);

    ngOnInit() {
        this.actionOptions = [
            {
                label: this.dotMessageService.get('block-editor.common.accept'),
                icon: 'pi pi-check',
                callback: () => this.emitAction(ACTIONS.ACCEPT),
                selectedOption: true
            },
            {
                label: this.dotMessageService.get('block-editor.common.regenerate'),
                icon: 'pi pi-sync',
                callback: () => this.emitAction(ACTIONS.REGENERATE),
                selectedOption: false
            },
            {
                label: this.dotMessageService.get('block-editor.common.delete'),
                icon: 'pi pi-trash',
                callback: () => this.emitAction(ACTIONS.DELETE),
                selectedOption: false
            }
        ];
    }

    private emitAction(action: ACTIONS) {
        this.actionEmitter.emit(action);
    }
}
