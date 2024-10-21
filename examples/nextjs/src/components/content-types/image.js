import Image from "next/image";

function ImageComponent({ fileAsset, title, description }) {
    return (
        <div className="overflow-hidden relative h-full bg-white rounded shadow-lg group">
            <div className="relative w-full h-full bg-gray-200">
                {fileAsset && (
                    <Image
                        src={fileAsset?.idPath ?? fileAsset}
                        fill={true}
                        className="object-cover w-full h-full"
                        alt={title}
                    />
                )}
            </div>
            <div className="absolute bottom-0 px-6 py-8 w-full text-white bg-orange-500 bg-opacity-80 transition-transform duration-300 translate-y-full w-100 group-hover:translate-y-0">
                <div className="mb-2 text-2xl font-bold">{title}</div>
                <p className="text-base">{description}</p>
            </div>
        </div>
    );
}

export default ImageComponent;
