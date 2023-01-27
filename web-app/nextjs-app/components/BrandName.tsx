import clsx from 'clsx';

interface Props {
    className?: string;
}

const BrandName = ({ className }: Props) => (
    <span
        className={clsx(
            'font-londrina whitespace-nowrap tracking-wider',
            className
        )}
    >
        <span className="text-indigo-900">name</span>
        <span className="text-pink-800">ringers</span>
    </span>
);

export default BrandName;
