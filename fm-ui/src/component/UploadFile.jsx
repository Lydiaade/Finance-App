import {Component} from 'react';
import {BACKEND_URL} from "../config";

class UploadFile extends Component {
    state = {}
    onFileChange = event => {
        this.setState({selectedFile: event.target.files[0]});
    };

    // On file upload (click the upload button)
    onFileUpload = () => {
        const formData = new FormData();

        formData.append(
            "myFile",
            this.state.selectedFile,
            this.state.selectedFile.name
        );

        console.log(this.state.selectedFile);

        fetch(`${BACKEND_URL}upload/transactions`, {method: "POST", body: formData})
            .then((response) => console.log(response.status));
    };

    // File content to be displayed after
    // file upload is complete
    fileData = () => {

        if (this.state.selectedFile) {

            return (
                <div>
                    <h2>File Details:</h2>
                    <p>File Name: {this.state.selectedFile.name}</p>

                    <p>File Type: {this.state.selectedFile.type}</p>

                    <p>
                        Last Modified:{" "}
                        {this.state.selectedFile.lastModifiedDate.toDateString()}
                    </p>

                </div>
            );
        } else {
            return (
                <div>
                    <br/>
                    <h4>Choose before Pressing the Upload button</h4>
                </div>
            );
        }
    };

    render() {

        return (
            <div>
                <div>
                    <input type="file" accept=".csv" onChange={this.onFileChange}/>
                    <button onClick={this.onFileUpload}>
                        Upload File
                    </button>
                </div>
                {this.fileData()}
            </div>
        );
    }
}


export default UploadFile;