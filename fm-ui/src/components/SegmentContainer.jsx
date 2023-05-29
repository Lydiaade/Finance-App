import React, {Component} from "react";

class SegmentContainer extends Component {

    render() {
        const {segments} = this.props;

        return (
            <table className="table container-fluid">
                <thead>
                <tr className="segment-header">
                    <th scope="col" className="Segment">Segment Name</th>
                </tr>
                </thead>
                <tbody>
                {segments.map((segment) => (
                    <tr className="segment" key={segments.indexOf(segment)}>
                        <td className="segmentName">{segment.name}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        );
    }
}

export default SegmentContainer;
