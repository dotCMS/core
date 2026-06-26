import Image from 'next/image';

import type { DotCMSBasicContentlet } from '@dotcms/types';

import { EditButton } from '../editor/EditButton';

type StoreProduct = DotCMSBasicContentlet & {
    image: string;
    title: string;
    retailPrice?: number | string;
    salePrice?: number | string;
};

interface StoreProductListProps {
    widgetTitle?: string;
    widgetCodeJSON: {
        products?: StoreProduct[];
    };
}

export default function StoreProductList({ widgetTitle, widgetCodeJSON }: StoreProductListProps) {
    const products = widgetCodeJSON.products;

    if (!products?.length) {
        return null;
    }

    return (
        <section className="flex flex-col gap-8">
            {widgetTitle && (
                <h2 className="font-display text-h2 font-semibold text-ink">{widgetTitle}</h2>
            )}
            <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,15rem),1fr))] gap-6">
                {products.map((product) => (
                    <ProductCard key={product.identifier} product={product} />
                ))}
            </div>
        </section>
    );
}

function ProductCard({ product }: { product: StoreProduct }) {
    const { image, title, retailPrice, salePrice } = product;

    const hasDiscount =
        salePrice && retailPrice && Number(salePrice) < Number(retailPrice);

    return (
        <article className="group relative flex h-full flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
            <EditButton contentlet={product} />
            <div className="relative h-56 border-b border-line bg-bg">
                {image && (
                    <Image
                        src={image}
                        alt={title}
                        fill
                        sizes="(min-width: 1024px) 25vw, (min-width: 640px) 50vw, 100vw"
                        className="object-contain p-6 transition-transform duration-500 ease-(--ease-out-quart) group-hover:scale-105"
                    />
                )}
                {hasDiscount && (
                    <span className="absolute left-4 top-4 rounded-full bg-accent px-3 py-1 text-xs font-semibold uppercase tracking-wide text-bg">
                        {Math.round((1 - Number(salePrice) / Number(retailPrice)) * 100)}% off
                    </span>
                )}
            </div>

            <div className="flex grow flex-col p-5">
                <h3 className="line-clamp-2 font-display text-base font-semibold text-ink">
                    {title}
                </h3>
                <div className="mt-auto flex items-baseline gap-2 pt-3">
                    <span className="text-lg font-semibold text-ink">
                        ${hasDiscount ? salePrice : retailPrice}
                    </span>
                    {hasDiscount && (
                        <span className="text-sm text-muted line-through">${retailPrice}</span>
                    )}
                </div>
            </div>
        </article>
    );
}
