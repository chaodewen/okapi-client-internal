package okapi.server;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okapi.annotation.DELETE;
import okapi.annotation.GET;
import okapi.annotation.POST;
import okapi.annotation.PUT;
import okapi.client.Service;
import okapi.client.ServiceClient;
import okapi.gen.InvokeService;
import okapi.gen.Response;
import okapi.util.Tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ApiRule{
	private String action;
	private Method method;
	private Class<?> module;
	private String rule;
	public String getAction() {
		return action;
	}
	public Method getMethod() {
		return method;
	}
	public Class<?> getModule() {
		return module;
	}
	public String getRule(){
		return rule;
	}
	public ApiRule(String action, String rule, Method method, Class<?> module) {
		super();
		this.action = action;
		this.rule = rule;
		this.method = method;
		this.module = module;
	}
	public boolean isMatched(String method, String api_path){
		if(!method.equalsIgnoreCase(action)){
			return false;
		}
		return ServerOperator.isCompitable(rule, api_path);
	}
}
public class InvokeServiceHandler implements InvokeService.Iface {
	private List<ApiRule> api = new ArrayList<ApiRule>();
	private Class<?> module;
	private String service_id2;
	public InvokeServiceHandler(Class<?> module, String service_id2) {
		super();
		this.service_id2 = service_id2;
		this.module = module;
		for(Method m : module.getDeclaredMethods()) {
			if(m.getAnnotation(GET.class) != null){
				GET get = m.getAnnotation(GET.class);
				api.add(new ApiRule("GET", get.value(), m, module));
			}
			else if(m.getAnnotation(POST.class) != null) {
				POST post = m.getAnnotation(POST.class);
				api.add(new ApiRule("POST", post.value(), m, module));
			}
			else if(m.getAnnotation(PUT.class) != null) {
				PUT put = m.getAnnotation(PUT.class);
				api.add(new ApiRule("PUT", put.value(), m, module));
			}
			else if(m.getAnnotation(DELETE.class) != null) {
				DELETE delete = m.getAnnotation(DELETE.class);
				api.add(new ApiRule("DELETE", delete.value(), m, module));
			}
		}
	}
	
	public JSONArray getAPIRule(){
		JSONArray array = new JSONArray();
		for(ApiRule rule: api){
			JSONObject obj = new JSONObject();
			obj.put("rule", rule.getRule());
			JSONArray methods = new JSONArray();
			methods.add(rule.getAction());
			obj.put("methods", methods);
			obj.put("function", rule.getMethod().getName());
			array.add(obj);
		}
		return array;
	}
	
	public Response InvokeAPI(String api_path, String method, Map<String, String> arg, Map<String, String> headers, ByteBuffer body) {
			if(!api_path.startsWith("/")) {
				api_path = "/" + api_path;
			}
			System.out.println("Invoke Incoming, api_path: " + api_path);			
			Object obj = null;
			int code = 200;
			Map<String, String> h = new HashMap<String, String>();
			for(ApiRule  rule : api) {
				if(rule.isMatched(method, api_path)){
						Method m = rule.getMethod();
						System.out.println("执行函数"+m.getName());
						m.setAccessible(true);
						Object[] uriArg = ServerOperator.getArg(rule.getRule(), api_path);
						if(uriArg.length == m.getParameterTypes().length) {
							try {
								Service svc = (Service)(module.newInstance());
								svc.Args = arg;
								svc.Headers = headers;
								svc.Body = body;
								// 添加鉴权步骤
								if(arg.containsKey("token" )) {
									okapi.client.Response rsp = ServiceClient.invokeAPI(
											"/okapi/auth/1/permission/" + this.service_id2 + "/token/" + arg.get("token")).get();
									if(rsp.isOK()) {
										long result = Long.valueOf(String.valueOf(rsp.getBody()));
										if(result == 1) {
											System.out.println("InvokeAPI:Auth Check Passed.");
											obj = m.invoke(svc, uriArg);
										}
										else if(result == -1) {
											System.out.println("InvokeAPI:Token Wrong!");
											code = 401;
											obj = "InvokeAPI:Auth Check Passed.";
										}
										else if(result == -2){
											System.out.println("InvokeAPI:Token Not Found In Token Collection!");
											code = 401;
											obj = "InvokeAPI:Token Not Found In Token Collection!";
										}
										else {
											System.out.println("InvokeAPI:Unknown Error!");
											code = 400;
											obj = "InvokeAPI:Unknown Error!";
										}
									}
									else {
										System.out.println("InvokeAPI:Auth Request Error!");
										code = rsp.getCode();
										obj = "InvokeAPI:Auth Request Error!";
									}
								}
								else {
									code = 401;
									obj = "InvokeAPI:No Token Input!";
								}
							} catch (IllegalAccessException
									| IllegalArgumentException
									| InvocationTargetException
									| InstantiationException e) {
								e.printStackTrace();
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));
								code = 501;
								obj = e.getMessage() + sw.toString();
							}
						}
						else {
							code = 502;
							obj =  "URI参数数量错误！";
							for(int i = 0; i < uriArg.length; i++){
								obj += i + ":" + uriArg.getClass().getName()+ "," + uriArg.toString() + "|";
							}
							for(int i = 0; i < m.getParameterTypes().length; i ++){
								obj +=i + ":"  + m.getParameterTypes()[i].toString() + "|";
							}
						}
						
						if(obj instanceof okapi.client.Response) {
							okapi.client.Response cRsp = (okapi.client.Response) obj;
							code = cRsp.getCode();
							h = cRsp.getHeaders();
							obj = cRsp.getBody();
						}else if(obj instanceof JSONObject || obj instanceof JSONArray){
							h.put("Content-Type", "application/json");
						}else if(obj instanceof byte[] || obj instanceof ByteBuffer){
							h.put("Content-Type", "application/octet-stream");
						}else{
							h.put("Content-Type", "text/plain");
						}
						ByteBuffer bb = Tools.transToByteBuffer(obj);
						return new Response(code, h, bb);
				}
			}
			code = 404;
			obj = "cannot find matched rule";
			ByteBuffer bb = Tools.transToByteBuffer(obj);
			return new Response(code, h, bb);
	}
}