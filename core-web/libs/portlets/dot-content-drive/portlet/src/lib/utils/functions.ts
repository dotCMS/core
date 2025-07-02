export function decodeFilters(filters: string): Record<string, string> {
    if (!filters) {
        return {};
    }

    const filtersArray = filters.split(';').filter((filter) => filter.trim() !== '');

    return filtersArray.reduce(
        (acc, filter) => {
            const [key, value] = filter.split(':').map((part) => part.trim());

            // We have to handle the multiselector (,) but this is enough to pave the path for now
            acc[key] = value;

            return acc;
        },
        {} as Record<string, string>
    );
}
