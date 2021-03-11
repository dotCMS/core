import {
    Component,
    h,
    Host,
    Prop,
    Event,
    EventEmitter,
    Method,
    Watch,
    Element
} from '@stencil/core';
import {
    DotCardContentletItem,
    DotCardContentletEvent
} from '../../models/dot-card-contentlet.model';
import { DotContentletItem } from '../../models/dot-contentlet-item.model';

const getValueAsArray = (value: string): string[] => {
    return value && typeof value === 'string' ? value.split(',') : [];
};

const getSelecttion = (items: DotCardContentletItem[], value: string): DotContentletItem[] => {
    if (items && items.length && value && typeof value === 'string') {
        return items
            .filter(({ data: { inode } }: DotCardContentletItem) =>
                value.split(',').includes(inode)
            )
            .map(({ data }: DotCardContentletItem) => data);
    }

    return [];
};

@Component({
    tag: 'dot-card-view',
    styleUrl: 'dot-card-view.scss',
    shadow: true
})
export class DotCardView {
    @Element() el: HTMLElement;
    @Prop() items: DotCardContentletItem[] = [];
    @Prop({
        reflect: true,
        mutable: true
    })
    value: string;

    @Event() selected: EventEmitter;
    @Event() cardClick: EventEmitter;

    private selection: DotContentletItem[] = [];

    private cards: NodeListOf<HTMLDotCardContentletElement>;

    private lastChecked;

    @Watch('items')
    watchItems(newValue: DotCardContentletItem[]) {
        this.selection = getSelecttion(newValue, this.value);
    }

    @Watch('value')
    watchValue(newValue: string) {
        this.selection = getSelecttion(this.items, newValue);
    }

    @Method()
    async getValue(): Promise<DotContentletItem[]> {
        return this.selection;
    }

    @Method()
    async clearValue(): Promise<void> {
        this.value = '';
        const cards = this.getCards();

        cards.forEach((card: HTMLDotCardContentletElement) => {
            card.checked = false;
        });
    }

    componentDidLoad() {
        this.selection = getSelecttion(this.items, this.value);
        this.cards = this.getCards();
    }

    private clearMenu() {
        this.cards.forEach((card: HTMLDotCardContentletElement) => {
            card.hideMenu();
        });
    }

    render() {
        const value = getValueAsArray(this.value);

        return (
            <Host>
                {this.items.map((item: DotCardContentletItem) => (
                    <dot-card-contentlet
                        onContextMenu={async (e: MouseEvent) => {
                            e.preventDefault();
                            const target = e.target as HTMLDotCardContentletElement;
                            this.clearMenu();
                            target.showMenu(e.x, e.y);
                        }}
                        onContextMenuClick={() => {
                            this.clearMenu();
                        }}
                        onClick={() => {
                            this.clearMenu();
                            this.cardClick.emit(item.data);
                        }}
                        key={item.data.inode}
                        checked={value.includes(item.data.inode)}
                        onCheckboxChange={({
                            detail: { originalTarget, shiftKey }
                        }: CustomEvent<DotCardContentletEvent>) => {
                            let inBetween = false;

                            if (shiftKey && originalTarget.checked) {
                                this.cards.forEach((card: HTMLDotCardContentletElement) => {
                                    if (card === originalTarget || card === this.lastChecked) {
                                        inBetween = !inBetween;
                                    }
                                    if (inBetween) {
                                        card.checked = true;
                                        this.setValue(originalTarget, card.item.data);
                                    }
                                });
                            }

                            this.lastChecked = originalTarget;

                            this.setValue(originalTarget, item.data);
                        }}
                        item={item}
                    />
                ))}
            </Host>
        );
    }

    private setValue(originalTarget: HTMLDotCardContentletElement, data: DotContentletItem): void {
        if (originalTarget.checked) {
            this.selection.push(data);
        } else {
            this.selection = this.selection.filter(
                ({ identifier }: DotContentletItem) => identifier !== data.identifier
            );
        }

        this.value = this.selection.map(({ inode }: DotContentletItem) => inode).join(',');
        this.selected.emit(this.selection);
    }

    private getCards(): NodeListOf<HTMLDotCardContentletElement> {
        return this.el.shadowRoot.querySelectorAll('dot-card-contentlet');
    }
}
