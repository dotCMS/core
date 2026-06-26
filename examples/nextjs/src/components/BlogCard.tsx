import Image from 'next/image';
import type { DotCMSBasicContentlet } from '@dotcms/types';

import { useIsEditMode } from '@/hooks/useIsEditMode';
import { editContentlet } from '@dotcms/uve';
import type { Blog } from '@/types/content';
import { formatDate } from '@/utils/formatDate';

interface BlogCardProps {
    blog: Blog;
}

export default function BlogCard({ blog }: BlogCardProps) {
    const { title, urlMap, inode, modDate, urlTitle, teaser } = blog;
    const isEditMode = useIsEditMode();

    return (
        <article className="group relative flex h-full flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
            {isEditMode && (
                <button
                    type="button"
                    // The Blog fragment is identified by `identifier`; widen at
                    // the SDK boundary to the full contentlet shape it expects.
                    onClick={() => editContentlet(blog as DotCMSBasicContentlet)}
                    className="absolute right-3 top-3 z-10 rounded-full bg-primary px-4 py-1.5 text-sm font-semibold text-bg shadow-md transition-colors hover:bg-primary-deep"
                >
                    Edit
                </button>
            )}

            <div className="relative aspect-[3/2] w-full overflow-hidden bg-surface">
                {inode ? (
                    <Image
                        src={inode}
                        alt={urlTitle || title}
                        fill
                        sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
                        className="object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105"
                    />
                ) : (
                    <div className="grid h-full place-items-center text-sm text-muted">
                        No image
                    </div>
                )}
            </div>

            <div className="flex grow flex-col p-5">
                <h3 className="font-display text-xl font-semibold leading-snug text-ink">
                    <a
                        href={urlMap}
                        className="transition-colors after:absolute after:inset-0 hover:text-primary"
                    >
                        {title}
                    </a>
                </h3>

                {teaser && (
                    <p className="mt-2 line-clamp-2 leading-relaxed text-muted">{teaser}</p>
                )}

                {modDate && (
                    <time className="mt-auto pt-4 text-sm text-muted">{formatDate(modDate)}</time>
                )}
            </div>
        </article>
    );
}
