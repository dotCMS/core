import Image from 'next/image';
import Link from 'next/link';
import { DotCMSEditableText } from '@dotcms/react/next';

function Banner(contentlet) {
    const { title, caption, inode, image, link, buttonText } = contentlet;

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            {image && (
                <Image
                    src={inode}
                    fill={true}
                    className="object-cover"
                    alt={title}
                />
            )}
            <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
                <h2 className="mb-2 text-6xl font-bold text-shadow">
                    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
                </h2>
                <p className="mb-4 text-xl text-shadow">{caption}</p>
                <Link
                    className="p-4 text-xl transition duration-300 bg-indigo-600 rounded hover:bg-indigo-700"
                    href={link || '#'}>
                    {buttonText}
                </Link>
            </div>
        </div>
    );
}

export default Banner;
