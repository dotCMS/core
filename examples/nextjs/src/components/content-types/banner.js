import Image from "next/image";
import Link from "next/link";

export function Banner(contentlet) {
    const { title, caption, image, link, buttonText, customStyles } = contentlet;

    const colorClass = customStyles?.color;
    const fontSizeClass = customStyles?.fontSize;
    const mode = customStyles?.mode || 'vertical';


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
            <div className={`${isHorizontal ? 'w-1/2' : 'absolute inset-0'} flex flex-col justify-center items-center p-4 text-center ${colorClass}`}>
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
