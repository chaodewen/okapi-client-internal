package okapi.client;

import net.sf.json.JSONObject;
import okapi.gen.InvokeService;
import okapi.util.Tools;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class ServiceClient  {
	public static void main(String[] args) {
		System.out.println("UserClassPath:" + System.getProperty("java.class.path"));
		System.out.println("SystemClassPath:" + System.getProperty("sun.boot.class.path"));
		try {
//			Response rsp = invokeAPIClient("localhost", "12345", "/Train/T112", "get", null, null, null).get();
//			System.out.println(rsp.getJSONObject().toString());
			Response rsp = invokeAPIClient("localhost", "12345", "/Train/T112", "get", null, null, null).get();
			System.out.println(rsp.getBodyByJSONObject());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static InvokeFuture invokeAPI(String api_path) {
		return invokeAPI(api_path, "GET", null, null, null);
	}
	public static InvokeFuture invokeAPI(String api_path, Map<String, String> arg) {
		return invokeAPI(api_path, "GET", arg, null, null);
	}
	public static InvokeFuture invokeAPI(String api_path, String method) {
		return invokeAPI(api_path, method, null, null, null);
	}
	public static InvokeFuture invokeAPI(String api_path, String method, Object body) {
		return invokeAPI(api_path, method, null, null, body);
	}
	public static InvokeFuture invokeAPI(String api_path, String method,  Map<String, String> arg, Object body) {
		return invokeAPI(api_path, method, arg, null, body);
	}
	public static InvokeFuture invokeAPI(String api_path, 
			String method, Map<String, String> arg, Map<String, String> headers, Object body) {
		ByteBuffer bb = Tools.transToByteBuffer(body);
//		if(body  instanceof JSONObject || body instanceof JSONArray){
//			if(headers == null){
//				headers = new HashMap<String, String>();
//			}
//			headers.put("Content-Type", "application/json");
//		}
		
		if(api_path.matches(ClientGlobalSettings.getRegAPIPath())) {
			String[] uriPart = ClientOperator.separateURI(api_path);
			if(uriPart.length >= 3) {
				String userName = uriPart[0];
				String serviceName = uriPart[1];
				String versionCode = uriPart[2];
				String api_path_last = "";
				for(int i = 3; i < uriPart.length; i++) {
					api_path_last += "/" + uriPart[i];
				}
				if(userName.equals("okapi") && serviceName.equals("services") && versionCode.equals("1")) {
					try {
//						System.out.println("api_path:"+api_path);
						return invokeAPIClient(ClientGlobalSettings.getInvokeAPIHost(), 
								ClientGlobalSettings.getInvokeAPIPort(), api_path_last, method, arg, headers, bb);
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Unknown Error!");
						Response resp = new Response(400, e.toString());
						return new ThriftInvokeFuture(resp);
					}
				}
				else {
					// 应该填充非系统API的调用方法
					String id = userName + "." + serviceName + "." + versionCode;
					try {
						System.out.println("获取API host, port:" + id);
						Response rsp = invokeAPI("/okapi/services/1/" + id + "/deploy/select").get();
						if(rsp.getCode() == 200) {
							JSONObject jo = JSONObject.fromObject(rsp.getBody());
							return invokeAPIClient(jo.getString("host"), String.valueOf(jo.getInt("port")), 
									api_path_last, method, arg, headers, bb);
						}
						else {
							System.out.println("invokeAPI:服务信息查询失败！");
							Response resp = new Response(400, "invokeAPI:服务信息查询失败！");
							return new ThriftInvokeFuture(resp);
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("invokeAPI:Unknown Error!");
						Response resp = new Response(400, e);
						return new ThriftInvokeFuture(resp);
					}
				}
			}
			System.out.println("invokeAPI:URI Length Error!");
			Response resp = new Response(400, "invokeAPI:URI Length Error!");
			return new ThriftInvokeFuture(resp);
		}
		else if(api_path.matches(ClientGlobalSettings.getRegHTTPURL())) {
			// HTTP请求
			InvokeFuture future = Tools.forwardHttp(method, api_path, arg, headers, body);
			return future;			
		}
		else {
			System.out.println("invokeAPI:URI:"+api_path);
			System.out.println("invokeAPI:URI Syntax Error!");
			Response resp = new Response(400, "invokeAPI:URI Syntax Error!");
			return new ThriftInvokeFuture(resp);
		}
	}
	private static InvokeFuture invokeAPIClient(String host, String port, String api_path, 
			String method, Map<String, String> arg, Map<String, String> headers, ByteBuffer body) throws Exception {
		try {
			TAsyncClientManager clientManager = new TAsyncClientManager();
			TNonblockingTransport transport = new TNonblockingSocket(host, Integer.valueOf(port), 
					ClientGlobalSettings.getClientTimeout());
			TProtocolFactory protocol = new TBinaryProtocol.Factory();
			InvokeService.AsyncClient asyncClient = new InvokeService.AsyncClient(protocol, 
					clientManager, transport);
			System.out.println("Client calls ... host:" + host + ", port:" + port + 
					", api_path:" + api_path + ", mehod:" + method);
			InvokeAPICallback callBack = new InvokeAPICallback(transport);
			asyncClient.InvokeAPI(api_path, method, arg, headers, body, callBack);
			return callBack.getOutput();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("invokeAPIClient:Unknown Error!");
			Response resp = new Response(400, e);
			return new ThriftInvokeFuture(resp);
		}
	}
}