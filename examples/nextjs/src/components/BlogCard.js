import { useIsEditMode } from "@/hooks/isEditMode";
import { editContentlet } from "@dotcms/uve";
import Image from "next/image";

const dateFormatOptions = {
    year: "numeric",
    month: "long",
    day: "numeric",
};

export default function BlogCard({ blog }) {
    const { title, image, urlMap, inode, modDate, urlTitle, teaser } = blog;

    const isEditMode = useIsEditMode();

    return (
        <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 relative flex flex-col h-full">
            {isEditMode && (
                <button
                    onClick={() => editContentlet(blog)}
                    className="absolute top-2 right-2 z-10 bg-blue-500 text-white rounded-md py-2 px-4 shadow-md hover:bg-blue-600"
                >
                    Edit
                </button>
            )}

            <div className="relative h-48 w-full">
                {image ? (
                    <Image
                        src={inode}
                        alt={urlTitle || title}
                        fill={true}
                        className="object-cover"
                    />
                ) : (
                    <div className="absolute inset-0 bg-gray-200 flex items-center justify-center">
                        <span className="text-gray-400">No image</span>
                    </div>
                )}
            </div>

            <div className="p-4 flex flex-col grow">
                <h3 className="text-lg font-bold mb-2 hover:text-blue-600">
                    <a href={urlMap}>{title}</a>
                </h3>

                {teaser && (
                    <p className="text-gray-600 text-sm mb-3 line-clamp-2">
                        {teaser}
                    </p>
                )}

                <div className="flex justify-between items-center mt-auto pt-3 border-t border-gray-100">
                    <time className="text-sm text-gray-500">
                        {new Date(modDate).toLocaleDateString(
                            "en-US",
                            dateFormatOptions,
                        )}
                    </time>
                </div>
            </div>
        </div>
    );
}
