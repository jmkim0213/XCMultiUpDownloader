package net.xenix.lib.multidownload.data;

public class XCDownloadEnum {
	public static enum XCDownloadState {
		NONE,
		READY,
		START,
		CANCEL,
		COMPLETE,
		FAIL
	}

	public static enum XCDownloadMethod {
		GET,
		POST,
		PUT
	}
}
