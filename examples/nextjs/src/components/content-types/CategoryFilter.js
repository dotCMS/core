import Link from "next/link";

export default function CategoryFilter({ widgetCodeJSON }) {
    const categories = widgetCodeJSON.categories;

    if (!categories) {
        console.warn("No categories found in CategoryFilter");

        return null;
    }

    return (
        <div className="bg-white rounded-lg shadow-sm p-4">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
                Categories
            </h3>
            <div className="space-y-2">
                {categories?.map((category) => (
                    <CategoryItem
                        key={category.url}
                        title={category.title}
                        url={category.url}
                    />
                ))}
            </div>
        </div>
    );
}

function CategoryItem({ title, url }) {
    return (
        <div className="border-b border-gray-100 pb-2">
            <div className="flex items-center justify-between">
                <Link
                    href={url}
                    className="text-blue-600 hover:text-blue-800 transition-colors duration-200 py-1 block"
                >
                    {title}
                </Link>
            </div>
        </div>
    );
}
