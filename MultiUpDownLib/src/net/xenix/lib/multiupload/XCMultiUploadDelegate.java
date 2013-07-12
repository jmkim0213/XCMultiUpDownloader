package net.xenix.lib.multiupload;

import net.xenix.lib.multiupload.data.XCUploadData;
import net.xenix.lib.multiupload.data.XCUploadEnum.XCUploadState;

public interface XCMultiUploadDelegate {
	public void onPreExecuteInBackground(XCUploadData data);
	public void onChangeState(String key, XCUploadState state);
	public void onCompleteUpload(String key, String response);
	public void onUpdateProgress(String key, long currentLength, long totalLength);
	public void onFailUpload(String key, Exception e);
}
