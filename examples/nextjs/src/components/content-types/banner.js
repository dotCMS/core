import Image from "next/image";
import Link from "next/link";
import { DotEditableText } from "@dotcms/react";

function Banner(contentlet) {
    const { title, caption, image, link, buttonText } = contentlet;

    return (
        <div className="relative w-full h-[50vh]">
            {image && (
                <Image
                    src={image?.idPath ?? image}
                    fill={true}
                    className="object-cover"
                    alt={title}
                />
            )}
            <div className="flex absolute inset-0 flex-col justify-center items-center text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">
                    <DotEditableText
                        contentlet={contentlet}
                        fieldName="title"
                    />
                </h2>
                <p className="mb-4 text-xl text-shadow">{caption}</p>
                <Link
                    className="p-4 text-xl bg-purple-500 rounded transition duration-300 hover:bg-purple-600"
                    href={link || "#"}
                >
                    {buttonText}
                </Link>
            </div>
        </div>
    );
}

export default Banner;
