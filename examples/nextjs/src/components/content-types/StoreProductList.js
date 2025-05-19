import React from 'react';
import Image from 'next/image';

import { EditButton } from '../editor/EditButton';

export default function StoreProductList({ widgetTitle, widgetCodeJSON }) {
    const products = widgetCodeJSON.products;

    if (!products) {
        console.warn('No products found in StoreProductList');
        return null;
    }

    return (
        <div>
            <h2 className="text-4xl font-bold mb-4">{widgetTitle}</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {products?.map((product) => (
                    <ProductCard key={product.identifier} product={product} />
                ))}
            </div>
        </div>
    );
}

function ProductCard({ product }) {
    const { inode, title, retailPrice, salePrice } = product;

    const hasDiscount = salePrice && retailPrice && Number(salePrice) < Number(retailPrice);

    return (
        <div className="group relative" onClick={() => alert('Selected Product: ' + title)}>
            <EditButton contentlet={product} />
            <div className="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow duration-300 flex flex-col h-full">
                <div className="relative aspect-square w-full overflow-hidden bg-gray-100">
                    <Image
                        src={inode}
                        alt={title}
                        fill
                        className="object-cover object-center group-hover:scale-105 transition-transform duration-300"
                    />
                </div>

                <div className="p-4 flex flex-col flex-grow">
                    <h3 className="text-gray-900 font-medium text-sm mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors">
                        {title}
                    </h3>

                    <div className="mt-auto pt-2 flex items-center justify-between">
                        {hasDiscount ? (
                            <div>
                                <span className="text-red-600 font-semibold mr-2">
                                    ${salePrice}
                                </span>
                                <span className="text-gray-500 text-sm line-through">
                                    ${retailPrice}
                                </span>
                            </div>
                        ) : (
                            <span className="text-gray-900 font-semibold">${retailPrice}</span>
                        )}

                        {hasDiscount && (
                            <span className="bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full">
                                {Math.round((1 - Number(salePrice) / Number(retailPrice)) * 100)}%
                                OFF
                            </span>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
