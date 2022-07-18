import {
    Component,
    OnInit,
    ViewChild,
    ElementRef,
    EventEmitter,
    Output,
    Input
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';

// Components
import {
    SuggestionsCommandProps,
    SuggestionsComponent
} from '../suggestions/suggestions.component';

// Models
import { isValidURL } from '../../../utils/bubble-menu.utils';

export interface blockLinkMenuForm {
    link: string;
    blank: boolean;
}

@Component({
    selector: 'dotcms-bubble-menu-link-form',
    templateUrl: './bubble-menu-link-form.component.html',
    styleUrls: ['./bubble-menu-link-form.component.scss']
})
export class BubbleMenuLinkFormComponent implements OnInit {
    @ViewChild('input') input: ElementRef;
    @ViewChild('suggestions', { static: false }) suggestionsComponent: SuggestionsComponent;

    @Output() hide: EventEmitter<boolean> = new EventEmitter(false);
    @Output() removeLink: EventEmitter<boolean> = new EventEmitter(false);
    @Output() submitForm: EventEmitter<{ link: string; blank: boolean }> = new EventEmitter();

    @Input() showSuggestions = false;
    @Input() initialValues: blockLinkMenuForm = {
        link: '',
        blank: true
    };

    private minChars = 3;

    loading = false;
    form: FormGroup;
    options = [
        { name: 'New Window', blank: true },
        { name: 'Same Window', blank: false }
    ];

    // Getters
    get noResultsTitle() {
        return `No resutls for <strong>${this.newLink}</strong>`;
    }

    get currentLink() {
        return this.initialValues.link;
    }

    get newLink() {
        return this.form.get('link').value;
    }

    constructor(private fb: FormBuilder) {
        /* */
    }

    ngOnInit() {
        this.form = this.fb.group({ ...this.initialValues });

        this.form
            .get('link')
            .valueChanges.pipe(debounceTime(500))
            .subscribe((link) => {
                // If it's a valid url, do not search
                if (link.length < this.minChars || isValidURL(link)) {
                    return;
                }
                this.suggestionsComponent?.searchContentlets({ link });
            });
    }

    /**
     *
     *
     * @memberof BubbleMenuLinkFormComponent
     */
    setLoading() {
        const shouldShow = !(this.newLink.length < this.minChars || isValidURL(this.newLink));
        this.showSuggestions = shouldShow;
        this.loading = shouldShow;
    }

    /**
     *
     *
     * @param {blockLinkMenuForm} { link, blank }
     * @memberof BubbleMenuLinkFormComponent
     */
    setFormValue({ link, blank }: blockLinkMenuForm) {
        this.form.setValue({ link, blank }, { emitEvent: false });
    }

    /**
     * Set Focus Search Input
     *
     * @memberof BubbleMenuLinkFormComponent
     */
    focusInput() {
        this.input.nativeElement.focus();
    }

    /**
     * Listen Key events on search input
     *
     * @param {KeyboardEvent} e
     * @return {*}
     * @memberof BubbleMenuLinkFormComponent
     */
    onKeyDownEvent(e: KeyboardEvent) {
        if (!this.showSuggestions) {
            return true;
        }
        switch (e.key) {
            case 'Escape':
                this.hide.emit(true);
                break;
            case 'Enter':
                this.suggestionsComponent?.execCommand();
                // prevent submit form
                return false;
            case 'ArrowUp':
                this.suggestionsComponent?.updateSelection(e);
                break;
            case 'ArrowDown':
                this.suggestionsComponent?.updateSelection(e);
                break;
        }
    }

    /**
     * Reset form value to initials
     *
     * @memberof BubbleMenuLinkFormComponent
     */
    resetForm() {
        this.showSuggestions = false;
        this.setFormValue({ ...this.initialValues });
    }

    /**
     * Set url on selection
     *
     * @param {SuggestionsCommandProps} { payload: { url } }
     * @memberof BubbleMenuLinkFormComponent
     */
    onSelection({ payload: { url } }: SuggestionsCommandProps) {
        this.setFormValue({ ...this.form.value, link: url });
        this.submitForm.emit(this.form.value);
    }
}
