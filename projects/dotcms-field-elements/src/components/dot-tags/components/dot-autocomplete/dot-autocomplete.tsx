import { Component, Prop, Event, EventEmitter, Element, Watch } from '@stencil/core';
import autoComplete from '@tarekraafat/autocomplete.js/dist/js/autoComplete';

@Component({
    tag: 'dot-autocomplete',
    styleUrl: 'dot-autocomplete.scss'
})
export class DotAutocompleteComponent {
    @Element() el: HTMLElement;

    /** (optional) Disables field's interaction */
    @Prop({ reflectToAttr: true }) disabled = false;

    /** (optional) text to show when no value is set */
    @Prop({ reflectToAttr: true }) placeholder = '';

    /** (optional)  Min characters to start search in the autocomplete input */
    @Prop({ reflectToAttr: true }) threshold = 0;

    /** (optional)  Max results to show after a autocomplete search */
    @Prop({ reflectToAttr: true }) maxResults = 0;

    /** (optional) Duraction in ms to start search into the autocomplete */
    @Prop({ reflectToAttr: true }) debounce = 300;

    /** Function to get the data to use for the autocomplete search */
    @Prop({ reflectToAttr: true }) data: () => Promise<string[]> = null;

    @Event() select: EventEmitter<string>;
    @Event() lostFocus: EventEmitter<FocusEvent>;

    private readonly id = `autoComplete${new Date().getTime()}`;

    private keyEvent = {
        Enter: this.emitselect.bind(this),
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
                id={this.id}
                placeholder={this.placeholder || null}
                disabled={this.disabled || null}
                onBlur={(event: FocusEvent) => this.handleBlur(event)}
                onKeyDown={(event: KeyboardEvent) => this.handleKeyDown(event)}
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
            this.keyEvent[event.key](value);
        }
    }

    private handleBlur(event: FocusEvent): void {
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
    }

    private cleanOptions(): void {
        this.getResultList().innerHTML = '';
    }

    private emitselect(select: string): void {
        this.clean();
        this.select.emit(select);
    }

    private getInputElement(): HTMLInputElement {
        return this.el.querySelector(`#${this.id}`);
    }

    private initAutocomplete(): void {
        this.clearList();
        // tslint:disable-next-line:no-unused-expression
        new autoComplete({
            data: {
                src: async () => this.getData()
            },
            sort: (a, b) => {
                if (a.match < b.match) {
                    return -1;
                }
                if (a.match > b.match) {
                    return 1;
                }
                return 0;
            },
            placeHolder: this.placeholder,
            selector: `#${this.id}`,
            threshold: this.threshold,
            searchEngine: 'strict',
            highlight: true,
            maxResults: this.maxResults,
            debounce: this.debounce,
            resultsList: {
                container: () => this.getResultListId(),
                destination: this.getInputElement(),
                position: 'afterend'
            },
            resultItem: (data) => {
                return `${data.match}`;
            },
            onSelection: (feedback) => {
                this.focusOnInput();
                this.emitselect(feedback.selection.value);
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
        return `${this.id}_results_list`;
    }

    private async getData(): Promise<string[]> {
        const autocomplete = this.getInputElement();
        autocomplete.setAttribute('placeholder', 'Loading...');
        const data = !!this.data ? await this.data() : [];
        autocomplete.setAttribute('placeholder', this.placeholder || '');
        return data;
    }
}
