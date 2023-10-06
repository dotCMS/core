import { Observable } from 'rxjs';

import { Component, EventEmitter, Output, OnInit } from '@angular/core';

import { AiContentService } from '../../shared/services/ai-content/ai-content.service';

interface ActionOption {
    label: string;
    icon: string;
    selectedOption: boolean;
    callback: () => void;
}

@Component({
    selector: 'dot-ai-content-actions',
    templateUrl: './ai-content-actions.component.html',
    styleUrls: ['./ai-content-actions.component.scss']
})
export class AIContentActionsComponent implements OnInit {
    @Output() acceptEmitter = new EventEmitter<boolean>();
    @Output() regenerateEmitter = new EventEmitter<boolean>();
    @Output() deleteEmitter = new EventEmitter<boolean>();

    actionOptions!: ActionOption[];

    constructor(private aiContentService: AiContentService) {}

    ngOnInit() {
        this.actionOptions = [
            {
                label: 'Accept',
                icon: 'pi pi-check',
                callback: this.acceptContent.bind(this),
                selectedOption: true
            },
            {
                label: 'Regenerate',
                icon: 'pi pi-sync',
                callback: this.regenerateContent.bind(this),
                selectedOption: false
            },
            {
                label: 'Delete',
                icon: 'pi pi-trash',
                callback: this.deleteContent.bind(this),
                selectedOption: false
            }
        ];
    }

    private acceptContent() {
        this.acceptEmitter.emit(true);
    }

    private regenerateContent() {
        this.regenerateEmitter.emit(true);
    }

    private deleteContent() {
        this.deleteEmitter.emit(true);
    }

    handleClick(event): void {
        event.value.callback();
    }

    getLatestContent(contentType) {
        if (contentType === 'text') {
            return this.aiContentService.getLastContentResponse();
        } else {
            return this.aiContentService.getLastImageResponse();
        }
    }

    getNewContent(contentType: string): Observable<string> {
        const contentPrompt: string = this.aiContentService.getLastUsedPrompt();
        const imagePrompt: string = this.aiContentService.getLastImagePrompt();

        if (contentType === 'text') {
            return this.aiContentService.getIAContent(contentPrompt);
        } else {
            return this.aiContentService.getAIImage(imagePrompt);
        }
    }
}
