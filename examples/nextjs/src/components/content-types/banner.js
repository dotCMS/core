import Image from "next/image";
import Link from "next/link";
import { DotEditableText } from "@dotcms/react";

export function Banner(contentlet) {
    const { title, caption, image, link, buttonText, customStyles } = contentlet;
    console.log(customStyles);

    const color = customStyles?.color ? `text-${customStyles.color}-500` : 'text-white';
    
    const fontSize = customStyles?.fontSize || 'medium';
    let fontSizeClass;

    switch (fontSize) {
        case 'small':
            fontSizeClass = 'text-2xl';
            break;
        case 'medium':
            fontSizeClass = 'text-4xl';
            break;
        case 'large':
            fontSizeClass = 'text-6xl';
            break;
        default:
            fontSizeClass = 'text-base';
    }

    return (
        <div className="relative p-4 w-full h-96 bg-gray-200">
            {image && (
                <Image
                    src={image?.idPath ?? image}
                    fill={true}
                    className="object-cover"
                    alt={title}
                />
            )}
            <div className={`flex absolute inset-0 flex-col justify-center items-center p-4 text-center ${color}`}>
                <h2 className={`mb-2 font-bold ${fontSizeClass} text-shadow`}>
                    {title}
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
