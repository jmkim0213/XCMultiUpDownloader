package net.xenix.lib.multidownload;

import net.xenix.lib.multidownload.data.XCDownloadData;
import net.xenix.lib.multidownload.data.XCDownloadEnum.XCDownloadState;

public interface XCMultiDownloadDelegate {
	public void onPreExecuteInBackground(XCDownloadData downloadData);
	public void onChangeState(String key, XCDownloadState state);
	public void onUpdateProgress(String key, long currentLength, long totalLength);
	public void onFailDownload(String key, Exception e);
}
