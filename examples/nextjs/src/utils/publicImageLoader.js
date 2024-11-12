"use client";

import { useEffect, useState } from "react";

const PublicImageLoader = ({ src }) => {
    const [imageSrc, setImageSrc] = useState(null);
    useEffect(() => setImageSrc(`${window.location.origin}${src}`), [src]);
    return imageSrc ?? "/";
};

export default PublicImageLoader;