import Image from "next/image";
import Link from "next/link";

function Activity({ title, description, image, urlTitle, customStyles }) {
    const showImage = customStyles?.showImage;
    console.log(showImage);

    return (
        <article className="overflow-hidden p-4 bg-white rounded shadow-lg">
            {(image && showImage) && (
                <Image
                    className="w-full"
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
