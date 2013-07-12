package net.xenix.multiuploader.sample.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.xenix.lib.multiupload.XCMultiUploadDelegate;
import net.xenix.lib.multiupload.XCMultiUploadManager;
import net.xenix.lib.multiupload.data.XCUploadData;
import net.xenix.lib.multiupload.data.XCUploadEnum.XCUploadState;
import net.xenix.multiuploader.sample.row.DataRow;
import net.xenix.multiuploader.sample.row.DataRow.DataRowDelegate;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class DataAdapter extends BaseAdapter {
	private Context mContext;
	private List<XCUploadData> mXCUploadDataList;
	private HashMap<String, DataRow> mDataRowMap;
	private HashMap<String, Integer> mProgressMap;

	private HashMap<String, XCUploadState> mStateMap;

	private XCMultiUploadManager mXCMultiUploadManager;

	public DataAdapter(Context context, ArrayList<XCUploadData> uploadDatas) {
		mContext = context;
		mXCUploadDataList = uploadDatas;
		mDataRowMap = new HashMap<String, DataRow>();
		mProgressMap = new HashMap<String, Integer>();
		mStateMap = new HashMap<String, XCUploadState>();
		mXCMultiUploadManager = new XCMultiUploadManager(mUploadDelegate, 3);
		mXCMultiUploadManager.start();
	}
	
	public void release() {
		mXCMultiUploadManager.stop();	
	}
	
	@Override
	public int getCount() {
		return mXCUploadDataList.size();
	}

	@Override
	public Object getItem(int position) {
		return mXCUploadDataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		DataRow row = (DataRow)convertView;
		if ( row == null ) {
			row = new DataRow(mContext);
			row.setDelegate(mDataRowDelegate);
		}
		
		XCUploadData XCUploadData = mXCUploadDataList.get(position);
		String key = XCUploadData.getKey();
		
		mDataRowMap.put(key, row);
		row.setData(XCUploadData);
		
		
		refreshRowViewState(key);
		refreshRowViewProgress(key);
		return row;
	}
	
	private void refreshRowViewState(String key) {
		DataRow row =  mDataRowMap.get(key);
		XCUploadData currentXCUploadData = row.getCurrentUploadData();
		if ( row == null || currentXCUploadData == null || !currentXCUploadData.getKey().equalsIgnoreCase(key) )
			return ;
		XCUploadState state = getXCUploadState(key);
		
		row.setTextBold(false);
		switch ( state ) {
		case START:
			row.setTextBold(true);
		case READY:
			row.setButtonText("취소");
			break;
			
		case COMPLETE:
			row.setButtonText("완료/업로드");
			break;
		
		case CANCEL:
			row.setButtonText("업로드");
			break;
			
		case NONE:
		case FAIL:
			row.setButtonText("업로드");
			break;
		}
	}
	
	private void refreshRowViewProgress(String key) {
		DataRow row =  mDataRowMap.get(key);
		XCUploadData currentXCUploadData = row.getCurrentUploadData();
		if ( row == null || currentXCUploadData == null || !currentXCUploadData.getKey().equalsIgnoreCase(key) )
			return ;
	
		Integer progress = mProgressMap.get(key);
		if ( progress != null ) {
			row.setUpgradeProgress(progress);
		}
		else  {
			row.setUpgradeProgress(0);
		}
	}
	
	private XCUploadState getXCUploadState(String key) {
		XCUploadState state = mStateMap.get(key);
		if ( state == null ) {
			state = XCUploadState.NONE;
		}
		return state;
	}
	
	private DataRowDelegate mDataRowDelegate = new DataRowDelegate() {
		
		@Override
		public void onClickButton(XCUploadData XCUploadData) {
			XCUploadState state = getXCUploadState(XCUploadData.getKey());
			switch ( state ) {
			// 업로드
			case NONE:
			case CANCEL:
			case FAIL:
				mXCMultiUploadManager.add(XCUploadData);
				
				break;
				
			// 재 업로드
			case COMPLETE:
				mXCMultiUploadManager.add(XCUploadData);
				break;
				
			// 취소
			case START:
			case READY:
				mXCMultiUploadManager.cancel(XCUploadData.getKey());
				break;
			}
		}
	};
	
	private XCMultiUploadDelegate mUploadDelegate = new XCMultiUploadDelegate() {
		
		@Override
		public void onPreExecuteInBackground(XCUploadData uploadData) {
			uploadData.addHeaderParam("User-Agent", "A");
		}
		
		@Override
		public void onUpdateProgress(String key, long currentLength, long totalLength) {
		    float progress = (float)currentLength/totalLength;
			int progressInt = (int)(progress * 100.0F);
			mProgressMap.put(key, progressInt);
			refreshRowViewProgress(key);
		}
		
		@Override
		public void onFailUpload(String key, Exception e) {
			
		}
		
		@Override
		public void onChangeState(String key, XCUploadState state) {
			mStateMap.put(key, state);
			refreshRowViewState(key);
		}

		@Override
		public void onCompleteUpload(String key, String response) {
			Log.e("TAG", "response: " + response);
		
		}
	};

}
