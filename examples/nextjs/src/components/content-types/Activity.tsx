import Image from 'next/image';
import Link from 'next/link';

import type { DotCMSImage, DotStyleProperties } from '@/types/content';

interface ActivityProps {
    title?: string;
    description?: string;
    image?: DotCMSImage;
    urlTitle?: string;
    dotStyleProperties?: DotStyleProperties;
}

function Activity({ title, description, image, urlTitle, dotStyleProperties }: ActivityProps) {
    // Extract style properties with defaults
    const titleSize = (dotStyleProperties?.['title-size'] as string) || 'text-xl';
    const descriptionSize = (dotStyleProperties?.['description-size'] as string) || 'text-base';
    const titleStyle = (dotStyleProperties?.['title-style'] as Record<string, boolean>) || {};
    const layout = (dotStyleProperties?.layout as string) || 'left';
    const imageHeight = (dotStyleProperties?.['image-height'] as string) || 'h-56';
    const cardBackground = (dotStyleProperties?.['card-background'] as string) || 'white';
    const borderRadius = (dotStyleProperties?.['border-radius'] as string) || 'small';
    const cardEffects = (dotStyleProperties?.['card-effects'] as Record<string, boolean>) || {};
    const buttonColor = (dotStyleProperties?.['button-color'] as string) || 'blue';
    const buttonSize = (dotStyleProperties?.['button-size'] as string) || 'medium';
    const buttonStyle = (dotStyleProperties?.['button-style'] as Record<string, boolean>) || {};

    // Build title classes
    const titleClasses = [
        'mb-2',
        titleSize,
        titleStyle.bold ? 'font-bold' : 'font-normal',
        titleStyle.italic ? 'italic' : '',
        titleStyle.underline ? 'underline' : ''
    ]
        .filter(Boolean)
        .join(' ');

    // Get layout classes
    const getLayoutClasses = () => {
        switch (layout) {
            case 'right':
                return 'flex flex-row-reverse';
            case 'center':
                return 'flex flex-col items-center text-center';
            case 'overlap':
                return 'relative min-h-96';
            case 'left':
            default:
                return 'flex flex-row';
        }
    };

    const getImageContainerClasses = () => {
        switch (layout) {
            case 'overlap':
                return 'absolute inset-0 z-0';
            case 'center':
                return 'relative w-full';
            default:
                return 'relative flex-shrink-0 w-1/2';
        }
    };

    const getContentContainerClasses = () => {
        switch (layout) {
            case 'overlap':
                return 'relative z-10 p-8 bg-white/90 min-h-96 flex flex-col justify-center';
            case 'center':
                return 'w-full px-6 py-4';
            default:
                return 'flex-1 p-6 flex flex-col justify-center';
        }
    };

    // Get card background classes
    const getCardBackgroundClasses = () => {
        const bgMap: Record<string, string> = {
            white: 'bg-white',
            gray: 'bg-gray-100',
            'light-blue': 'bg-blue-50',
            'light-green': 'bg-green-50'
        };
        return bgMap[cardBackground] || bgMap.white;
    };

    // Get border radius classes
    const getBorderRadiusClasses = () => {
        const radiusMap: Record<string, string> = {
            none: 'rounded-none',
            small: 'rounded-sm',
            medium: 'rounded-md',
            large: 'rounded-lg'
        };
        return radiusMap[borderRadius] || radiusMap.small;
    };

    // Get card effect classes
    const getCardEffectClasses = () => {
        const classes = [];
        if (cardEffects.shadow) {
            classes.push('shadow-lg');
        } else {
            classes.push('shadow-md');
        }
        if (cardEffects.border) {
            classes.push('border border-gray-200');
        }
        return classes.join(' ');
    };

    // Get button color classes
    const getButtonColorClasses = () => {
        const colorMap: Record<string, string> = {
            blue: 'bg-blue-500 hover:bg-blue-700',
            green: 'bg-green-500 hover:bg-green-700',
            red: 'bg-red-500 hover:bg-red-700',
            purple: 'bg-purple-500 hover:bg-purple-700',
            orange: 'bg-orange-500 hover:bg-orange-700',
            teal: 'bg-teal-500 hover:bg-teal-700'
        };
        return colorMap[buttonColor] || colorMap.blue;
    };

    // Get button size classes
    const getButtonSizeClasses = () => {
        switch (buttonSize) {
            case 'small':
                return 'px-3 py-1.5 text-sm';
            case 'large':
                return 'px-6 py-3 text-lg';
            case 'medium':
            default:
                return 'px-4 py-2 text-base';
        }
    };

    // Get button style classes
    const getButtonStyleClasses = () => {
        const classes = [];
        if (buttonStyle.rounded) {
            classes.push('rounded-lg');
        } else if (buttonStyle['full-rounded']) {
            classes.push('rounded-full');
        } else {
            classes.push('rounded-full');
        }
        if (buttonStyle.shadow) {
            classes.push('shadow-lg');
        }
        return classes.join(' ');
    };

    const buttonClasses = [
        'inline-block',
        'font-bold',
        'text-white',
        'transition duration-300',
        getButtonColorClasses(),
        getButtonSizeClasses(),
        getButtonStyleClasses()
    ].join(' ');

    const cardClasses = [
        'overflow-hidden',
        'mb-4',
        getCardBackgroundClasses(),
        getBorderRadiusClasses(),
        getCardEffectClasses()
    ].join(' ');

    const articleClasses = [
        layout === 'overlap' ? 'p-0' : 'p-4',
        cardClasses,
        getLayoutClasses()
    ].join(' ');

    return (
        <article className={articleClasses}>
            {image?.identifier && (
                <div className={getImageContainerClasses()}>
                    <div
                        className={`relative w-full ${layout === 'overlap' ? 'h-full' : imageHeight} overflow-hidden`}>
                        <Image
                            className="object-cover"
                            src={image.identifier}
                            fill={true}
                            alt="Activity Image"
                        />
                    </div>
                </div>
            )}
            <div className={getContentContainerClasses()}>
                <p className={titleClasses}>{title}</p>
                <p className={`${descriptionSize} line-clamp-3 mb-4`}>{description}</p>
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
