package net.xenix.lib.multiupload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import net.xenix.lib.multiupload.data.XCUploadData;
import net.xenix.lib.multiupload.data.XCUploadEnum.XCUploadState;

public class XCMultiUploadManager {
	private XCMultiUploadDelegate mDelegate;
	private LinkedBlockingQueue<XCUploadData> mUploadQueue;
	private Set<XCUploader> mUploaderSet;
	private int mUploadCnt;

	public XCMultiUploadManager(XCMultiUploadDelegate delegate, int downloaderCnt) {
		mDelegate = delegate;
		mUploadQueue = new LinkedBlockingQueue<XCUploadData>();
		mUploaderSet = new HashSet<XCUploader>(downloaderCnt);
		mUploadCnt = downloaderCnt;
	}	
	
	public void start() {
		if ( !mUploaderSet.isEmpty() ) {
			stop();
		}
		
		for ( int i = 0; i < mUploadCnt; i++ ) {
			XCUploader downloader = new XCUploader(mUploadDelegate, mUploadQueue);
			mUploaderSet.add(downloader);
			downloader.start();
		}
	}
	
	public void stop() {
		for ( XCUploader downloader : mUploaderSet ) {
			downloader.stopLoop();
		}
		mUploaderSet.clear();
	}
	
	public void add(XCUploadData uploadData) {
		synchronized ( mUploadQueue ) {
			mUploadQueue.add(uploadData);
		}
		onChangeState(uploadData.getKey(), XCUploadState.READY);
	}
	
	public void addAll(XCUploadData[] uploadDatas) {
		for ( int i = 0; i < uploadDatas.length; i++ ) {
			add(uploadDatas[i]);	
		}
	}
	
	public void addAll(List<XCUploadData> uploadDatas) {
		XCUploadData[] uploadDataArray = uploadDatas.toArray(new XCUploadData[0]);
		addAll(uploadDataArray);
	}
	
	public void cancel(String key) {
		XCUploadData keyData = new XCUploadData(key, null, null, null);
		synchronized ( mUploadQueue ) {
			if ( mUploadQueue.contains(keyData) ) {
				mUploadQueue.remove(keyData);
				onChangeState(key, XCUploadState.CANCEL);
			}
			else {
				cancelInUploader(key);
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
		synchronized (mUploadQueue) {
			return mUploadQueue.size();
		}
	}
	
	private void cancelInUploader(String key) {
		for ( XCUploader downloader : mUploaderSet ) {
			downloader.cancelUpload(key);
		}
	}
	
	// Private - Delegate
	private void onPreExecuteInBackground(XCUploadData uploadData) {
		if ( mDelegate != null ) {
			mDelegate.onPreExecuteInBackground(uploadData);
		}
	}
	
	private void onChangeState(String key, XCUploadState state) {
		if ( mDelegate != null ) {
			mDelegate.onChangeState(key, state);
		}
	}
	
	private void onCompleteUpload(String key, String response) {
		if ( mDelegate != null ) {
			mDelegate.onCompleteUpload(key, response);
		}
	}
	
	
	private void onUpdateProgress(String key, long currentLength, long totalLength) {
		if ( mDelegate != null ) {
			mDelegate.onUpdateProgress(key, currentLength, totalLength);
		}
	}
	
	private void onFailDownload(String key, Exception e) {
		if ( mDelegate != null ) {
			mDelegate.onChangeState(key, XCUploadState.FAIL);
			mDelegate.onFailUpload(key, e);
		}
	}
	
	// InnerClass
	private XCUploadDelegate mUploadDelegate = new XCUploadDelegate() {
		@Override
		public void onStartUpload(final XCUploadData uploadData) {
			XCMultiUploadManager.this.onPreExecuteInBackground(uploadData);
			
			XCMultiUploadManager.this.onChangeState(uploadData.getKey(), XCUploadState.START);
		}
		
		@Override
		public void onUpdateProgress(final XCUploadData uploadData, final long currentLength, final long totalLength) {
			XCMultiUploadManager.this.onUpdateProgress(uploadData.getKey(), currentLength, totalLength);
		}
		
		@Override
		public void onCompleteUpload(final XCUploadData uploadData, final String response) {
			XCMultiUploadManager.this.onCompleteUpload(uploadData.getKey(), response);
			XCMultiUploadManager.this.onChangeState(uploadData.getKey(), XCUploadState.COMPLETE);
		}

		@Override
		public void onCancelUpload(final XCUploadData uploadData) {
			XCMultiUploadManager.this.onChangeState(uploadData.getKey(), XCUploadState.CANCEL);
		}
		
		@Override
		public void onFailUpload(final XCUploadData uploadData, final Exception e) {
			XCMultiUploadManager.this.onFailDownload(uploadData.getKey(), e);
		}
	};
}
