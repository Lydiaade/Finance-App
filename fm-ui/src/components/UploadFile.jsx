import {Component} from 'react';
import {BACKEND_URL} from "../config";

class UploadFile extends Component {
    state = {}
    onFileChange = event => {
        this.setState({selectedFile: event.target.files[0]});
    };

    onFileUpload = () => {
        const formData = new FormData();

        formData.append("file", this.state.selectedFile);

        fetch(`${BACKEND_URL}/transactions/upload/csv`, {
            method: "POST",
            headers: {
                'Accept': 'application/json'
            },
            body: formData
        })
            .then((response) => console.log(response.status));
    };

    render() {
        return (
            <div>
                <form>
                    <fieldset>
                        <div className="mb-3">
                            <label htmlFor="formFile" className="form-label">Upload transactions csv file</label>
                            <input className="form-control" type="file" accept=".csv" id="formFile"
                                   onChange={this.onFileChange} required/>
                        </div>
                        <button onClick={this.onFileUpload} className="btn btn-primary">Upload</button>
                    </fieldset>
                </form>
            </div>
        );
    }
}


export default UploadFile;