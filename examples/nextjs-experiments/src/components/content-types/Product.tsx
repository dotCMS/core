import Image from 'next/image';
import Link from 'next/link';

import type { DotCMSImage } from '@/types/content';

interface ProductProps {
    image?: DotCMSImage;
    title?: string;
    salePrice?: number;
    retailPrice?: number;
    urlTitle?: string;
}

const formatPrice = (price?: number) =>
    new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(price ?? 0);

function Product({ image, title, salePrice, retailPrice, urlTitle }: ProductProps) {
    const onSale = Boolean(retailPrice && salePrice);

    return (
        <article className="group flex flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
            <div className="relative flex h-56 items-center justify-center border-b border-line bg-bg p-6">
                {image?.idPath && (
                    <Image
                        className="max-h-full w-auto object-contain transition-transform duration-500 ease-(--ease-out-quart) group-hover:scale-105"
                        src={image.idPath}
                        width={240}
                        height={240}
                        alt={title || 'Product'}
                    />
                )}
                {onSale && (
                    <span className="absolute left-4 top-4 rounded-full bg-accent px-3 py-1 text-xs font-semibold uppercase tracking-wide text-bg">
                        Sale
                    </span>
                )}
            </div>
            <div className="flex flex-1 flex-col p-5">
                <h3 className="line-clamp-1 font-display text-lg font-semibold text-ink">{title}</h3>
                <div className="mt-2 flex items-baseline gap-2">
                    <span className="text-2xl font-semibold text-ink">
                        {formatPrice(onSale ? salePrice : (retailPrice ?? salePrice))}
                    </span>
                    {onSale && (
                        <span className="text-sm text-muted line-through">
                            {formatPrice(retailPrice)}
                        </span>
                    )}
                </div>
                <Link
                    href={`/store/products/${urlTitle || '#'}`}
                    className="group/btn mt-5 inline-flex w-fit items-center gap-1.5 text-sm font-semibold text-primary transition-colors hover:text-primary-deep"
                >
                    Buy now
                    <span aria-hidden="true" className="transition-transform duration-300 ease-(--ease-out-quart) group-hover/btn:translate-x-0.5">
                        →
                    </span>
                </Link>
            </div>
        </article>
    );
}

export default Product;
