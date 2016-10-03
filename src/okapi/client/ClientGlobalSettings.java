package okapi.client;

public class ClientGlobalSettings {
	private static String INVOKEAPIHOST = "okapi";
	private static String INVOKEAPIPORT = "23241";
	private static int CLIENTTIMEOUT = 30000;
	private static String REGAPIPATH = "^(/([^/]+))+$";
	private static String REGHTTPURL = "https?://[^\\s]+";
	private static String REGCALLINFO = "[\\w-?%&={}]+.[\\w-?%&={}]+.[\\w-?%&={}]+";
	public static String getInvokeAPIHost() {
		return INVOKEAPIHOST;
	}
	public static String getInvokeAPIPort() {
		return INVOKEAPIPORT;
	}
	public static int getClientTimeout() {
		return CLIENTTIMEOUT;
	}
	public static String getRegAPIPath() {
		return REGAPIPATH;
	}
	public static String getRegHTTPURL() {
		return REGHTTPURL;
	}
	public static String getRegCallInfo() {
		return REGCALLINFO;
	}
}