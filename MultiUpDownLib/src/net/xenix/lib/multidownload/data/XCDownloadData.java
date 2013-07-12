package net.xenix.lib.multidownload.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.xenix.lib.multidownload.data.XCDownloadEnum.XCDownloadMethod;

public class XCDownloadData {
	private final String key;
	private String filePath;
	private String url;
	
	private XCDownloadMethod method;
	private HashMap<String, String> params;
	private HashMap<String, String> headerParams;
	
	
	private Object object;
	
	
	
	public XCDownloadData(String key, String filePath, String url, XCDownloadMethod method) {
		this.key = key;
		this.filePath = filePath;
		this.url = url;
		this.method = method;
		this.params = new HashMap<String, String>();
		this.headerParams = new HashMap<String, String>();
	}

	public XCDownloadData(String key, String filePath, String url) {
		this(key, filePath, url, XCDownloadMethod.GET);
	}
	
	
	public String getKey() {
		return key;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public XCDownloadMethod getMethod() {
		return method;
	}

	public void setMethod(XCDownloadMethod method) {
		this.method = method;
	}
	
	public void addParam(String key, String value) {
		this.params.put(key, value);
	}
	
	public void addHeaderParam(String key, String value) {
		this.headerParams.put(key, value);
	}
	
	public void setObject(Object object) {
		this.object = object;
	}
	
	public Object getObject() {
		return object;
	}
	
	public Iterator<Entry<String, String>> paramsIterator() {
		return this.params.entrySet().iterator();
	}
	
	
	public Iterator<Entry<String, String>> headerParamsIterator() {
		return this.headerParams.entrySet().iterator();
	}
	
	@Override
	public int hashCode() {
		return this.key.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		boolean retValue = false;
		
		if ( (o != null) && (o instanceof XCDownloadData) ) {
			retValue = this.key.equalsIgnoreCase(((XCDownloadData)o).key);
		}
		else {
			retValue = this.key.equals(o);
		}
		
		return retValue;
	}
}
