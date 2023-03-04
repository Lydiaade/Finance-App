import {Component} from "react";
import {CartesianGrid, Line, LineChart, ResponsiveContainer, XAxis, YAxis, Label} from 'recharts';
import {Tooltip} from "react-bootstrap";

class Chart extends Component {
    render() {
        const {data} = this.props;
        return (
            // <ResponsiveContainer width="100%" height={200}>
            //     <LineChart width={500}
            //                height={200}
            //                data={data}
            //                syncId="anyId" margin={{
            //         top: 10,
            //         right: 30,
            //         left: 0,
            //         bottom: 0,
            //     }}>
            //         <CartesianGrid strokeDasharray="3 3"/>
            //         <XAxis dataKey="name"/>
            //         <YAxis/>
            //         <Tooltip/>
            //         <Line type="monotone" dataKey="total" stroke="#8884d8" fill="#8884d8"/>
            //     </LineChart>
            // </ResponsiveContainer>
            <div>
                <ResponsiveContainer width="100%" height={500}>
                    <LineChart
                        width={500}
                        height={200}
                        data={data}
                        margin={{ top: 15, right: 30, left: 20, bottom: 5 }}
                    >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="name">
                            <Label value="Months in year" offset={0} position="insideBottom" />
                        </XAxis>
                        <YAxis label={{ value: 'Total Spend', angle: -90, position: 'insideLeft' }}/>
                        <Tooltip />
                        <Line type="monotone" dataKey="total" stroke="#8884d8" fill="#8884d8" activeDot={{ r: 8 }} />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        );
    }
}

export default Chart;