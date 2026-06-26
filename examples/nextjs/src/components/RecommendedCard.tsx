import Image from 'next/image';
import { EditButton } from './editor/EditButton';
import type { Blog, Destination } from '@/types/content';

interface RecommendedCardProps {
    contentlet: Blog | Destination;
}

const RecommendedCard = ({ contentlet }: RecommendedCardProps) => {
    const { title, inode, urlMap, modDate } = contentlet;
    const url = 'url' in contentlet ? contentlet.url : undefined;
    const dateFormatOptions: Intl.DateTimeFormatOptions = {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    };

    return (
        <div className="flex gap-7 min-h-16 relative">
            <EditButton contentlet={contentlet} />
            <a className="relative min-w-32" href={urlMap || url}>
                {inode && <Image src={inode} alt={title} fill={true} className="object-cover" />}
            </a>
            <div className="flex flex-col gap-1">
                <a className="text-sm text-white font-bold" href={urlMap || url}>
                    {title}
                </a>
                <time className="text-gray-400">
                    {new Date(modDate ?? '').toLocaleDateString('en-US', dateFormatOptions)}
                </time>
            </div>
        </div>
    );
};

export default RecommendedCard;
