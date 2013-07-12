package net.xenix.lib.multidownload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import net.xenix.lib.multidownload.data.XCDownloadData;
import net.xenix.lib.multidownload.data.XCDownloadEnum.XCDownloadState;

public class XCMultiDownloadManager {
	private XCMultiDownloadDelegate mDelegate;
	private LinkedBlockingQueue<XCDownloadData> mDownloadQueue;
	private Set<XCDownloader> mDownloaderSet;
	private int mDownloaderCnt;

	public XCMultiDownloadManager(XCMultiDownloadDelegate delegate, int downloaderCnt) {
		mDelegate = delegate;
		mDownloadQueue = new LinkedBlockingQueue<XCDownloadData>();
		mDownloaderSet = new HashSet<XCDownloader>(downloaderCnt);
		mDownloaderCnt = downloaderCnt;
	}
	
	public void start() {
		if ( !mDownloaderSet.isEmpty() ) {
			stop();
		}
		
		for ( int i = 0; i < mDownloaderCnt; i++ ) {
			XCDownloader downloader = new XCDownloader(mDownloadDelegate, mDownloadQueue);
			mDownloaderSet.add(downloader);
			downloader.start();
		}
	}
	
	public void stop() {
		for ( XCDownloader downloader : mDownloaderSet ) {
			downloader.stopLoop();
		}
		mDownloaderSet.clear();
	}
	
	public void add(XCDownloadData downloadData) {
		synchronized ( mDownloadQueue ) {
			mDownloadQueue.add(downloadData);
		}
		onChangeState(downloadData.getKey(), XCDownloadState.READY);
	}
	
	public void addAll(XCDownloadData[] downloadDatas) {
		for ( int i = 0; i < downloadDatas.length; i++ ) {
			add(downloadDatas[i]);	
		}
	}
	
	public void addAll(List<XCDownloadData> downloadDatas) {
		XCDownloadData[] downloadDataArray = downloadDatas.toArray(new XCDownloadData[0]);
		addAll(downloadDataArray);
	}
	
	public void cancel(String key) {
		XCDownloadData keyData = new XCDownloadData(key, null, null);
		synchronized ( mDownloadQueue ) {
			if ( mDownloadQueue.contains(keyData) ) {
				mDownloadQueue.remove(keyData);
				onChangeState(key, XCDownloadState.CANCEL);
			}
			else {
				cancelInDownloader(key);
			}
		}
	}
	
	public void cancelAll(String[] keys) {
		for ( int i = 0; i < keys.length; i++ ) {
			cancel(keys[i]);
		}
	}
	
	public void cancelAll(List<String> keys) {
		String[] keyArray = keys.toArray(new String[0]);
		cancelAll(keyArray);
	}
	
	public int size() {
		synchronized (mDownloadQueue) {
			return mDownloadQueue.size();
		}
	}
	
	private void cancelInDownloader(String key) {
		for ( XCDownloader downloader : mDownloaderSet ) {
			downloader.cancelDownload(key);
		}
	}
	
	// Private - Delegate
	private void onPreExecuteInBackground(XCDownloadData downloadData) {
		if ( mDelegate != null ) {
			mDelegate.onPreExecuteInBackground(downloadData);
		}
	} 			

	private void onChangeState(String key, XCDownloadState state) {
		if ( mDelegate != null ) {
			mDelegate.onChangeState(key, state);
		}
	}
	
	
	private void onUpdateProgress(String key, long currentLength, long totalLength) {
		if ( mDelegate != null ) {
			mDelegate.onUpdateProgress(key, currentLength, totalLength);
		}
	}
	
	private void onFailDownload(String key, Exception e) {
		if ( mDelegate != null ) {
			mDelegate.onChangeState(key, XCDownloadState.FAIL);
			mDelegate.onFailDownload(key, e);
		}
	}
	
	// InnerClass
	private XCDownloadDelegate mDownloadDelegate = new XCDownloadDelegate() {
		@Override
		public void onStartDownload(final XCDownloadData downloadData) {
			XCMultiDownloadManager.this.onPreExecuteInBackground(downloadData);
			XCMultiDownloadManager.this.onChangeState(downloadData.getKey(), XCDownloadState.START);
		}
		
		@Override
		public void onUpdateProgress(final XCDownloadData downloadData, final long currentLength, final long totalLength) {
			XCMultiDownloadManager.this.onUpdateProgress(downloadData.getKey(), currentLength, totalLength);
		}
		
		@Override
		public void onCompleteDownload(final XCDownloadData downloadData) {
			XCMultiDownloadManager.this.onChangeState(downloadData.getKey(), XCDownloadState.COMPLETE);
		}

		@Override
		public void onCancelDownload(final XCDownloadData downloadData) {
			XCMultiDownloadManager.this.onChangeState(downloadData.getKey(), XCDownloadState.CANCEL);
		}
		
		@Override
		public void onFailDownload(final XCDownloadData downloadData, final Exception e) {
			XCMultiDownloadManager.this.onFailDownload(downloadData.getKey(), e);
		}
	};
}
