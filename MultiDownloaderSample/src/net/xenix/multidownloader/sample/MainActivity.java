package net.xenix.multidownloader.sample;

import java.util.ArrayList;

import net.xenix.lib.multidownload.data.XCDownloadData;
import net.xenix.multidownloader.sample.adapter.DataAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ListView;

public class MainActivity extends Activity {

	private final static int MIN_INDEX = 100;
	private final static int MAX_INDEX = 199;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		ArrayList<XCDownloadData> downloadDatas = new ArrayList<XCDownloadData>(); 
		for ( int i = MIN_INDEX; i < (MAX_INDEX + 1); i++ ) {
			String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MultiTest3/" + i + ".jpg";
//			String url = "http://admin.xenix.net/photo/Image_" + i + ".jpg";
			String url = "http://www.xenix.net/lgcloud/Android.zip";
		
			XCDownloadData downloadData = new XCDownloadData(String.valueOf(i), filePath, url);
			downloadDatas.add(downloadData);
		}
		
		ListView listView = (ListView)findViewById(R.id.activity_main_ListView);
		listView.setAdapter(new DataAdapter(this, downloadDatas));
	}
	
}
