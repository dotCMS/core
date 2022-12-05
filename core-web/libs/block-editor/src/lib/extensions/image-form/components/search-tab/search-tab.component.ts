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
import { FormGroup, FormBuilder } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-search-tab',
    templateUrl: './search-tab.component.html',
    styleUrls: ['./search-tab.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchTabComponent implements OnInit, OnChanges {
    @Input() contentlets: DotCMSContentlet[] = [];
    @Output() search: EventEmitter<string> = new EventEmitter();
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();

    @ViewChild('inputSearch') input: ElementRef;

    public form: FormGroup;

    constructor(private fb: FormBuilder) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });

        this.form.valueChanges.pipe(debounceTime(500)).subscribe(({ search }) => {
            this.search.emit(search);
        });
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
