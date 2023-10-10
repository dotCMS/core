import { Observable } from 'rxjs';

import { Component, EventEmitter, Output, OnInit } from '@angular/core';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

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
    styleUrls: ['./ai-content-actions.component.scss']
})
export class AIContentActionsComponent implements OnInit {
    @Output() actionEmitter = new EventEmitter<ACTIONS>();

    actionOptions!: ActionOption[];
    tooltipContent = 'Describe the size, color palette, style, mood, etc.';

    constructor(private aiContentService: AiContentService) {}

    ngOnInit() {
        this.actionOptions = [
            {
                label: 'Accept',
                icon: 'pi pi-check',
                callback: () => this.emitAction(ACTIONS.ACCEPT),
                selectedOption: true
            },
            {
                label: 'Regenerate',
                icon: 'pi pi-sync',
                callback: () => this.emitAction(ACTIONS.REGENERATE),
                selectedOption: false
            },
            {
                label: 'Delete',
                icon: 'pi pi-trash',
                callback: () => this.emitAction(ACTIONS.DELETE),
                selectedOption: false
            }
        ];
    }

    private emitAction(action: ACTIONS) {
        this.actionEmitter.emit(action);
    }

    getLatestContent(): string {
        return this.aiContentService.getLatestContent();
    }

    getNewContent(contentType: string): Observable<string> {
        return this.aiContentService.getNewContent(contentType);
    }
}
