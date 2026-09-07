import Image from 'next/image';
import Link from 'next/link';

import type { DotCMSImage } from '@/types/content';

interface CalendarActivity {
    title?: string;
    urlMap?: string;
}

interface CalendarLocation {
    title?: string;
    url?: string;
    activities?: CalendarActivity[];
}

interface CalendarEventProps {
    image?: DotCMSImage;
    title?: string;
    urlMap: string;
    description?: string;
    location?: CalendarLocation[];
}

interface ExtractedLocationsAndActivities {
    locations: Omit<CalendarLocation, 'activities'>[];
    activities: CalendarActivity[];
}

function extractLocationsAndActivities(
    contentlet?: CalendarLocation[]
): ExtractedLocationsAndActivities {
    const initialValue: ExtractedLocationsAndActivities = {
        locations: [],
        activities: []
    };

    return (
        contentlet?.reduce((acc, { activities, ...location }) => {
            acc.activities = acc.activities.concat(activities ?? []);
            acc.locations.push(location);

            return acc;
        }, initialValue) ?? initialValue
    );
}

function CalendarEvent({ image, title, urlMap, description, location }: CalendarEventProps) {
    const { locations, activities } = extractLocationsAndActivities(location);

    return (
        <article className="flex w-full flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm sm:flex-row">
            <div className="relative h-56 shrink-0 bg-surface sm:h-auto sm:w-2/5">
                {image?.identifier && (
                    <Image
                        src={image.identifier}
                        alt={title ?? ''}
                        fill
                        sizes="(min-width: 640px) 40vw, 100vw"
                        className="object-cover"
                    />
                )}
            </div>
            <div className="flex flex-col gap-4 p-6 sm:p-8">
                <h3 className="font-display text-2xl font-semibold leading-snug text-ink">
                    {title}
                </h3>

                {!!locations.length && !!locations[0]?.title && (
                    <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-muted">Locations</span>
                        {locations.map(({ title, url }, index) => (
                            <Link
                                key={index}
                                href={url ?? ''}
                                className="rounded-full bg-primary-tint px-2.5 py-0.5 text-xs font-medium text-primary transition-colors hover:bg-primary hover:text-bg"
                            >
                                {title}
                            </Link>
                        ))}
                    </div>
                )}

                {!!activities.length && !!activities[0]?.title && (
                    <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-muted">Activities</span>
                        {activities.slice(0, 3).map(({ title, urlMap }, index) => (
                            <Link
                                key={index}
                                href={urlMap ?? ''}
                                className="rounded-full bg-surface px-2.5 py-0.5 text-xs font-medium text-ink transition-colors hover:bg-surface-2"
                            >
                                {title}
                            </Link>
                        ))}
                    </div>
                )}

                <div
                    className="line-clamp-3 leading-relaxed text-muted"
                    dangerouslySetInnerHTML={{ __html: description ?? '' }}
                />

                <Link
                    href={urlMap}
                    className="mt-1 inline-flex w-fit items-center gap-1.5 font-semibold text-primary transition-colors hover:text-primary-deep"
                >
                    Learn more
                    <span aria-hidden="true">→</span>
                </Link>
            </div>
        </article>
    );
}

export default CalendarEvent;
