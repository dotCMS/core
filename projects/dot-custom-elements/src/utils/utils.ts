export function generateId(): number {
    return Date.now().valueOf();
}

export interface DotOption {
    label: string;
    value: string;
}

export function getItemsFromString(rawString: string): DotOption[] {
    const items = rawString
        .split(' ')
        .filter((item) => item.length > 0)
        .map((item) => {
            const splittedItem = item.split('|');
            return { label: splittedItem[0], value: splittedItem[1] };
        });
    return items;
}
