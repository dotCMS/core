import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-pages-card',
    templateUrl: './dot-pages-card.component.html',
    styleUrls: ['./dot-pages-card.component.scss']
})
export class DotPagesCardComponent {
    @Input() imageUri: string;
    @Input() title: string;
    @Input() url: string;
    @Input() ownerPage: boolean;
}
