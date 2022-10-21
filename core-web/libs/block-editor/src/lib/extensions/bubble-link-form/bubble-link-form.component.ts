import {
    Component,
    OnInit,
    ViewChild,
    ElementRef,
    EventEmitter,
    Output,
    Input,
    HostListener
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { debounceTime, take } from 'rxjs/operators';

// Components
import { SuggestionsCommandProps } from '@dotcms/block-editor';

// Models
import { isValidURL } from '../bubble-menu/utils';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { SuggestionsService, DotLanguageService, Languages } from '../../shared';
import { SuggestionPageComponent } from './components/suggestion-page/suggestion-page.component';

export interface NodeProps {
    link: string;
    blank?: boolean;
}

@Component({
    selector: 'dot-bubble-link-form',
    templateUrl: './bubble-link-form.component.html',
    styleUrls: ['./bubble-link-form.component.scss']
})
export class BubbleLinkFormComponent implements OnInit {
    @ViewChild('input') input: ElementRef;
    @ViewChild('suggestions', { static: false }) suggestionsComponent: SuggestionPageComponent;

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
    private dotLangs: Languages;

    loading = false;
    form: FormGroup;
    items = [];

    /**
     * Avoid loosing the `focus` target
     *
     * @param {MouseEvent} e
     * @memberof SuggestionListComponent
     */
    @HostListener('mousedown', ['$event'])
    onMouseDownHandler(e: MouseEvent) {
        const { target } = e;

        if (target === this.input.nativeElement) {
            return;
        }

        e.preventDefault();
    }

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

    constructor(
        private fb: FormBuilder,
        private suggestionsService: SuggestionsService,
        private dotLanguageService: DotLanguageService
    ) {}

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

                this.searchContentlets({ link });
            });
        this.form
            .get('blank')
            .valueChanges.subscribe((blank) =>
                this.setNodeProps.emit({ link: this.currentLink, blank })
            );

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));
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
        this.items = shouldShow ? this.items : [];
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
    setFormValue({ link = '', blank = true }: NodeProps) {
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
        e.stopImmediatePropagation();

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

    /**
     * Search contentlets filtered by url
     *
     * @private
     * @param {*} { link = '' }
     * @memberof BubbleMenuLinkFormComponent
     */
    searchContentlets({ link = '' }) {
        this.loading = true;
        this.suggestionsService
            .getContentletsUrlMap({ filter: link })
            .pipe(take(1))
            .subscribe((contentlets: DotCMSContentlet[]) => {
                this.items = contentlets.map((contentlet) => {
                    const { languageId } = contentlet;
                    contentlet.language = this.getContentletLanguage(languageId);

                    return {
                        label: contentlet.title,
                        icon: 'contentlet/image',
                        data: {
                            contentlet: contentlet
                        },
                        command: () => {
                            this.onSelection({
                                payload: contentlet,
                                type: {
                                    name: 'dotContent'
                                }
                            });
                        }
                    };
                });
                this.loading = false;
            });
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
