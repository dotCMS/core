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
import { SuggestionsCommandProps, SuggestionsComponent } from '@dotcms/block-editor';

// Models
import { isValidURL } from '../bubble-menu/utils';

export interface NodeProps {
    link: string;
    blank: boolean;
}

@Component({
    selector: 'dot-bubble-link-form',
    templateUrl: './bubble-link-form.component.html',
    styleUrls: ['./bubble-link-form.component.scss']
})
export class BubbleLinkFormComponent implements OnInit {
    @ViewChild('input') input: ElementRef;
    @ViewChild('suggestions', { static: false }) suggestionsComponent: SuggestionsComponent;

    @Output() hide: EventEmitter<boolean> = new EventEmitter(false);
    @Output() removeLink: EventEmitter<boolean> = new EventEmitter(false);
    @Output() isSuggestionOpen: EventEmitter<boolean> = new EventEmitter(false);
    @Output() setNodeProps: EventEmitter<NodeProps> = new EventEmitter();

    @Input() showSuggestions = false;
    @Input() initialValues: NodeProps = {
        link: '',
        blank: true
    };

    private minChars = 3;
    loading = false;
    form: FormGroup;

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
        this.form
            .get('blank')
            .valueChanges.subscribe((blank) =>
                this.setNodeProps.emit({ link: this.currentLink, blank })
            );
    }

    /**
     * Submit node props and close link form.
     *
     * @memberof BubbleLinkFormComponent
     */
    submitForm() {
        this.setNodeProps.emit(this.form.value);
        this.hide.emit(true);
    }

    /**
     *
     *
     * @memberof BubbleLinkFormComponent
     */
    setLoading() {
        const shouldShow = this.newLink.length >= this.minChars && !isValidURL(this.newLink);
        this.showSuggestions = shouldShow;
        this.loading = shouldShow;
        if (shouldShow) {
            // Wait for the suggestions to appear in the DOM
            requestAnimationFrame(() => this.isSuggestionOpen.emit(true));
        }
    }

    /**
     * Set Form values without emit `valueChanges` event.
     *
     * @param {NodeProps} { link, blank }
     * @memberof BubbleLinkFormComponent
     */
    setFormValue({ link, blank }: NodeProps) {
        this.form.setValue({ link, blank }, { emitEvent: false });
    }

    /**
     * Set Focus Search Input
     *
     * @memberof BubbleLinkFormComponent
     */
    focusInput() {
        this.input.nativeElement.focus();
    }

    /**
     * Listen Key events on search input
     *
     * @param {KeyboardEvent} e
     * @return {*}
     * @memberof BubbleLinkFormComponent
     */
    onKeyDownEvent(e: KeyboardEvent) {
        const items = this.suggestionsComponent?.items;

        if (e.key === 'Escape') {
            this.hide.emit(true);

            return true;
        }

        if (!this.showSuggestions || !items?.length) {
            return true;
        }

        switch (e.key) {
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
     * @memberof BubbleLinkFormComponent
     */
    resetForm() {
        this.showSuggestions = false;
        this.setFormValue({ ...this.initialValues });
    }

    /**
     * Set url on selection
     *
     * @param {SuggestionsCommandProps} { payload: { url } }
     * @memberof BubbleLinkFormComponent
     */
    onSelection({ payload: { url } }: SuggestionsCommandProps) {
        this.setFormValue({ ...this.form.value, link: url });
        this.submitForm();
    }
}
