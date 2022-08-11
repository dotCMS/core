import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-convert-to-block-info',
    templateUrl: './dot-convert-to-block-info.component.html',
    styleUrls: ['./dot-convert-to-block-info.component.scss']
})
export class DotConvertToBlockInfoComponent implements OnInit {
    @Input() currentFieldType;
    @Output() action = new EventEmitter<MouseEvent>();
    @Input() currentField;

    messages: Record<string, string>;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.messages = {
            content: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.info.content'
            ),
            button: this.dotMessageService.get('contenttypes.field.properties.wysiwyg.info.button'),
            learnMore: this.dotMessageService.get('learn-more')
        };
    }
}
