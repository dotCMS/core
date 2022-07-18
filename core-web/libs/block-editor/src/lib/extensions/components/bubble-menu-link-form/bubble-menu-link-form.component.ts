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
import { take, debounceTime } from 'rxjs/operators';

// Services
import { DotLanguageService, Languages } from '../../services/dot-language/dot-language.service';
import { SuggestionsService } from '../../services/suggestions/suggestions.service';

// Components
import {
    DotMenuItem,
    SuggestionsCommandProps,
    SuggestionsComponent
} from '../suggestions/suggestions.component';

// Models
import { DotCMSContentlet } from '@dotcms/dotcms-models';
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

    private dotLangs: Languages;
    private minChars = 3;

    options = [
        { name: 'New Window', blank: true },
        { name: 'Same Window', blank: false }
    ];

    loading = false;
    items: DotMenuItem[] = [];
    form: FormGroup;

    get noResultsTitle() {
        return `No resutls for: <strong>${this.newLink}</strong>`;
    }

    get currentLink() {
        return this.initialValues.link;
    }

    get newLink() {
        return this.form.get('link').value;
    }

    constructor(
        private suggestionService: SuggestionsService,
        private dotLanguageService: DotLanguageService,
        private fb: FormBuilder
    ) {
        /* */
    }

    ngOnInit() {
        this.form = this.fb.group({ ...this.initialValues });

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));

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
        if (!this.items.length) {
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

    resetForm() {
        this.showSuggestions = false;
        this.setFormValue({ ...this.initialValues });
    }

    /**
     * Search contentlets filtered by url
     *
     * @private
     * @param {*} { link = '' }
     * @memberof BubbleMenuLinkFormComponent
     */
    private searchContentlets({ link = '' }) {
        this.suggestionService
            .getContentletsUrlMap({ filter: link })
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
                // Active first result
                requestAnimationFrame(() => this.suggestionsComponent?.setFirstItemActive());
            });
    }

    onSelection({ payload: { url } }: SuggestionsCommandProps) {
        this.setFormValue({ ...this.form.value, link: url });
        this.submitForm.emit(this.form.value);
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
