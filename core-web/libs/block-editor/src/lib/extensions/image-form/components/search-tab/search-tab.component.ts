import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    OnChanges,
    SimpleChanges,
    ViewChild,
    ElementRef
} from '@angular/core';
import { FormGroup, FormGroupDirective } from '@angular/forms';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { sanitizeUrl } from '@dotcms/block-editor';
import { squarePlus } from '../../../../shared/components/suggestions/suggestion-icons';

@Component({
    selector: 'dot-search-tab',
    templateUrl: './search-tab.component.html',
    styleUrls: ['./search-tab.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchTabComponent implements OnInit, OnChanges {
    @Input() contentlets: DotCMSContentlet[] = [];
    @Input() formControl: [] = [];
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();
    @ViewChild('inputSearch') input: ElementRef;

    public form: FormGroup;
    public icon = sanitizeUrl(squarePlus);

    constructor(private parentForm: FormGroupDirective) {}

    ngOnInit(): void {
        this.form = this.parentForm.control;
    }

    ngOnChanges(changes: SimpleChanges) {
        const { contentlets } = changes;
        if (contentlets.currentValue) {
            requestAnimationFrame(() => {
                this.input?.nativeElement.focus();
            });
        }
    }
}
