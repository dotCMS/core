import RecommendedCard from "@/components/RecommendedCard";
import type { Destination } from "@/types/content";

interface DestinationsProps {
    destinations?: Destination[];
}

export default function Destinations({ destinations }: DestinationsProps) {
    if (!destinations?.length) return null;

    return (
        <div className="flex flex-col">
            <h2 className="mb-6 text-sm font-semibold uppercase tracking-wider text-bg/60">
                Popular destinations
            </h2>
            <div className="flex flex-col gap-4">
                {destinations.map((destination) => (
                    <RecommendedCard
                        key={destination.identifier}
                        contentlet={destination}
                    />
                ))}
            </div>
        </div>
    );
}
