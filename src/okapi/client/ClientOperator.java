package okapi.client;

public class ClientOperator {
	public static String[] separateURI(String uri) {
		StringBuilder sb = new StringBuilder(uri);
		if(uri.startsWith("/")) {
			sb.deleteCharAt(0);
		}
		if(uri.endsWith("/")) {
			sb.deleteCharAt(uri.length() - 1);
		}
		return sb.toString().split("/");
	}
}