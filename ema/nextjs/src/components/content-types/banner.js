import Image from 'next/image';
import Link from 'next/link';
import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';

function Banner({ title, image, caption, buttonText, link }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            <Image
                src={`${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`}
                fill={true}
                className="object-cover"
                alt={title}
            />
            <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">{title}</h2>
                <p className="mb-4 text-xl text-shadow">{caption}</p>
                <Link
                    className="p-4 text-xl transition duration-300 bg-blue-500 rounded hover:bg-blue-600"
                    href={link || '#'}>
                    {buttonText}
                </Link>
            </div>
        </div>
    );
}

export default Banner;
