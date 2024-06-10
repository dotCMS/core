import useLatest from "@/components/hooks/useLatest";
import Contentlets from "@/components/shared/contentlets";

export default function Destinations() {
    const destinations = useLatest("Destination");

    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7">Popular Destinations</h2>
            {destinations.length && <Contentlets contentlets={destinations} />}
        </div>
    );
}
