import Image from 'next/image';

import type { DotCMSImage } from '@/types/content';

interface ImageComponentProps {
    fileAsset?: DotCMSImage;
    title?: string;
    description?: string;
}

function ImageComponent({ fileAsset, title, description }: ImageComponentProps) {
    const hasCaption = Boolean(title || description);

    return (
        <figure className="group relative isolate overflow-hidden rounded-2xl bg-surface-2">
            <div className="relative h-96 w-full">
                {fileAsset?.identifier && (
                    <Image
                        src={fileAsset.identifier}
                        fill
                        sizes="(min-width: 1024px) 50vw, 100vw"
                        className="object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105"
                        alt={title ?? ''}
                    />
                )}
            </div>
            {hasCaption && (
                <figcaption className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/75 via-black/30 to-transparent px-6 pb-6 pt-16 text-bg">
                    {title && (
                        <p className="font-display text-2xl font-semibold leading-tight">{title}</p>
                    )}
                    {description && <p className="mt-1 text-sm text-bg/85">{description}</p>}
                </figcaption>
            )}
        </figure>
    );
}

export default ImageComponent;
