package net.xenix.multiuploader.sample;

import java.util.ArrayList;

import net.xenix.lib.multiupload.data.XCUploadData;
import net.xenix.multiuploader.sample.adapter.DataAdapter;
import net.xenix.multiuploader.sample.manager.TakePictureManager;
import net.xenix.multiuploader.sample.manager.TakePictureManager.TakePictureManagerDelegate;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;

public class MainActivity extends Activity {
	private ArrayList<XCUploadData> mUploadDatas;
	private TakePictureManager mTakePictureManager;
	private DataAdapter mDataAdapter;
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTakePictureManager = new TakePictureManager(this);
		mTakePictureManager.setDelegate(mPictureManagerDelegate);
		
		mUploadDatas = new ArrayList<XCUploadData>(10);
		mDataAdapter = new DataAdapter(this, mUploadDatas);
		findViewById(R.id.activity_main_Button_addFile).setOnClickListener(mClickListener);
		
		ListView listView = (ListView)findViewById(R.id.activity_main_ListView);
		listView.setAdapter(mDataAdapter);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mTakePictureManager.onActivityResult(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	private OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mTakePictureManager.takePickFromAlbum();
		}
	};
	
	private TakePictureManagerDelegate mPictureManagerDelegate = new TakePictureManagerDelegate() {
		
		@Override
		public void onTakePickture(Uri imageUri) {
			String filePath = imageUri.getPath();
			
			XCUploadData uploadData = new XCUploadData(filePath, "http://admin.xenix.net/service/file_upload", filePath, "upload_file");
			mUploadDatas.add(uploadData);
			mDataAdapter.notifyDataSetChanged();
		}
		
		@Override
		public void onDeletePickture() {
			
		}
	};

}
