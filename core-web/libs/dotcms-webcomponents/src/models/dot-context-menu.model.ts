export interface DotContextMenuOption<T> {
    label: string;
    action: (e: CustomEvent<T>) => void;
}
