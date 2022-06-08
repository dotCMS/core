import {
    Component,
    ViewChild,
    ElementRef,
    EventEmitter,
    Output,
    Input
} from '@angular/core';

@Component({
    selector: 'dotcms-bubble-menu-link-form',
    templateUrl: './bubble-menu-link-form.component.html',
    styleUrls: ['./bubble-menu-link-form.component.scss']
})
export class BubbleMenuLinkFormComponent {

    @ViewChild('input') input: ElementRef;
    
    @Output() hideForm: EventEmitter<boolean> = new EventEmitter(false);
    @Output() removeLink: EventEmitter<boolean> = new EventEmitter(false);
    @Output() setLink: EventEmitter<string> = new EventEmitter();

    @Input() nodeLink = '';
    @Input() newLink = '';

    addLink() {
        this.setLink.emit( this.newLink );
    }

    copyLink() {
        navigator.clipboard
            .writeText(this.nodeLink)
            .then(() => this.hideForm.emit(true))
            .catch(() => alert('Could not copy link'));
    }

    focusInput() {
        this.input.nativeElement.focus();
    }
}
