import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';

@Component({
    selector: 'dot-reorder-menu',
    templateUrl: './dot-reorder-menu.component.html'
})
export class DotReorderMenuComponent implements OnInit {
    @Input() url: string;
    @Output() close: EventEmitter<any> = new EventEmitter();

    constructor(
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService.getMessages([
            'editpage.content.contentlet.menu.reorder.title'
        ]).subscribe();
    }

    /**
     * Handle close event from the iframe
     *
     * @memberof DotContentletWrapperComponent
     */
    onClose(): void {
        this.close.emit();
    }

}
