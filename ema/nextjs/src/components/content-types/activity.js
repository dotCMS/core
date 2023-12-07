import Image from 'next/image';
import Link from 'next/link';
import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';

function Activity({ title, description, image, urlTitle }) {
    const {
        viewAs: { language }
    } = useContext(GlobalContext);

    return (
        <article className="p-4 overflow-hidden bg-white rounded shadow-lg">
            <Image
                className="w-full"
                src={
                    image
                        ? `${process.env.NEXT_PUBLIC_DOTCMS_HOST}${image}?language_id=${language.id}`
                        : ''
                }
                width={100}
                height={100}
                alt="Activity Image"
            />
            <div className="px-6 py-4">
                <p className="mb-2 text-xl font-bold">{title}</p>
                <p className="text-base line-clamp-3">{description}</p>
            </div>
            <div className="px-6 pt-4 pb-2">
                <Link
                    href={`/activities/${urlTitle || '#'}`}
                    className="inline-block px-4 py-2 font-bold text-white bg-blue-500 rounded-full hover:bg-blue-700">
                    Link to detail →
                </Link>
            </div>
        </article>
    );
}

export default Activity;
