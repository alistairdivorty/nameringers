import Slider from '@mui/material/Slider';
import clsx from 'clsx';

interface Props {
    value: number;
    handleChange: (newValue: number | number[]) => void;
    className?: string;
}

const marks = [
    {
        value: 0,
        label: '0%',
    },
    {
        value: 100,
        label: '100%',
    },
];

const DistanceSlider = ({ handleChange, className }: Props) => {
    return (
        <div className={clsx(className, 'flex gap-4 items-start')}>
            <div className="text-xs font-semibold">
                Dissimilarity
                <br />
                Threshold:
            </div>
            <Slider
                size="small"
                sx={{
                    color: '#312e81',
                    '& .MuiSlider-markLabel': {
                        top: 16,
                    },
                }}
                defaultValue={75}
                valueLabelDisplay="auto"
                marks={marks}
                valueLabelFormat={(value) => <div>{value + '%'}</div>}
                onChange={(event: Event, newValue: number | number[]) =>
                    handleChange((newValue as number) / 100)
                }
            />
        </div>
    );
};

export default DistanceSlider;
