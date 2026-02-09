import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-pages-card',
    templateUrl: './dot-pages-card.component.html',
    styleUrls: ['./dot-pages-card.component.scss'],
    imports: [
        CommonModule,
        CardModule,
        DotPagesFavoritePageEmptySkeletonComponent,
        ButtonModule,
        TooltipModule
    ]
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
