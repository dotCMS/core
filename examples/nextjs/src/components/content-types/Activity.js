import Image from "next/image";
import Link from "next/link";

function Activity({ title, description, image, inode, urlTitle }) {
    return (
        <article className="p-4 overflow-hidden bg-white rounded-sm shadow-lg mb-4">
            {image && (
                <div className="relative w-full h-56 overflow-hidden">
                    <Image
                        className="object-cover"
                        src={inode}
                        fill={true}
                        alt="Activity Image"
                    />
                </div>
            )}
            <div className="px-6 py-4">
                <p className="mb-2 text-xl font-bold">{title}</p>
                <p className="text-base line-clamp-3">{description}</p>
            </div>
            <div className="px-6 pt-4 pb-2">
                <Link
                    href={`/activities/${urlTitle || "#"}`}
                    className="inline-block px-4 py-2 font-bold rounded-full bg-blue-500 hover:bg-blue-700 text-white"
                >
                    Link to detail →
                </Link>
            </div>
        </article>
    );
}

export default Activity;
