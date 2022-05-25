import { Component, Prop, Event, EventEmitter, Element, Watch, h } from '@stencil/core';
import autoComplete from '@tarekraafat/autocomplete.js';

interface SelectionItem {
    index: number;
    value: string;
    match: string;
}

export interface SelectionFeedback {
    event: KeyboardEvent | MouseEvent;
    query: string;
    matched: number;
    results: string[];
    selection: SelectionItem;
}

@Component({
    tag: 'dot-autocomplete',
    styleUrl: 'dot-autocomplete.scss'
})
export class DotAutocompleteComponent {
    @Element()
    el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** (optional) text to show when no value is set */
    @Prop({ reflect: true })
    placeholder = '';

    /** (optional)  Min characters to start search in the autocomplete input */
    @Prop({ reflect: true })
    threshold = 0;

    /** (optional)  Max results to show after a autocomplete search */
    @Prop({ reflect: true })
    maxResults = 0;

    /** (optional) Duraction in ms to start search into the autocomplete */
    @Prop({ reflect: true })
    debounce = 300;

    /** Function or array of string to get the data to use for the autocomplete search */
    @Prop()
    data: () => Promise<string[]> | string[] = null;

    @Event()
    selection: EventEmitter<string>;
    @Event()
    enter: EventEmitter<string>;
    @Event()
    lostFocus: EventEmitter<FocusEvent>;

    private readonly id = `autoComplete${new Date().getTime()}`;
    private enteredSuggestionList = false;

    private keyEvent = {
        ArrowDown: this.enteredList.bind(this),
        ArrowUp: this.enteredList.bind(this),
        Enter: this.emitEnter.bind(this),
        Escape: this.clean.bind(this)
    };

    componentDidLoad(): void {
        if (this.data) {
            this.initAutocomplete();
        }
    }

    render() {
        return (
            <input
                autoComplete="off"
                disabled={this.disabled || null}
                id={this.id}
                onBlur={(event: FocusEvent) => this.handleBlur(event)}
                onKeyDown={(event: KeyboardEvent) => this.handleKeyDown(event)}
                placeholder={this.placeholder || null}
            />
        );
    }

    @Watch('threshold')
    watchThreshold(): void {
        this.initAutocomplete();
    }

    @Watch('data')
    watchData(): void {
        this.initAutocomplete();
    }

    @Watch('maxResults')
    watchMaxResults(): void {
        this.initAutocomplete();
    }

    private handleKeyDown(event: KeyboardEvent): void {
        const { value } = this.getInputElement();

        if (value && this.keyEvent[event.key]) {
            event.preventDefault();
            this.keyEvent[event.key](value);
        }
    }

    private handleBlur(event: FocusEvent): void {
        event.preventDefault();
        setTimeout(() => {
            if (document.activeElement.parentElement !== this.getResultList()) {
                this.clean();
                this.lostFocus.emit(event);
            }
        }, 0);
    }

    private clean(): void {
        this.getInputElement().value = '';
        this.cleanOptions();
        this.enteredSuggestionList = false;
    }

    private cleanOptions(): void {
        this.getResultList().innerHTML = '';
    }

    private enteredList(): void {
        this.enteredSuggestionList = !this.getResultList().attributes['hidden'];
    }

    private emitEnter(select: string): void {
        if (select && !this.enteredSuggestionList) {
            this.enter.emit(select);
        }
        this.clean();
    }

    private getInputElement(): HTMLInputElement {
        return this.el.querySelector(`#${this.id}`);
    }

    private initAutocomplete(): void {
        this.clearList();

        new autoComplete({
            data: {
                src: async () => this.getData()
            },
            selector: `#${this.id}`,
            threshold: this.threshold,
            searchEngine: 'strict',
            debounce: this.debounce,
            resultsList: {
                destination: this.getInputElement.bind(this),
                id: `list_${this.id}`,
                position: 'afterend',
                noResults: false
            },
            resultItem: {
                highlight: true,
                selected: 'autoComplete_highlighted'
            },
            events: {
                input: {
                    selection: (event) => {
                        event.preventDefault();
                        this.focusOnInput();
                        this.clean();
                    }
                }
            }
        });
    }

    private clearList(): void {
        const list = this.getResultList();
        if (list) {
            list.remove();
        }
    }

    private focusOnInput(): void {
        this.getInputElement().focus();
    }

    private getResultList(): HTMLElement {
        return this.el.querySelector(`#${this.getResultListId()}`);
    }

    private getResultListId(): string {
        return `list_${this.id}`;
    }

    private async getData(): Promise<string[]> {
        const autocomplete = this.getInputElement();
        autocomplete.setAttribute('placeholder', 'Loading...');
        const data = typeof this.data === 'function' ? await this.data() : [];
        autocomplete.setAttribute('placeholder', this.placeholder || '');
        return data;
    }
}
