package net.xenix.lib.multidownload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.xenix.lib.multidownload.data.XCDownloadData;
import net.xenix.lib.multidownload.exception.XCNotEnoughStorageException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.annotation.SuppressLint;
import android.app.PendingIntent.CanceledException;
import android.os.Build;
import android.util.Log;

class XCDownloader extends Thread {
	private BlockingQueue<XCDownloadData> mDownloadQueue;
	private XCDownloadData mCurrentDownloadData;
	private XCDownloadDelegate mDelegate;
	private boolean mStopLoop;
	private boolean mDownloadCancel;
	
	public XCDownloader(XCDownloadDelegate delegate, BlockingQueue<XCDownloadData> downloadData) {
		mDelegate = delegate;
		mDownloadQueue = downloadData;
	}
	
	public void cancelDownload(String key) {
		if ( mCurrentDownloadData != null ) {
			mDownloadCancel = mCurrentDownloadData.equals(key);
		}
	}
	
	public void stopLoop() {
		this.interrupt();
		mDownloadCancel = true;
		mStopLoop = true;
	}
	
	@SuppressLint("NewApi")
	@Override
	public void run() {
		while ( !mStopLoop ) {
			
			HttpClient httpClient = null;
			HttpRequestBase httpRequest = null;
			HttpEntity entity = null;
			
			InputStream is = null;
			BufferedInputStream bis = null;
			RandomAccessFile raf = null;
			File saveFile = null;
			XCDownloadData downloadData = null;
			try {
				downloadData = mDownloadQueue.take();
				checkCancelAndStop();
				
				mCurrentDownloadData = downloadData;
				onStartDownload(downloadData);
				checkCancelAndStop();
				
				httpClient = getNewHttpClient();
	
				Iterator<Entry<String, String>> paramIt = downloadData.paramsIterator();
				String url = downloadData.getUrl();
				switch (downloadData.getMethod()) {
				case GET:
					HttpGet httpGet = new HttpGet(getURL(url, paramIt));
					httpRequest = httpGet;
					break;
					
				case POST:
					HttpPost httpPost = new HttpPost(url);
					httpPost.setEntity(getParamEntity(paramIt));	
					httpRequest = httpPost;
					break;
					
				case PUT:
					HttpPut httpPut = new HttpPut(url);
					httpRequest = httpPut;
					break;
				}
				httpRequest.setHeader("Connection", "Keep-Alive");
				
				Iterator<Entry<String, String>> headerParamIt = downloadData.headerParamsIterator();
				while ( headerParamIt.hasNext() ) {
					Entry<String, String> param = headerParamIt.next();
					String key = param.getKey();
					String value = param.getValue();
					httpRequest.setHeader(key, value);

				}
				
				saveFile = new File(downloadData.getFilePath());
				File dirFile = saveFile.getParentFile();
				if ( !dirFile.exists() ) {
					dirFile.mkdirs();
				} 
				else if ( !saveFile.exists() ) {
					saveFile.createNewFile();
				}
				else {
					httpRequest.addHeader("Range", "bytes=" + saveFile.length() + "-");
				}
				

				HttpResponse httpResponse = httpClient.execute(httpRequest);
				checkCancelAndStop();
				
				final int statusCode = httpResponse.getStatusLine().getStatusCode();
				
				// 실패 	
				
				if ( statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_PARTIAL_CONTENT ) {					
					if ( statusCode != HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE ) {
						throw new Exception(String.format("Status: %d", statusCode));
					}
					else {
						onCompleteDownload(downloadData);
						continue ;
					}
				}
				
				
				entity = httpResponse.getEntity();
				
				if ( entity != null ) {
					checkCancelAndStop();
					
				    long totalLength = entity.getContentLength();
					long currentLength = 0;
					
					if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO ) {
						long freeSpace = dirFile.getFreeSpace();
						Log.e("TAG", "Download Size: " + totalLength);
						Log.e("TAG", "Fress Size: " + freeSpace);
						
						if ( totalLength > freeSpace  ) {
							throw new XCNotEnoughStorageException();
						}
					}
					

				    is = entity.getContent();
				    
					bis = new BufferedInputStream(is);
					
					byte [] buffer = new byte[8 * 1024];
					int read = 0;
					
					long fileLength = saveFile.length();
					raf = new RandomAccessFile(saveFile, "rw");
					raf.seek(raf.length());
					currentLength += fileLength;
					totalLength += fileLength;
					
					while ( (read = bis.read(buffer)) >= 0 ) {
						checkCancelAndStop();
						
						raf.write(buffer, 0, read);
					    currentLength += read;
					    
					    onUpdateProgress(downloadData, currentLength, totalLength);

					    Thread.sleep(5);
					}
				}
				
				checkCancelAndStop();
				onCompleteDownload(downloadData);
				
			} catch (XCNotEnoughStorageException e ) {
				Log.e("TAG", e.toString());

				onFailDownload(downloadData, e);
				
			} catch (IOException e) {
				Log.e("TAG", e.toString());

				onFailDownload(downloadData, e);
				
			} catch (InterruptedException e) {
				Log.e("TAG", e.toString());

				if ( downloadData != null ) {
					if ( mDownloadCancel ) {
						onCancelDownload(downloadData);
					}
					else {
						onFailDownload(downloadData, e);
					}
				}
				
			} catch (CanceledException e) {
				Log.e("TAG", e.toString());

				onCancelDownload(downloadData);
				
			} catch (Exception e) {
				Log.e("TAG", e.toString());
				if ( downloadData != null ){
					onFailDownload(downloadData, e);
				}
			} finally {
				if ( httpClient != null ) {
					httpClient.getConnectionManager().shutdown();
				}
				
				if ( httpRequest != null ) {
					httpRequest.abort();	
				}
				
				if ( entity != null ) {
					try {
						entity.consumeContent();
					} catch (IOException e) { }
				}
				
				if ( is != null ) {
					try {
						is.close();
					} catch (IOException e) { }
				}
				
				if ( bis != null ) {
					try {
						bis.close();
					} catch (IOException e) { }
				}
				
				if ( raf != null ) {
					try {
//						raf.f
						raf.close();
					} catch (IOException e) { }
				}
				
				mCurrentDownloadData = null;
				mDownloadCancel = false;
			}
		}
	}
	
	private String getURL(String url, Iterator<Entry<String, String>> paramIterator) {
		StringBuilder urlBuilder = new StringBuilder(url);
	
		if ( url.endsWith("/") ) {
			urlBuilder.deleteCharAt(urlBuilder.length() - 1);
		}
		
		while ( paramIterator.hasNext() ) {
			Entry<String, String> param = paramIterator.next();
			String key = param.getKey();
			String value = param.getValue();
			
			String encodeValue = null;
			try {
				encodeValue = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				encodeValue = value;
			}
			if (!url.contains("?")) {
				urlBuilder.append("?").append(key).append("=").append(encodeValue);
			} 
			else {
				urlBuilder.append("&").append(key).append("=").append(encodeValue);
			}
		}
		
		return urlBuilder.toString();
	}
	
	private UrlEncodedFormEntity getParamEntity(Iterator<Entry<String, String>> paramIterator) {
		UrlEncodedFormEntity entity = null;
	
		ArrayList<NameValuePair> paramList = new ArrayList<NameValuePair>();

		while ( paramIterator.hasNext() ) {
			Entry<String, String> param = paramIterator.next();
			String key = param.getKey();
			String value = param.getValue();
			paramList.add(new BasicNameValuePair(key, value));
		}
		
		try {
			entity = new UrlEncodedFormEntity(paramList, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		
		return entity;
	}
	
	private void checkCancelAndStop() throws CanceledException, InterruptedException {
		if ( mDownloadCancel ) {
			throw new CanceledException();
		}
		else if ( mStopLoop ) {
			throw new InterruptedException();
		}

	}
	
	private void onStartDownload(XCDownloadData downloadData) {
		if ( mDelegate != null ) {
			mDelegate.onStartDownload(downloadData);
		}
	}
	
	
	private void onUpdateProgress(XCDownloadData downloadData, long currentLength, long totalLength) {
		if ( mDelegate != null ) {
			mDelegate.onUpdateProgress(downloadData, currentLength, totalLength);
		}
	}
	
	private void onCompleteDownload(XCDownloadData downloadData) {
		if ( mDelegate != null ) {
			mDelegate.onCompleteDownload(downloadData);
		}
	}
	
	private void onFailDownload(XCDownloadData downloadData, Exception e) {
		
		if ( mDelegate != null ) {
			mDelegate.onFailDownload(downloadData, e);
		}
	}
	
	private void onCancelDownload(XCDownloadData downloadData) {
		if ( mDelegate != null ) {
			mDelegate.onCancelDownload(downloadData);
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
