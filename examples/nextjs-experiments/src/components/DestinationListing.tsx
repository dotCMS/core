import Image from 'next/image';
import Link from 'next/link';

import type { Destination } from '@/types/content';
import { EditButton } from './editor/EditButton';

interface DestinationListingProps {
    destinations?: Destination[];
}

export default function DestinationListing({ destinations }: DestinationListingProps) {
    if (!destinations?.length) {
        return (
            <p className="py-12 text-center text-muted">No destinations yet.</p>
        );
    }

    return (
        <section className="flex flex-col gap-10">
            <header className="flex max-w-2xl flex-col gap-3">
                <span className="eyebrow">Where to next</span>
                <h2 className="font-display text-h2 font-semibold text-ink">
                    Destinations worth the journey
                </h2>
                <p className="text-lg leading-relaxed text-muted">
                    Hand-picked places our writers keep returning to, with the
                    experiences that make each one worth the trip.
                </p>
            </header>

            <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,18rem),1fr))] gap-6">
                {destinations.map((destination) => (
                    <article
                        key={destination.identifier}
                        className="group relative flex flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5"
                    >
                        <EditButton contentlet={destination} />
                        <div className="relative h-60 overflow-hidden">
                            {destination.image && (
                                <Image
                                    src={destination.image as string}
                                    alt={destination.title}
                                    fill
                                    sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
                                    className="object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105"
                                />
                            )}
                            <div
                                aria-hidden="true"
                                className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100"
                            />
                            {destination.selectValue && (
                                <span className="absolute left-4 top-4 rounded-full bg-primary-deep/85 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-bg backdrop-blur-sm">
                                    {destination.selectValue}
                                </span>
                            )}
                        </div>

                        <div className="flex flex-1 flex-col p-6">
                            <h3 className="font-display text-2xl font-semibold leading-tight text-ink">
                                <Link
                                    href={destination.url ?? '#'}
                                    className="transition-colors after:absolute after:inset-0 hover:text-primary"
                                >
                                    {destination.title}
                                </Link>
                            </h3>
                            <p className="mt-3 line-clamp-3 leading-relaxed text-muted">
                                {destination.shortDescription}
                            </p>

                            {destination.activities && destination.activities.length > 0 && (
                                <ul className="mt-5 flex flex-wrap gap-2 border-t border-line pt-5">
                                    {destination.activities.map((activity, index) => (
                                        <li
                                            key={index}
                                            className="rounded-full bg-surface px-3 py-1 text-xs font-medium text-ink"
                                        >
                                            {activity}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </article>
                ))}
            </div>
        </section>
    );
}
