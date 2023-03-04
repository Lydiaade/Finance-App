import {Component} from "react";
import {CartesianGrid, Line, LineChart, ResponsiveContainer, XAxis, YAxis, Tooltip, Label} from 'recharts';

class Chart extends Component {
    render() {
        const {data} = this.props;
        return (
            <div>
                <ResponsiveContainer width="100%" height={500}>
                    <LineChart
                        width={500}
                        height={200}
                        data={data}
                        margin={{ top: 15, right: 30, left: 20, bottom: 30 }}
                    >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name">
                            <Label value="Months" offset={-10} position="insideBottom" />
                        </XAxis>
                        <YAxis tickCount={10} label={{ value: 'Total Spend (£)', angle: -90, position: 'insideLeft' }}/>
                        <Tooltip />
                        <Line type="monotone" dataKey="total" stroke="#8884d8" fill="#8884d8" activeDot={{ r: 8 }} />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        );
    }
}

export default Chart;