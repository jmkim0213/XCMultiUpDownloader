package net.xenix.lib.multiupload;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.xenix.lib.multidownload.exception.XCCancelException;
import net.xenix.lib.multiupload.data.XCProgressMultipartEntity;
import net.xenix.lib.multiupload.data.XCProgressMultipartEntity.XCProgressListener;
import net.xenix.lib.multiupload.data.XCUploadData;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.util.Log;

class XCUploader extends Thread {
	
	private BlockingQueue<XCUploadData> mUploadQueue;
	private XCUploadData mCurrentUploadData;
	private XCUploadDelegate mDelegate;
	private boolean mStopLoop;
	private boolean mUploadCancel;
	
	public XCUploader(XCUploadDelegate delegate, BlockingQueue<XCUploadData> uploadData) {
		mDelegate = delegate;
		mUploadQueue = uploadData;
	}
	
	public void cancelUpload(String key) {
		if ( mCurrentUploadData != null ) {
			mUploadCancel = mCurrentUploadData.equals(key);
			this.interrupt();
		}
	}
	
	public void stopLoop() {
		this.interrupt();
		mUploadCancel = true;
		mStopLoop = true;
	}
	
	@SuppressLint("NewApi")
	@Override
	public void run() {
		while ( !mStopLoop ) {
			HttpClient httpClient = null;
			XCUploadData uploadData = null;
			XCProgressMultipartEntity multipartContent = null;
			HttpEntity httpEntity = null;

			try {
				uploadData = mUploadQueue.take();

				checkCancelAndStop();
				
				mCurrentUploadData = uploadData;
				onStartUpload(uploadData);
				checkCancelAndStop();
				
				httpClient = getNewHttpClient();

				HttpPost post = new HttpPost(uploadData.getUrl());
				post.setHeader("Connection", "keep-alive");
				post.setHeader("User-Agent", "ANDROID");

				Iterator<Entry<String, String>> headerParamIt = uploadData.headerParamsIterator();
				while ( headerParamIt.hasNext() ) {
					Entry<String, String> paramEntry = headerParamIt.next();
					post.setHeader(paramEntry.getKey(), paramEntry.getValue());
				}
				

				multipartContent = new XCProgressMultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, null, new XCProgressListener() {
					
					@Override
					public void transferred(long transferred, long contentLength) {
						onUpdateProgress(mCurrentUploadData, transferred, contentLength);
					}
				});
				
				// 파라메터 세팅
				Iterator<Entry<String, String>> paramIt = uploadData.paramsIterator();
				while ( paramIt.hasNext() ) {
					Entry<String, String> paramEntry = paramIt.next();
					multipartContent.addPart(paramEntry.getKey(), new StringBody(paramEntry.getValue()));
				}
				
				// 파일 사이즈
				
				File file = new File(uploadData.getFilePath());

				// 파일 첨부
				
				FileBody cbFile = new FileBody(file);
				
				multipartContent.addPart(uploadData.getFileParamName(), cbFile);

				// HttpPost Entity에 연결
				post.setEntity(multipartContent);
				

				// 10초 응답시간 타임아웃 설정
				HttpParams params = httpClient.getParams();
				params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
				HttpConnectionParams.setConnectionTimeout(params, 10000);
				HttpConnectionParams.setSoTimeout(params, 10000);

				// org.apache.http.client.httpClient 로 실행
				checkCancelAndStop();
				
				HttpResponse response = httpClient.execute(post);
				int statusCode = response.getStatusLine().getStatusCode();

				checkCancelAndStop();
				
				if ( statusCode == HttpStatus.SC_OK ) {
					httpEntity = response.getEntity();				
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(httpEntity);
					
					String responseStr = EntityUtils.toString(bufHttpEntity, HTTP.UTF_8);
					onCompleteUpload(mCurrentUploadData, responseStr);
				}
				    
				else if ( (statusCode == HttpStatus.SC_NO_CONTENT) || (statusCode == HttpStatus.SC_CREATED) ) {
					onCompleteUpload(mCurrentUploadData, "");
				}
				else {
					throw new Exception("Status Code is " + statusCode);
				}
				
			} 
			catch (XCCancelException e) {
				Log.e("TAG", e.toString());
				onCancelUpload(uploadData);
			} catch (IOException e) {
				Log.e("TAG", e.toString());
				onFailUpload(uploadData, e);
				
			} catch (InterruptedException e) {
				Log.e("TAG", e.toString());

				if ( uploadData != null ){
					if ( mUploadCancel ) {
						onCancelUpload(uploadData);
					}
					else {
						onFailUpload(uploadData, e);
					}
				}
				
			}  catch (Exception e) {
				Log.e("TAG", e.toString());
				if ( uploadData != null ){
					onFailUpload(uploadData, e);
				}
			} finally {
				if ( httpClient != null ) {
					httpClient.getConnectionManager().shutdown();
				}

				if ( multipartContent != null ) {
					try {
						multipartContent.consumeContent();
					} catch (Exception e) { }
				}
				
				if ( httpEntity != null ) {
					try {
						httpEntity.consumeContent();
					} catch (IOException e) { }
				}
				
				
				mCurrentUploadData = null;
				mUploadCancel = false;
			} 
		}
	}
	
	private void checkCancelAndStop() throws XCCancelException, InterruptedException {
		if ( mUploadCancel ) {
			throw new XCCancelException();
		}
		else if ( mStopLoop ) {
			throw new InterruptedException();
		}
	}
	
	private void onStartUpload(XCUploadData uploadData) {
		if ( mDelegate != null ) {
			mDelegate.onStartUpload(uploadData);
		}
	}
	
	
	private void onUpdateProgress(XCUploadData uploadData,  long currentLength, long totalLength) {
		if ( mDelegate != null ) {
			mDelegate.onUpdateProgress(uploadData, currentLength, totalLength);
		}
	}
	
	private void onCompleteUpload(XCUploadData uploadData, String response) {
		if ( mDelegate != null ) {
			mDelegate.onCompleteUpload(uploadData, response);
		}
	}
	
	private void onFailUpload(XCUploadData uploadData, Exception e) {
		
		if ( mDelegate != null ) {
			mDelegate.onFailUpload(uploadData, e);
		}
	}
	
	private void onCancelUpload(XCUploadData uploadData) {
		if ( mDelegate != null ) {
			mDelegate.onCancelUpload(uploadData);
		}
	}
	
	
	

	public HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            
            trustStore.load(null, null);
            SSLSocketFactory sf = new XCSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpConnectionParams.setConnectionTimeout(params, 15 * 1000); 
			HttpConnectionParams.setSoTimeout(params, 15 * 1000);
			
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));
            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
            
            return new DefaultHttpClient(ccm, params);
        }
        catch (Exception e)  {
            return new DefaultHttpClient();
        }
    }

    public class XCSSLSocketFactory extends SSLSocketFactory {
       private SSLContext sslContext = SSLContext.getInstance("TLS");

        public XCSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);
            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() { 
                	return null; 
                }
            };
            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        
        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host,port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }
}
