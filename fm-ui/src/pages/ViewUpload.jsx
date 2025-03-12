import React, { Component } from "react";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import { BACKEND_URL } from "../config";
import UploadData from "../components/UploadData";

class ViewUpload extends Component {
  state = {};

  componentDidMount() {
    // this.getUploadData();
  }

  // getUploads = () => {
  //   fetch(`${BACKEND_URL}/uploads/upload/`)
  //     .then((data) => data.json())
  //     .then((data) => this.setState({ uploads: data }));
  // };

  render() {
    console.log(this.props);
    return (
      <div className="container">
        <h1 className="pageTitle">Upload Details</h1>
      </div>
    );
  }
}

export default ViewUpload;
