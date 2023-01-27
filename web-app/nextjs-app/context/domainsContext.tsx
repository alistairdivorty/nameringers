import React, { useEffect, useState } from 'react';
import { DomainsContextType, IDomain } from '@types';

type Props = {
    children: React.ReactNode;
};

const DomainsContext = React.createContext<DomainsContextType | null>(null);

const DomainsContextProvider: React.FC<Props> = ({ children }) => {
    const [domains, setDomains] = useState<IDomain[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [query, setQuery] = useState<string>('');
    const [distance, setDistance] = useState<number>(75);

    const updateIsLoading = (isLoading: boolean) => setIsLoading(isLoading);
    const updateQuery = (query: string) => setQuery(query);
    const updateDistance = (distance: number) => setDistance(distance);

    const fetchDomains = (query: string, distance: Number) => {
        if (!query) {
            setDomains([]);
            return;
        }

        const url = new URL('https://api.nameringers.com');
        url.searchParams.append('query', query);
        url.searchParams.append('distance', distance.toString());

        setIsLoading(true);

        fetch(url)
            .then((res) => res.json())
            .then((json) => {
                setDomains(
                    json.data.Get.Domain.map(
                        (domain: {
                            name: string;
                            _additional: { distance: number };
                        }) => {
                            return {
                                name: domain.name,
                                distance: domain._additional.distance,
                            };
                        }
                    )
                );

                setIsLoading(false);
            })
            .catch((err) => {
                console.log(err);
                setIsLoading(false);
            });
    };

    useEffect(() => {
        fetchDomains(query, distance);
    }, [query, distance]);

    return (
        <DomainsContext.Provider
            value={{
                domains,
                isLoading,
                query,
                updateIsLoading,
                updateQuery,
                updateDistance,
            }}
        >
            {children}
        </DomainsContext.Provider>
    );
};

export { DomainsContext, DomainsContextProvider };
