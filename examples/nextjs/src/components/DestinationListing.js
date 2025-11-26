import Link from "next/link";
import Image from "next/image";
import { EditButton } from "./editor/EditButton";

export default function DestinationListing({ destinations }) {
    if (!destinations || !destinations.length) {
        return <div>No destinations found</div>;
    }

    return (
        <div className="container mx-auto my-12">
            <h2 className="text-4xl font-bold mb-6 text-gray-800 text-center">
                Best Destination to Visit in {new Date().getFullYear()}
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 my-8">
                {destinations.map((destination) => (
                    <div
                        key={destination.identifier}
                        className="relative bg-white rounded-lg overflow-hidden shadow-md hover:shadow-lg transition-shadow duration-300"
                    >
                        <EditButton contentlet={destination} />
                        <div className="relative h-64">
                            <Image
                                src={destination.inode}
                                alt={destination.title}
                                layout="fill"
                                objectFit="cover"
                                className="transition-transform duration-300 hover:scale-105"
                            />
                            {destination.selectValue && (
                                <div className="absolute top-4 left-4 bg-orange-500 text-white text-sm font-bold px-4 py-1 rounded">
                                    {destination.selectValue.toUpperCase()}
                                </div>
                            )}
                        </div>
                        <div className="p-6">
                            <h2 className="text-2xl font-bold mb-3 text-gray-800">
                                <Link
                                    href={destination.url}
                                    className="hover:text-orange-500 transition-colors duration-300"
                                >
                                    {destination.title}
                                </Link>
                            </h2>
                            <p className="text-gray-600 mb-4 line-clamp-3">
                                {destination.shortDescription}
                            </p>

                            {destination.activities &&
                                destination.activities.length > 0 && (
                                    <div className="mt-4 pt-4 border-t border-gray-200">
                                        <div className="font-medium text-gray-700 mb-1">
                                            Activites:
                                        </div>
                                        <div className="text-gray-600">
                                            {destination.activities.map(
                                                (activity, index) => (
                                                    <span
                                                        key={index}
                                                        className="inline-block"
                                                    >
                                                        {activity}
                                                        {index <
                                                            destination
                                                                .activities
                                                                .length -
                                                                1 && (
                                                            <span className="mx-1 text-orange-400">
                                                                ,
                                                            </span>
                                                        )}
                                                    </span>
                                                ),
                                            )}
                                        </div>
                                    </div>
                                )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
