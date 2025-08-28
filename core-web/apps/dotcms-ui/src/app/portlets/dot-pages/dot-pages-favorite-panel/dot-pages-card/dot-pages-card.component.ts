import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-pages-card',
    templateUrl: './dot-pages-card.component.html',
    styleUrls: ['./dot-pages-card.component.scss'],
    standalone: false
})
export class DotPagesCardComponent {
    @Input() actionButtonId: string;
    @Input() imageUri: string;
    @Input() title: string;
    @Input() url: string;
    @Input() ownerPage: boolean;
    @Output() edit = new EventEmitter<boolean>();
    @Output() goTo = new EventEmitter<boolean>();
    @Output() showActionMenu = new EventEmitter<MouseEvent>();
}
