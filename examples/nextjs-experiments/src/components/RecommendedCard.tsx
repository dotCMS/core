import Image from 'next/image';

import type { Blog, Destination } from '@/types/content';
import { formatDate } from '@/utils/formatDate';
import { EditButton } from './editor/EditButton';

interface RecommendedCardProps {
    contentlet: Blog | Destination;
}

const RecommendedCard = ({ contentlet }: RecommendedCardProps) => {
    const { title, inode, urlMap, modDate } = contentlet;
    const url = 'url' in contentlet ? contentlet.url : undefined;
    const href = urlMap || url || '#';

    return (
        <article className="group relative flex items-center gap-4">
            <EditButton contentlet={contentlet} />
            <a
                href={href}
                className="relative aspect-square w-16 shrink-0 overflow-hidden rounded-lg bg-bg/10"
                tabIndex={-1}
                aria-hidden="true"
            >
                {inode && (
                    <Image
                        src={inode}
                        alt=""
                        fill
                        sizes="64px"
                        className="object-cover transition-transform duration-500 ease-(--ease-out-quart) group-hover:scale-105"
                    />
                )}
            </a>
            <div className="flex min-w-0 flex-col gap-1">
                <a
                    href={href}
                    className="line-clamp-2 text-sm font-medium leading-snug text-bg transition-colors hover:text-accent-soft"
                >
                    {title}
                </a>
                {modDate && (
                    <time className="text-xs text-bg/55">{formatDate(modDate)}</time>
                )}
            </div>
        </article>
    );
};

export default RecommendedCard;
