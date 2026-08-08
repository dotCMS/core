import '@material/mwc-icon-button';
import { Component, Event, EventEmitter, h, Host, Prop } from '@stencil/core';
import { DotSelectButtonOption } from '../../models/dotSelectButtonOption';

@Component({
    tag: 'dot-select-button',
    styleUrl: 'dot-select-button.scss',
    shadow: true
})
export class DotSelectButton {
    @Prop({ reflect: true })
    value = '';

    @Prop({ reflect: true })
    options: DotSelectButtonOption[] = [];

    @Event() selected: EventEmitter<string>;

    render() {
        return (
            <Host>
                {this.options.map((option: DotSelectButtonOption) => {
                    const active =
                        option.label.toLocaleLowerCase() === this.value.toLocaleLowerCase();
                    return (
                        <mwc-icon-button
                            class={{
                                active
                            }}
                            icon={option.icon}
                            label={option.label}
                            aria-label={option.label}
                            disabled={option.disabled}
                            onClick={() => {
                                this.setSelected(option);
                            }}
                        />
                    );
                })}
            </Host>
        );
    }

    private setSelected(option: DotSelectButtonOption) {
        this.value = option.label;
        this.selected.emit(option.label.toLocaleLowerCase());
    }
}
