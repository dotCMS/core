import RecommendedCard from "@/components/RecommendedCard";

export default function Destinations({ destinations }) {
    if (!destinations?.length) return null;

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-white">
                Popular Destinations
            </h2>
            <div className="flex flex-col gap-5">
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
