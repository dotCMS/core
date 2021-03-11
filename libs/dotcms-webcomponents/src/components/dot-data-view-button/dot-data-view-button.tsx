import { Component, h, Prop } from '@stencil/core';

@Component({
    tag: 'dot-data-view-button',
    styleUrl: 'dot-data-view-button.css'
})
export class DotDataViewButton {
    @Prop() value: string;

    render() {
        return (
            <dot-select-button
                value={this.value}
                options={[
                    { label: 'List', icon: 'format_list_bulleted' },
                    { label: 'Card', icon: 'grid_on' }
                ]}
            />
        );
    }
}
