import { useContext } from 'react';
import { DomainsContext } from '@/context/domainsContext';

const useDomainsContext = () => {
    return useContext(DomainsContext);
};

export default useDomainsContext;
