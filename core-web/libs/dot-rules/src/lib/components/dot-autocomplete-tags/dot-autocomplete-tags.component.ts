import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { AutoCompleteSelectEvent, AutoCompleteUnselectEvent } from 'primeng/autocomplete';

@Component({
    selector: 'dot-autocomplete-tags',
    templateUrl: './dot-autocomplete-tags.component.html',
    styleUrls: ['./dot-autocomplete-tags.component.css']
})
export class DotAutocompleteTagsComponent implements OnInit {
    @Input() inputId: string;
    @Input() value: any[];
    @Input() options: any[];
    @Input() placeholder: string;

    @Output() onChange = new EventEmitter<any>();

    filteredOptions: SelectItem[] = [];

    ngOnInit() {
        this.filteredOptions = this.options;
        this.value = this.value === null ? [] : this.value;
    }

    filterOptions(event: any) {
        const currentValue = this.value.join();
        this.filteredOptions = this.options.filter(
            (option) =>
                option.label.indexOf(event.query) >= 0 && currentValue.indexOf(option.label) < 0
        );
    }

    checkForTag(event: any) {
        if (event.keyCode === 13 && event.currentTarget.value.trim()) {
            this.value.push(event.currentTarget.value);
            this.onChange.emit(this.value);
            event.currentTarget.value = null;
        }
    }
    addItem(event: AutoCompleteSelectEvent) {
        const { value } = event;
        this.value.splice(-1, 1);
        this.value.push(value);
        this.onChange.emit(this.value);
    }

    removeItem(_event: AutoCompleteUnselectEvent) {
        this.onChange.emit(this.value);
    }
}
