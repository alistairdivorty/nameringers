import Mirror from '@/components/Mirror';
import BrandName from '@/components/BrandName';
import clsx from 'clsx';

interface Props {
    className?: String;
}

const Hero = ({ className }: Props) => (
    <div
        className={clsx(
            className,
            'relative flex justify-center px-4 md:px-10'
        )}
    >
        <div className="absolute w-full h-full bg-gradient-to-r from-blue-300 to-emerald-300 -rotate-6 origin-bottom-left scale-x-125"></div>
        <div className="relative max-w-7xl h-full grid lg:grid-cols-2 gap-7 lg:gap-0">
            <div className="lg:pl-24 pt-20 lg:pt-0 place-self-center z-10">
                <h2 className="flex flex-col items-center gap-10">
                    <BrandName className="text-5xl lg:text-[5rem]" />
                    <p
                        className="text-lg lg:text-xl font-semibold text-indigo-900"
                        style={{ lineHeight: '1.7em' }}
                    >
                        Use this tool to protect your brand against
                        typosquatting, brandjacking and domain impersonation by
                        carrying out approximate search of newly registered
                        domains.
                    </p>
                </h2>
            </div>
            <Mirror className="h-96 lg:h-2/3 place-self-end z-0 lg:z-10" />
        </div>
    </div>
);

export default Hero;
