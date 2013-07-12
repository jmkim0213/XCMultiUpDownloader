package net.xenix.multidownloader.sample.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.xenix.lib.multidownload.XCMultiDownloadDelegate;
import net.xenix.lib.multidownload.XCMultiDownloadManager;
import net.xenix.lib.multidownload.data.XCDownloadData;
import net.xenix.lib.multidownload.data.XCDownloadEnum.XCDownloadState;
import net.xenix.multidownloader.sample.row.DataRow;
import net.xenix.multidownloader.sample.row.DataRow.DataRowDelegate;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class DataAdapter extends BaseAdapter {
	private Context mContext;
	private List<XCDownloadData> mXCDownloadDataList;
	private HashMap<String, DataRow> mDataRowMap;
	private HashMap<String, Integer> mProgressMap;

	private HashMap<String, XCDownloadState> mStateMap;

	private XCMultiDownloadManager mXCMultiDownloadManager;

	public DataAdapter(Context context, ArrayList<XCDownloadData> downloadDatas) {
		mContext = context;
		mXCDownloadDataList = downloadDatas;
		
		mDataRowMap  = new HashMap<String, DataRow>();
		mProgressMap = new HashMap<String, Integer>();
		
		mStateMap = new HashMap<String, XCDownloadState>();
		mXCMultiDownloadManager = new XCMultiDownloadManager(mDownloadDelegate, 3);
		mXCMultiDownloadManager.start();
	}
	
	public void release() {
		mXCMultiDownloadManager.stop();	
	}
	
	@Override
	public int getCount() {
		return mXCDownloadDataList.size();
	}

	@Override
	public Object getItem(int position) {
		return mXCDownloadDataList.get(position);
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
		
		XCDownloadData downloadData = mXCDownloadDataList.get(position);
		String key = downloadData.getKey();
		
		mDataRowMap.put(key, row);
		row.setData(downloadData);
		
		
		refreshRowViewState(key);
		refreshRowViewProgress(key);
		return row;
	}
	
	private void refreshRowViewState(String key) {
		DataRow row =  mDataRowMap.get(key);
		XCDownloadData currentDownloadData = row.getCurrentDownloadData();
		if ( row == null || currentDownloadData == null || !currentDownloadData.getKey().equalsIgnoreCase(key) )
			return ;
		XCDownloadState state = getXCDownloadState(key);
		
		row.setTextBold(false);
		switch ( state ) {
		case START:
			row.setTextBold(true);
		case READY:
			row.setButtonText("취소");
			break;
			
		case COMPLETE:
			row.setButtonText("완료/재다운");
			break;
		
		case CANCEL:
			row.setButtonText("다운");
			break;
			
		case NONE:
		case FAIL:
			row.setButtonText("다운");
			break;
		}
	}
	
	private void refreshRowViewProgress(String key) {
		DataRow row =  mDataRowMap.get(key);
		XCDownloadData currentXCDownloadData = row.getCurrentDownloadData();
		if ( row == null || currentXCDownloadData == null || !currentXCDownloadData.getKey().equalsIgnoreCase(key) )
			return ;
	
		Integer progress = mProgressMap.get(key);
		if ( progress != null ) {
			row.setUpgradeProgress(progress);
		}
		else  {
			row.setUpgradeProgress(0);
		}
	}
	
	private XCDownloadState getXCDownloadState(String key) {
		XCDownloadState state = mStateMap.get(key);
		if ( state == null ) {
			state = XCDownloadState.NONE;
		}
		return state;
	}
	
	private DataRowDelegate mDataRowDelegate = new DataRowDelegate() {
		
		@Override
		public void onClickButton(XCDownloadData downloadData) {
			XCDownloadState state = getXCDownloadState(downloadData.getKey());
			switch ( state ) {
			// 다운
			case NONE:
			case CANCEL:
			case FAIL:
				mXCMultiDownloadManager.add(downloadData);
				
				break;
				
			// 재 다운
			case COMPLETE:
				mXCMultiDownloadManager.add(downloadData);
				break;
				
			// 취소
			case START:
			case READY:
				mXCMultiDownloadManager.cancel(downloadData.getKey());
				break;
			}
		}
	};
	
	private XCMultiDownloadDelegate mDownloadDelegate = new XCMultiDownloadDelegate() {
		
		@Override
		public void onPreExecuteInBackground(XCDownloadData downloadData) {
			
		}
		
		@Override
		public void onUpdateProgress(String key, long currentLength, long totalLength) {
		    float progress = (float)currentLength/totalLength;
			int progressInt = (int)(progress * 100.0F);
			mProgressMap.put(key, progressInt);
			refreshRowViewProgress(key);
		}
		
		@Override
		public void onFailDownload(String key, Exception e) {
			
		}
		
		@Override
		public void onChangeState(String key, XCDownloadState state) {
			mStateMap.put(key, state);
			refreshRowViewState(key);
		}
	};

}
