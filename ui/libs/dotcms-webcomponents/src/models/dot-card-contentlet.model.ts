import { DotContentletItem } from './dot-contentlet-item.model';
import { DotContextMenuOption } from './dot-context-menu.model';

export interface DotCardContentletItem {
    data: DotContentletItem;
    actions: DotContextMenuOption<{ index: number }>[];
}

export interface DotCardContentletEvent {
    originalTarget: HTMLDotCardContentletElement;
    shiftKey: boolean;
}
