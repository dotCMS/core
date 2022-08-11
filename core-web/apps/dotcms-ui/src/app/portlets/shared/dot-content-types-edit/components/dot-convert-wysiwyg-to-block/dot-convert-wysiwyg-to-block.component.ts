import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    styleUrls: ['./dot-convert-wysiwyg-to-block.component.scss']
})
export class DotConvertWysiwygToBlockComponent implements OnInit {
    @Input() currentFieldType;

    @Output() convert = new EventEmitter<MouseEvent>();

    accept = false;

    messages: Record<string, string>;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.messages = {
            infoHeader: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.info.header'
            ),
            infoContent: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.info.content'
            ),
            header: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.header'
            ),
            content: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.content'
            ),
            iunderstand: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.iunderstand'
            ),
            button: this.dotMessageService.get(
                'contenttypes.field.properties.wysiwyg.convert.button'
            )
        };
    }
}
