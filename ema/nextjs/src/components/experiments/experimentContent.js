'use client';
// TODO: Pass this to sdk/nextjs

import { useEffect } from 'react';
import { ExperimentHelper } from '../../../public/experiment_logic';
import { useJitsu } from '@jitsu/react';
import { useRouter } from 'next/navigation';

const helper = new ExperimentHelper();

function ExperimentContent({ children }) {
    const router = useRouter();

    const { trackPageView, id, set, unset } = useJitsu();
    useEffect(() => {
        set(helper.getExperimentJitsuData());
        async function checkExperiments() {
            if (typeof window !== 'undefined') {
                const currentPageUrl = new URL(window.location);
                await helper.check(currentPageUrl);

                const { experiments } = helper.getLocalStorageData();

                experiments.forEach(({ pageUrl, variant }) => {
                    document.querySelectorAll(`a[href*='${pageUrl}']`).forEach((enlace) => {
                        const urlFinal = `${window.location.origin}${variant.url}`;

                        // Only update if the href is different to avoid unnecessary work
                        if (enlace.href !== urlFinal) {
                            enlace.href = urlFinal;

                            enlace.addEventListener('click', function (event) {
                                event.preventDefault(); // not allow the default behavior
                                router.replace(urlFinal); // Next.js Router
                            });
                        }
                    });
                });

                trackPageView(); // Auto pageview from Jitsue
            }
        }

        checkExperiments(); // All the logic to handle Experiments (future sdk/experiments)
    }, [router, set, trackPageView]);

    return <div>{children}</div>;
}

export default ExperimentContent;
