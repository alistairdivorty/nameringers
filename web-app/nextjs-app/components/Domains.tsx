import clsx from 'clsx';
import { IDomain } from '@types';
import Spinner from '@/components/Spinner';

interface Props {
    domains: Array<IDomain>;
    isLoading: boolean;
    className?: String;
}

const Domains = ({ domains, isLoading, className }: Props) => {
    const domainElements = domains.map((domain, index) => (
        <tr key={domain.name}>
            <td
                className={clsx(
                    'py-0.5 pl-5 max-w-[12rem] overflow-hidden text-ellipsis',
                    {
                        'pt-2': index === 0,
                    }
                )}
            >
                <a
                    href={`https://${domain.name}`}
                    target="_blank"
                    rel="noreferrer"
                >
                    {domain.name}
                </a>
            </td>
            <td
                className={clsx('py-0.5', {
                    'pt-2': index === 0,
                })}
            >
                {((1 - domain.distance) * 100).toFixed(6)}%
            </td>
        </tr>
    ));

    return (
        <div
            className={clsx(
                'relative min-h-[30vh] w-full max-w-[40rem] rounded-3xl border-2 border-indigo-900 bg-white overflow-scroll',
                className
            )}
        >
            <div className="absolute inset-x-1/2 -translate-x-1/2 top-[8rem] w-14">
                <Spinner
                    className={clsx({
                        hidden: !isLoading,
                    })}
                />
            </div>
            <table className="table-auto border-collapse w-full">
                <thead className="bg-slate-200">
                    <tr className="text-left">
                        <th className="pl-5 py-4">Domain name</th>
                        <th className="pr-5 py-4 w-1/3">Similarity</th>
                    </tr>
                </thead>
                <tbody className={clsx({ isLoading: 'opacity-50' })}>
                    {domainElements}
                </tbody>
            </table>
        </div>
    );
};

export default Domains;
