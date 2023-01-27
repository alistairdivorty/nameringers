import React, { useState, useRef } from 'react';
import clsx from 'clsx';
import TextInput from '@/components/TextInput';
import DistanceSlider from '@/components/DistanceSlider';
import useDomainsContext from '@/hooks/useDomainsContext';

interface Props {
    className?: String;
}

const SearchForm = ({ className }: Props) => {
    const { updateQuery, updateDistance } = useDomainsContext()!;

    const [query, setQuery] = useState<string>('');
    const [distance, setDistance] = useState<number>(75);

    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        updateQuery(query);
        updateDistance(distance);
    };

    const formRef = useRef<HTMLFormElement>(null);

    return (
        <form
            ref={formRef}
            onSubmit={(event: React.FormEvent<HTMLFormElement>) =>
                handleSubmit(event)
            }
            className={clsx(className, 'space-y-2')}
        >
            <TextInput
                handleChange={(newValue: string) => setQuery(newValue)}
                className="w-full"
            />
            <DistanceSlider
                value={distance}
                handleChange={(newValue: number | number[]) =>
                    setDistance(newValue as number)
                }
            />
        </form>
    );
};

export default SearchForm;
