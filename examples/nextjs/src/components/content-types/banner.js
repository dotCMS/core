import Image from "next/image";
import Link from "next/link";
import { DotEditableText } from "@dotcms/react";

export function Banner(contentlet) {
    const { title, caption, image, link, buttonText, customStyles } = contentlet;
    console.log(customStyles);

    const color = customStyles?.color === 'white' ? 'text-white' : `text-${customStyles?.color}-500`;
    const fontSize = customStyles?.fontSize || 'medium';
    const mode = customStyles?.mode || 'vertical';

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

    const isHorizontal = mode === 'horizontal';

    return (
        <div className={`relative p-4 w-full ${isHorizontal ? 'flex h-64' : 'h-96'} bg-gray-200`}>
            <div className={`${isHorizontal ? 'relative w-1/2' : 'w-full h-full'}`}>
                {image && (
                    <Image
                        src={image?.idPath ?? image}
                        fill={true}
                        className="object-cover"
                        alt={title}
                    />
                )}
            </div>
            <div className={`${isHorizontal ? 'w-1/2' : 'absolute inset-0'} flex flex-col justify-center items-center p-4 text-center ${color}`}>
                <h2 className={`mb-2 font-bold ${fontSizeClass} ${isHorizontal ? '':'text-shadow'}`}>
                    {title}
                </h2>
                <p className={`mb-4 text-xl ${isHorizontal ? '':'text-shadow'}`}>{caption}</p>
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
