package net.xenix.lib.multidownload;

import net.xenix.lib.multidownload.data.XCDownloadData;

interface XCDownloadDelegate {
	public void onStartDownload(XCDownloadData downloadData);
	public void onUpdateProgress(XCDownloadData downloadData,  long currentLength, long totalLength);
	public void onCompleteDownload(XCDownloadData downloadData);
	public void onCancelDownload(XCDownloadData downloadData);
	public void onFailDownload(XCDownloadData downloadData, Exception e);
}
