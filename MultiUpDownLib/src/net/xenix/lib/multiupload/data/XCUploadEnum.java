package net.xenix.lib.multiupload.data;

public class XCUploadEnum {
	public static enum XCUploadState {
		NONE,
		READY,
		START,
		CANCEL,
		COMPLETE,
		FAIL
	}

	public static enum XCUploadMethod {
		GET,
		POST,
		PUT
	}
}
