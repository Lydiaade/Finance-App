import React, { Component } from "react";
import "../App.css";
import { BACKEND_URL } from "../config";
import "bootstrap/dist/css/bootstrap.min.css";
import SegmentContainer from "../components/SegmentContainer";

class TransactionsPage extends Component {
  state = {
    segmentData: [],
  };

  componentDidMount() {
    this.getSegments();
  }

  getSegments = () => {
    fetch(`${BACKEND_URL}/segments`)
      .then((data) => data.json())
      .then((data) => this.setState({ segmentData: data }));
  };

  // FM-19 follow-up: SegmentContainer no longer mutates the segment list (or
  // reloads the page) itself for the rename/delete-anyway flows - this page
  // owns segmentData and updates it in place based on what actually
  // succeeded server-side.
  handleSegmentDeleted = (segmentId) => {
    this.setState((prevState) => ({
      segmentData: prevState.segmentData.filter((segment) => segment.id !== segmentId),
    }));
  };

  handleSegmentRenamed = (segmentId, newName) => {
    this.setState((prevState) => ({
      segmentData: prevState.segmentData.map((segment) =>
        segment.id === segmentId ? { ...segment, name: newName } : segment
      ),
    }));
  };

  render() {
    return (
      <div>
        <React.Fragment>
          <h1 className="pageTitle">All Segments</h1>
          <main className="container-fluid m-2">
            <div className="itemList">
              <SegmentContainer
                segments={this.state.segmentData}
                onSegmentDeleted={this.handleSegmentDeleted}
                onSegmentRenamed={this.handleSegmentRenamed}
              />
            </div>
          </main>
        </React.Fragment>
      </div>
    );
  }
}

export default TransactionsPage;
