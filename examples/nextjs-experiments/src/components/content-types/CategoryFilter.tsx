import Link from "next/link";

interface Category {
    title: string;
    url: string;
}

interface CategoryFilterProps {
    widgetCodeJSON: {
        categories?: Category[];
    };
}

export default function CategoryFilter({ widgetCodeJSON }: CategoryFilterProps) {
    const categories = widgetCodeJSON.categories;

    if (!categories?.length) {
        return null;
    }

    return (
        <div className="rounded-2xl border border-line bg-bg p-6">
            <h3 className="mb-4 font-display text-lg font-semibold text-ink">Categories</h3>
            <ul className="flex flex-wrap gap-2">
                {categories.map((category) => (
                    <li key={category.url}>
                        <Link
                            href={category.url}
                            className="inline-flex rounded-full border border-line px-3.5 py-1.5 text-sm text-ink transition-colors hover:border-primary/40 hover:bg-primary-tint hover:text-primary"
                        >
                            {category.title}
                        </Link>
                    </li>
                ))}
            </ul>
        </div>
    );
}
