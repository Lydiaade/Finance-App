import React, { useState } from "react";
import { BACKEND_URL } from "../config";
import "./SegmentContainer.css";

const SegmentContainer = ({ segments }) => {
  const [segmentValue, setSegementValue] = useState("");

  const handleKeyPress = async (e) => {
    if (e.key === "Enter") {
      try {
        fetch(`${BACKEND_URL}/segments/segment`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ name: segmentValue }),
        });
        window.location.reload();
      } catch (error) {
        console.error("Error saving data:", error);
      }
    }
  };

  function deleteSegment(segmentId) {
    fetch(`${BACKEND_URL}/segments/segment/${segmentId}`, {
      method: "DELETE",
    }).then((data) => console.log(data));
    window.location.reload();
  }

  return (
    <div>
      <table className="table container-fluid">
        <thead>
          <tr className="segmentHeader">
            <th scope="col" className="SegmentName">
              Segment Name
            </th>
            <th scope="col" className="SegmentDelete"></th>
          </tr>
        </thead>
        <tbody>
          {!(segments && Array.isArray(segments))
            ? null
            : segments.map((segment) => (
                <tr className="segment" key={segments.indexOf(segment)}>
                  <td className="segmentName">{segment.name}</td>
                  <td className="segmentDelete">
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={deleteSegment.bind(this, segment.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
        </tbody>
      </table>
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
    </div>
  );
};

export default SegmentContainer;
