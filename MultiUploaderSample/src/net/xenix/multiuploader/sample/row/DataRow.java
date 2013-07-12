package net.xenix.multiuploader.sample.row;

import net.xenix.lib.multiupload.data.XCUploadData;
import net.xenix.multiuploader.sample.R;
import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DataRow extends FrameLayout {
	private TextView mTextView;
	private ProgressBar mProgressBar;
	private Button mButton;
	
	private DataRowDelegate mDelegate;
	private XCUploadData mUploadData;

	public DataRow(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DataRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public DataRow(Context context) {
		super(context);
		init(context);
	}
	
	public void setDelegate(DataRowDelegate delegate) {
		mDelegate = delegate;
	}
	
	private void init(Context context) {
		View view = View.inflate(context, R.layout.row_data, null);
	
		mTextView = (TextView)view.findViewById(R.id.row_data_TextView);
		mProgressBar = (ProgressBar)view.findViewById(R.id.row_data_ProgressBar);
		mButton = (Button)view.findViewById(R.id.row_data_Button);
		mButton.setOnClickListener(mClickListener);
		
		addView(view);
	}
	
	public XCUploadData getCurrentUploadData() {
		return mUploadData;
	}
	
	public void setData(XCUploadData data) {
		mUploadData = data;
		mTextView.setText(data.getFilePath());
	}
	
	public void setUpgradeProgress(int progress) {
		mProgressBar.setProgress(progress);
	}
	
	public void setButtonText(String text) {
		mButton.setText(text);
	}
	
	public void setTextBold(boolean bold) {
		if ( bold ) {
			mTextView.setPaintFlags(mTextView.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
		}
		else {
			mTextView.setPaintFlags(mTextView.getPaintFlags() & ~Paint.FAKE_BOLD_TEXT_FLAG);
		}
	}
	
	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.row_data_Button:
				if ( mDelegate != null ) {
					mDelegate.onClickButton(mUploadData);
				}
				break;
			}
		}
	};
	
	public static interface DataRowDelegate {
		public void onClickButton(XCUploadData UploadData);
	}
}
