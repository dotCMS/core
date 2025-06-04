import type { DotCMSBasicContentlet } from "@dotcms/types";

interface ProductProps extends DotCMSBasicContentlet {
  salePrice: number;
  retailPrice: number;
  urlTitle: string;
}
function Product({
  inode,
  image,
  title,
  salePrice,
  retailPrice,
  urlTitle,
}: ProductProps) {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(price);
  };

  return (
    <div className="overflow-hidden bg-white rounded-sm shadow-lg">
      <div className="p-4">
        {image && (
          <img
            className="object-contain w-full max-h-60"
            src={`/dA/${inode}`}
            width={100}
            height={100}
            alt="Activity Image"
          />
        )}
      </div>
      <div className="px-6 py-4 bg-slate-100">
        <div className="mb-2 text-xl font-bold line-clamp-1">{title}</div>
        {retailPrice && salePrice ? (
          <>
            <div className="text-gray-500 line-through">
              {formatPrice(retailPrice)}
            </div>
            <div className="text-3xl font-bold ">{formatPrice(salePrice)}</div>
          </>
        ) : (
          <>
            <div className="min-h-6" />
            <div className="text-3xl font-bold">
              {retailPrice ? formatPrice(retailPrice) : formatPrice(salePrice)}
            </div>
          </>
        )}
        <a
          href={`/store/products/${urlTitle || "#"}`}
          className="inline-block px-4 py-2 mt-4 text-white bg-green-500 rounded-sm hover:bg-green-600"
        >
          Buy Now
        </a>
      </div>
    </div>
  );
}

export default Product;
