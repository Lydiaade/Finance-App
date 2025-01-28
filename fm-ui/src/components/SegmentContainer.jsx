import React, {useState} from "react";
import {BACKEND_URL} from "../config";

const SegmentContainer = ( {segments}) => {
    const [segmentValue, setSegementValue] = useState("");

    const handleKeyPress = async (e) => {
        if (e.key === "Enter") {
          try {
            fetch(`${BACKEND_URL}/segments/segment`, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({"name": segmentValue})
                    })
            window.location.reload();
          } catch (error) {
            console.error("Error saving data:", error);
          }
        }
      };

    return (
            <table className="table container-fluid">
                <thead>
                <tr className="segment-header">
                    <th scope="col" className="Segment">Segment Name</th>
                </tr>
                </thead>
                <tbody>
                {!(segments && Array.isArray(segments)) ? null : segments.map((segment) => (
                     <tr className="segment" key={segments.indexOf(segment)}>
                         <td className="segmentName">{segment.name}</td>
                     </tr>
                ))}
            <tr>
            <div className="form-group">
                <input
                  type="text"
                  className="form-control"
                  id="inputBox"
                  placeholder="Add a new segment"
                  value={segmentValue}
                  onChange={(e) => setSegementValue(e.target.value)}
                  onKeyDown={handleKeyPress}
                />
                </div>
                </tr>
                </tbody>
            </table>
    );
}

export default SegmentContainer;
