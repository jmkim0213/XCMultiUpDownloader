package net.xenix.multiuploader.sample.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.xenix.image.lib.ImageProcessing;
import net.xenix.multiuploader.sample.utils.FileUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;

public class TakePictureManager {
	private final static int PICK_FROM_CAMERA = 1004;
	private final static int PICK_FROM_ALBUM  = 1005;
	private final static int CROP_IMAGE = 1006;
	
	private TakePictureManagerDelegate mDelegate;
	private Activity mActivity;
	private Uri mImageCaptureUri;
	private boolean mIsCrop = true;
	private int mCropWidth, mCropHeight;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if ( mDelegate != null )
				mDelegate.onTakePickture(mImageCaptureUri);
		}
		
	};
	
	
	public TakePictureManager(Activity context) {
		mActivity = context;
		mCropWidth  = 100;
		mCropHeight = 100;
	}
	
	
	public void setDelegate(TakePictureManagerDelegate delegate) {
		mDelegate = delegate;
	}
	
	public void setCrop(boolean crop) {
		mIsCrop = crop;	
	}
	
	public void setCropSize(int width, int height) {
		mCropWidth  = width;
		mCropHeight = height;
	}
	
	public void showMenuDialog(boolean withDelete) {
		ArrayList<String> takeMenu = new ArrayList<String>(3);
		
		takeMenu.add("사진 찍기");
		takeMenu.add("앨범에서 사진선택");
		
		if ( withDelete )
			takeMenu.add("삭제");
		
		ArrayAdapter<String> takePickAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.select_dialog_item, takeMenu);
		
		new AlertDialog.Builder(mActivity)
		.setTitle("Photo")
		.setAdapter(takePickAdapter, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				switch ( which ) {
				case 0:		// 사진찍기
					takePickFromCamera();
					break;
					
				case 1:		// 사진가져오기
					takePickFromAlbum();
					break;
					
				case 2:
					if ( mDelegate != null )
						mDelegate.onDeletePickture();
					break;
				}
			}
		})
		.create()
		.show();
	}
	


	public void takePickFromCamera() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);		
        mImageCaptureUri = getImageFileUri();
        
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        
        mActivity.startActivityForResult(intent, PICK_FROM_CAMERA);	
	}
	
	public void takePickFromAlbum() {
		Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        mActivity.startActivityForResult(intent, PICK_FROM_ALBUM);
	}
	
	private Uri getImageFileUri() {
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + mActivity.getPackageName();        
        String name = System.currentTimeMillis() + ".jpg";
        File directory = new File(path);
        if ( !directory.exists() ) {
        	directory.mkdirs();
        }
        
        return Uri.fromFile(new File(directory, name));	
	}
	
	private File getImageFileFromUri(Uri imgUri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        if (imgUri == null) {
        	imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
 
        Cursor mCursor = mActivity.getContentResolver().query(imgUri, projection, null, null, MediaStore.Images.Media.DATE_MODIFIED + " desc");
        if(mCursor == null || mCursor.getCount() < 1) {
            return null; // no cursor or no record
        }
        
        int column_index = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        mCursor.moveToFirst();
 
        String path = mCursor.getString(column_index);
 
        if (mCursor != null ) {
            mCursor.close();
            mCursor = null;
        }
 
        return new File(path);
	}
	

	
	
	

	
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		// TODO Auto-generated method stub
		
		new Thread() {
			@Override
			public void run() {
				if ( resultCode != Activity.RESULT_OK ) 
					return ;
				
				switch ( requestCode ) {
				case PICK_FROM_ALBUM:
					Uri imgUri = data.getData();
					File srcFile = getImageFileFromUri(imgUri);
					
					mImageCaptureUri = getImageFileUri();
					File destFile = new File(mImageCaptureUri.getPath());
					
					try {
						FileUtils.FileCopy(srcFile, destFile);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
				case PICK_FROM_CAMERA:
					if ( mIsCrop ) {
			            Intent intent = new Intent("com.android.camera.action.CROP");
			            intent.setDataAndType(mImageCaptureUri, "image/*");
			
			            // Crop한 이미지를 저장할 Path
			            intent.putExtra("output", mImageCaptureUri);
			            intent.putExtra("outputX", mCropWidth);
			            intent.putExtra("outputY", mCropHeight);
			            
			            intent.putExtra("aspectX", 1);
			            intent.putExtra("aspectY", 1);
			            mActivity.startActivityForResult(intent, CROP_IMAGE);
					}
					else {
						
						try {
							String imgPath = mImageCaptureUri.getPath();
							ExifInterface exif = new ExifInterface(imgPath);
							int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
							int exifDegree = exifOrientationToDegrees(exifOrientation);
							
							
							
							Options options = new Options();
							options.inJustDecodeBounds = true;
							
							BitmapFactory.decodeFile(imgPath, options);
							
							if ( options.outWidth > 2000 || options.outHeight > 2000 ) {
								ImageProcessing.resizeImage(imgPath, imgPath, 1600, 1600);
							}
							
							
							if(exifDegree != 0) {
								ImageProcessing.rotateImage(imgPath, imgPath, exifDegree);
							}
							
							mHandler.sendEmptyMessage(-1);
							
	
						} catch (Exception e) {
							
						}
						
					}
					
					break;
					
				case CROP_IMAGE:
					mHandler.sendEmptyMessage(-1);
					
					break;
				}
			}
		}.start();
				
		
	}

	private int exifOrientationToDegrees(int exifOrientation) {
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) 
			return 90;
		
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
			return 180;
		
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
			return 270;
		
		return 0;
	}
	
	private Bitmap rotate(Bitmap bitmap, int degrees) throws OutOfMemoryError {
		if (degrees != 0 && bitmap != null) {
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);


			Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			if (bitmap != converted) {
				bitmap.recycle();
				bitmap = converted;
			}

		}
		return bitmap;
	}

	public interface TakePictureManagerDelegate {
		public void onTakePickture(Uri imageUri);
		public void onDeletePickture();
	}
	 
	 
}


