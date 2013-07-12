package net.xenix.lib.multiupload;

import net.xenix.lib.multiupload.data.XCUploadData;

interface XCUploadDelegate {
	public void onStartUpload(XCUploadData uploadData);
	public void onUpdateProgress(XCUploadData uploadData,  long currentLength, long totalLength);
	public void onCompleteUpload(XCUploadData uploadData, String response);
	public void onCancelUpload(XCUploadData uploadData);
	public void onFailUpload(XCUploadData uploadData, Exception e);
}
