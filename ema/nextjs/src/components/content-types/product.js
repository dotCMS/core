import Image from 'next/image';
import Link from 'next/link';
import { useDotcmsPageContext } from '@dotcms/react';

function Product({ image, title, salePrice, retailPrice, urlTitle }) {
    const {
        viewAs: { language }
    } = useDotcmsPageContext();

    const formatPrice = (price) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(price);
    };

    return (
        <div className="overflow-hidden bg-white rounded shadow-lg">
            <div className="p-4">
                <Image
                    className="w-full"
                    src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`}
                    width={100}
                    height={100}
                    alt="Activity Image"
                />
            </div>
            <div className="px-6 py-4 bg-slate-100">
                <div className="mb-2 text-xl font-bold">{title}</div>
                {retailPrice && salePrice ? (
                    <>
                        <div className="text-gray-500 line-through">{formatPrice(retailPrice)}</div>
                        <div className="text-3xl font-bold ">{formatPrice(salePrice)}</div>
                    </>
                ) : (
                    <div className="text-3xl font-bold">
                        {retailPrice ? formatPrice(retailPrice) : formatPrice(salePrice)}
                    </div>
                )}
                <Link
                    href={`/store/products/${urlTitle || '#'}`}
                    className="inline-block px-4 py-2 mt-4 text-white bg-green-500 rounded hover:bg-green-600">
                    Buy Now
                </Link>
            </div>
        </div>
    );
}

export default Product;
