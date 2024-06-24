import Image from "next/image";
import Link from "next/link";
import { useDotcmsPageContext } from "@dotcms/react";
import imageLoaderByConfig from "@/utils/imageLoader";

function Activity({ title, description, image, urlTitle }) {
    const {
        pageAsset: {
            viewAs: { language },
        },
    } = useDotcmsPageContext();

    return (
        <article className="p-4 overflow-hidden bg-white rounded shadow-lg">
            {image && (
                <Image
                    className="w-full"
                    loader={imageLoaderByConfig({
                        language: language?.id ?? 1,
                    })}
                    src={image?.idPath ?? image}
                    width={100}
                    height={100}
                    alt="Activity Image"
                />
            )}
            <div className="px-6 py-4">
                <p className="mb-2 text-xl font-bold">{title}</p>
                <p className="text-base line-clamp-3">{description}</p>
            </div>
            <div className="px-6 pt-4 pb-2">
                <Link
                    href={`/activities/${urlTitle || "#"}`}
                    className="inline-block px-4 py-2 font-bold text-white bg-purple-500 rounded-full hover:bg-purple-700"
                >
                    Link to detail →
                </Link>
            </div>
        </article>
    );
}

export default Activity;
