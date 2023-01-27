import clsx from 'clsx';
interface Props {
    handleChange: (newValue: string) => void;
    className?: string;
}

const TextInput = ({ handleChange, className }: Props) => (
    <div className={clsx(className, 'h-12 flex')}>
        <input
            type="text"
            onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                handleChange(event.target.value)
            }
            className="h-full w-full border-t-2 border-b-2 border-l-2 border-indigo-900 rounded-l-lg bg-white px-4 py-2 focus:outline-none focus:border-indigo-400 transition-colors"
        />
        <button
            type="submit"
            className="rounded-r-lg bg-indigo-900 hover:bg-pink-800 transition-colors text-white font-semibold px-4"
        >
            Search
        </button>
    </div>
);

export default TextInput;
