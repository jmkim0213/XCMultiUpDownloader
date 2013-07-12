package net.xenix.lib.multiupload.data;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import net.xenix.lib.multidownload.exception.XCCancelException;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

public class XCProgressMultipartEntity extends MultipartEntity {
	private final XCProgressListener mListener;
	
	public XCProgressMultipartEntity(XCProgressListener listener) {
		mListener = listener;
	}
	
	
	public XCProgressMultipartEntity(HttpMultipartMode mode, XCProgressListener listener){
		super(mode);
		mListener = listener;
	}
	
	public XCProgressMultipartEntity(HttpMultipartMode mode, String boundary, Charset charset, XCProgressListener listener){
		super(mode, boundary, charset);
		mListener = listener;
	}
	

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		long contentsLength = getContentLength();
		
		super.writeTo(new XCCountingOutputStream(outstream, contentsLength, this.mListener));
	}
	
	public static class XCCountingOutputStream extends FilterOutputStream {
		private final XCProgressListener listener;
		private long transferred;
		private long contentLength;
 
		public XCCountingOutputStream(OutputStream outstream, long contentLength, XCProgressListener listener) {
			super(outstream);
			this.listener = listener;
			this.transferred = 0;
			this.contentLength = contentLength;
		}
		
		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			if ( Thread.currentThread().isInterrupted() ) {
				throw new XCCancelException("Thread Interrupt");
			}
			
			out.write(buffer, offset, length);
			this.transferred += length;
			this.listener.transferred(transferred, contentLength);
		}
	}
	
	public interface XCProgressListener {
		public abstract void transferred(long transferred, long contentLength);
	}
}
