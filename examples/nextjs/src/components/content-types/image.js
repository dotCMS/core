import Image from "next/image";

function ImageComponent({ fileAsset, title, description }) {
    return (
        <div className="relative overflow-hidden bg-white rounded shadow-lg group">
            <div className="relative w-full bg-gray-200 h-96">
                {fileAsset && (
                    <Image
                        src={fileAsset?.idPath ?? fileAsset}
                        fill={true}
                        className="object-cover w-full h-full"
                        alt={title}
                    />
                )}
            </div>
            <div className="absolute bottom-0 w-full px-6 py-8 text-white transition-transform duration-300 translate-y-full bg-orange-500 bg-opacity-80 w-100 group-hover:translate-y-0">
                <div className="mb-2 text-2xl font-bold">{title}</div>
                <p className="text-base">{description}</p>
            </div>
        </div>
    );
}

export default ImageComponent;
