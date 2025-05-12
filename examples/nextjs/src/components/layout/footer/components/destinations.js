import Contentlets from '@/components/contentlets';

export default function Destinations({ destinations }) {
    return (
        <div className="flex flex-col">
            <h2 className="text-2xl font-bold mb-7 text-white">Popular Destinations</h2>
            {!!destinations.length && <Contentlets contentlets={destinations} />}
        </div>
    );
}
