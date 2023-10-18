import {
    Component,
    Prop,
    State,
    Listen,
    Element,
    Host,
    h,
    Event,
    EventEmitter
} from '@stencil/core';
import { MaterialIconClasses } from './material-icon-classes';
import '@material/mwc-icon';

@Component({
    tag: 'dot-material-icon-picker',
    styleUrl: 'dot-material-icon-picker.scss'
})
export class DotMaterialIcon {
    @Element() element: HTMLElement;

    @State() showSuggestions: boolean;
    @State() suggestionArr: string[] = [];
    @State() selectedSuggestionIndex: number;

    /** Value for input placeholder */
    @Prop({ reflect: true }) placeholder: string = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** Value set from the dropdown option */
    @Prop({ mutable: true, reflect: true })
    value = '';

    /** Size value set for font-size */
    @Prop({ mutable: true, reflect: true }) size: string = null;

    /** Show/Hide color picker */
    @Prop({ mutable: true, reflect: true }) showColor: string = null;

    /** Color value set from the input */
    @Prop({ mutable: true, reflect: true })
    colorValue = '#000';

    /** Label set for the input color */
    @Prop({ mutable: true, reflect: true })
    colorLabel = 'Color';

    /** Values that the auto-complete textbox should search for */
    @Prop({ reflect: true }) suggestionlist: string[] = MaterialIconClasses;

    @Event()
    dotValueChange: EventEmitter<{ name: string; value: string; colorValue: string }>;

    @Listen('click', { target: 'window' })
    handleWindowClick(e: Event) {
        if (!this.element.contains(e.target as HTMLElement)) {
            this.showSuggestions = false;
            this.selectedSuggestionIndex = undefined;
        }
    }

    componentWillLoad() {
        this.emitValues();
    }

    findMatch = (searchTerm: string): string[] => {
        return this.suggestionlist.filter(
            (term) => term.includes(searchTerm) && term !== searchTerm
        );
    };

    onInput = (e: Event) => {
        this.value = (e.target as any).value;
        this.suggestionArr = this.findMatch(this.value);
        this.showSuggestions = true;
    };

    onChangeColor = (e: Event) => {
        this.colorValue = (e.target as any).value;
        this.emitValues();
    };

    onFocus = (resetSearch: boolean) => {
        this.showSuggestions = true;
        this.selectedSuggestionIndex = undefined;
        // On first focus, this.value is null
        const match = resetSearch ? '' : this.value || '';
        this.suggestionArr = this.findMatch(match);

        if (resetSearch) {
            const input: HTMLInputElement = this.element.querySelector('.dot-material-icon__input');
            input.focus();
        }
    };

    onKeyDown = (e: KeyboardEvent) => {
        switch (e.key) {
            case 'ArrowUp':
                if (this.suggestionArr.length > 0) {
                    this.selectedSuggestionIndex =
                        this.selectedSuggestionIndex === undefined ||
                        this.selectedSuggestionIndex === 0
                            ? this.suggestionArr.length - 1
                            : this.selectedSuggestionIndex - 1;

                    this.scrollIntoSelectedOption(this.selectedSuggestionIndex);
                }
                break;
            case 'ArrowDown':
                if (this.suggestionArr.length > 0) {
                    this.selectedSuggestionIndex =
                        this.selectedSuggestionIndex === undefined ||
                        this.selectedSuggestionIndex === this.suggestionArr.length - 1
                            ? 0
                            : this.selectedSuggestionIndex + 1;

                    this.scrollIntoSelectedOption(this.selectedSuggestionIndex);
                }
                break;
            default:
                break;
        }
    };

    onKeyPress = (e: KeyboardEvent) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (this.selectedSuggestionIndex !== undefined) {
                this.onSelect(this.suggestionArr[this.selectedSuggestionIndex]);
            }
        }
    };

    onSelect = (selection: string) => {
        this.value = selection;
        this.selectedSuggestionIndex = undefined;
        this.showSuggestions = false;
        this.emitValues();
    };

    onTextChange = (e: Event) => {
        const value = (e.target as HTMLInputElement).value;
        if (this.suggestionlist.includes(value)) {
            this.onSelect(value);
        }
    };

    getSuggestionElement = (suggestion: string): JSX.Element => {
        const isSelected =
            this.selectedSuggestionIndex !== undefined &&
            suggestion === this.suggestionArr[this.selectedSuggestionIndex];
        return (
            <li
                role="option"
                class={
                    'dot-material-icon__option ' +
                    (isSelected ? 'dot-material-icon__option-selected' : '')
                }
                onClick={() => this.onSelect(suggestion)}>
                <label id={suggestion + '_Id'}>
                    <mwc-icon aria-labelledby={suggestion + '_Id'}>{suggestion}</mwc-icon>
                    {suggestion}
                </label>
            </li>
        );
    };

    emitValues = () => {
        this.dotValueChange.emit({
            colorValue: this.colorValue,
            name: this.name,
            value: this.value
        });
    };

    render() {
        // reason: https://github.com/dotCMS/core/issues/22861
        const LABEL_IMPORTANT_ICON = 'label_important';

        return (
            <Host
                class={{
                    'is-open': this.showSuggestions
                }}
                style={{
                    'font-size': this.size
                }}>
                <div class="dot-material-icon__select-container">
                    <div class="dot-material-icon__select-input">
                        <mwc-icon
                            class="dot-material-icon__preview"
                            style={{ color: this.colorValue }}>
                            {this.value === LABEL_IMPORTANT_ICON ? '' : this.value}
                        </mwc-icon>
                        <input
                            class="dot-material-icon__input"
                            type="text"
                            role="searchbox"
                            placeholder={this.placeholder}
                            value={this.value}
                            onChange={(e) => this.onTextChange(e)}
                            onInput={(e) => this.onInput(e)}
                            onClick={() => this.onFocus(false)}
                            onKeyDown={(e) => this.onKeyDown(e)}
                            onKeyPress={(e) => this.onKeyPress(e)}
                        />
                        <button
                            class="dot-material-icon__button"
                            role="button"
                            type="button"
                            onClick={(e: MouseEvent) => {
                                e.preventDefault();
                                this.onFocus(true);
                            }}>
                            <mwc-icon>expand_more</mwc-icon>
                        </button>
                    </div>
                    <div class="dot-material-icon__select-options-container">
                        <ul
                            class="dot-material-icon__list"
                            role="listbox"
                            hidden={!this.showSuggestions}>
                            {this.suggestionArr.map((suggestion) =>
                                this.getSuggestionElement(suggestion)
                            )}
                        </ul>
                    </div>
                </div>
                {this.getColorPicker(this.showColor)}
            </Host>
        );
    }

    private scrollIntoSelectedOption(index: number) {
        const optionsList = this.element.querySelectorAll('.dot-material-icon__option');
        optionsList[index].scrollIntoView({
            behavior: 'smooth',
            block: 'nearest'
        });
    }

    private getColorPicker(show: string): JSX.Element {
        return show === 'true' ? (
            <div>
                <label htmlFor="iconColor" class="dot-material-icon__color-label">
                    {this.colorLabel}
                </label>
                <input
                    id="iconColor"
                    class="dot-material-icon__icon-color"
                    type="color"
                    name="icon-color"
                    role="textbox"
                    onInput={(e) => this.onChangeColor(e)}
                    value={this.colorValue}
                />
            </div>
        ) : null;
    }
}
