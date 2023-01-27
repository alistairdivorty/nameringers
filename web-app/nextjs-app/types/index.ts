export interface IDomain {
    name: string;
    distance: number;
}

export type DomainsContextType = {
    domains: IDomain[];
    isLoading: boolean;
    query: string;
    updateIsLoading: (isLoading: boolean) => void;
    updateQuery: (query: string) => void;
    updateDistance: (distance: number) => void;
};
