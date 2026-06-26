import { cva } from 'class-variance-authority';
import Image from 'next/image';
import Link from 'next/link';

import { cn, variant } from '@/lib/utils';
import type { DotCMSImage, DotStyleProperties } from '@/types/content';
import { BUTTON_COLORS, buttonColorVariants } from './styles';

const LAYOUTS = ['left', 'right', 'center', 'overlap'] as const;
const BACKGROUNDS = ['white', 'gray', 'light-blue', 'light-green'] as const;
const RADII = ['none', 'small', 'medium', 'large'] as const;
const BUTTON_SIZES = ['small', 'medium', 'large'] as const;

interface ActivityProps {
    title?: string;
    description?: string;
    image?: DotCMSImage;
    urlTitle?: string;
    dotStyleProperties?: DotStyleProperties;
}

// Layout drives three coordinated containers (article / image / content).
const articleVariants = cva('overflow-hidden mb-4', {
    variants: {
        layout: {
            left: 'flex flex-row p-4',
            right: 'flex flex-row-reverse p-4',
            center: 'flex flex-col items-center text-center p-4',
            overlap: 'relative min-h-96 p-0'
        }
    },
    defaultVariants: { layout: 'left' }
});

const imageContainerVariants = cva('', {
    variants: {
        layout: {
            left: 'relative flex-shrink-0 w-1/2',
            right: 'relative flex-shrink-0 w-1/2',
            center: 'relative w-full',
            overlap: 'absolute inset-0 z-0'
        }
    },
    defaultVariants: { layout: 'left' }
});

const contentContainerVariants = cva('', {
    variants: {
        layout: {
            left: 'flex-1 p-6 flex flex-col justify-center',
            right: 'flex-1 p-6 flex flex-col justify-center',
            center: 'w-full px-6 py-4',
            overlap: 'relative z-10 p-8 bg-white/90 min-h-96 flex flex-col justify-center'
        }
    },
    defaultVariants: { layout: 'left' }
});

const cardBackgroundVariants = cva('', {
    variants: {
        background: {
            white: 'bg-white',
            gray: 'bg-gray-100',
            'light-blue': 'bg-blue-50',
            'light-green': 'bg-green-50'
        }
    },
    defaultVariants: { background: 'white' }
});

const borderRadiusVariants = cva('', {
    variants: {
        radius: {
            none: 'rounded-none',
            small: 'rounded-sm',
            medium: 'rounded-md',
            large: 'rounded-lg'
        }
    },
    defaultVariants: { radius: 'small' }
});

const buttonSizeVariants = cva('', {
    variants: {
        size: {
            small: 'px-3 py-1.5 text-sm',
            medium: 'px-4 py-2 text-base',
            large: 'px-6 py-3 text-lg'
        }
    },
    defaultVariants: { size: 'medium' }
});

function Activity({ title, description, image, urlTitle, dotStyleProperties }: ActivityProps) {
    const titleSize = dotStyleProperties?.['title-size'] || 'text-xl';
    const descriptionSize = dotStyleProperties?.['description-size'] || 'text-base';
    const titleStyle = dotStyleProperties?.['title-style'] || {};
    const layout = variant(dotStyleProperties?.layout, LAYOUTS) ?? 'left';
    const imageHeight = dotStyleProperties?.['image-height'] || 'h-56';
    const cardBackground = dotStyleProperties?.['card-background'];
    const borderRadius = dotStyleProperties?.['border-radius'];
    const cardEffects = dotStyleProperties?.['card-effects'] || {};
    const buttonColor = dotStyleProperties?.['button-color'];
    const buttonSize = dotStyleProperties?.['button-size'];
    const buttonStyle = dotStyleProperties?.['button-style'] || {};

    const isOverlap = layout === 'overlap';

    const titleClasses = cn(
        'mb-2',
        titleSize,
        titleStyle.bold ? 'font-bold' : 'font-normal',
        titleStyle.italic && 'italic',
        titleStyle.underline && 'underline'
    );

    const articleClasses = cn(
        articleVariants({ layout }),
        cardBackgroundVariants({ background: variant(cardBackground, BACKGROUNDS) }),
        borderRadiusVariants({ radius: variant(borderRadius, RADII) }),
        cardEffects.shadow ? 'shadow-lg' : 'shadow-md',
        cardEffects.border && 'border border-gray-200'
    );

    const buttonClasses = cn(
        'inline-block font-bold text-white transition duration-300',
        buttonColorVariants({ color: variant(buttonColor, BUTTON_COLORS) }),
        buttonSizeVariants({ size: variant(buttonSize, BUTTON_SIZES) }),
        // Note: both rounded and full-rounded fall back to a pill in this layout.
        buttonStyle.rounded ? 'rounded-lg' : 'rounded-full',
        buttonStyle.shadow && 'shadow-lg'
    );

    return (
        <article className={articleClasses}>
            {image?.identifier && (
                <div className={imageContainerVariants({ layout })}>
                    <div className={cn('relative w-full overflow-hidden', isOverlap ? 'h-full' : imageHeight)}>
                        <Image
                            className="object-cover"
                            src={image.identifier}
                            fill={true}
                            alt="Activity Image"
                        />
                    </div>
                </div>
            )}
            <div className={contentContainerVariants({ layout })}>
                <p className={titleClasses}>{title}</p>
                <p className={cn('line-clamp-3 mb-4', descriptionSize)}>{description}</p>
                <div className={layout === 'center' ? 'flex justify-center' : ''}>
                    <Link href={`/activities/${urlTitle || '#'}`} className={buttonClasses}>
                        Link to detail →
                    </Link>
                </div>
            </div>
        </article>
    );
}

export default Activity;
