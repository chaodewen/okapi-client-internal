package okapi.server;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;

import net.sf.json.JSONObject;
import okapi.client.ClientGlobalSettings;
import okapi.client.Service;
import okapi.client.ServiceClient;
import okapi.gen.*;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

public class Server {
	final public static String DEPLOY_PATH = "/home/okapi/bin/";
	final private static  String sys_meta = "/okapi/services/1/";
	final private static  String sys_store = "/okapi/storage/1/";
	
	private String service_id;
	private String service_id2;
	private JSONObject service;
	private String path;
	private  Class<?> module;

	public Server(String id) throws Exception {
		this.service_id = id.replace(".", "/");
		this.service_id2 = id.replace("/", ".");
		path = DEPLOY_PATH + this.service_id + "/";
		if (!this.service_id.matches(ClientGlobalSettings.getRegCallInfo())) {
			throw new Exception("Service ID is invalid");
		}
		okapi.client.Response rspCheck = ServiceClient.invokeAPI(sys_meta + this.service_id2).get();
		if (rspCheck.isOK()) {
			System.out.println(rspCheck);
			service = rspCheck.getBodyByJSONObject();
		} else {
			throw new Exception("Get Service Info Error: " + rspCheck.toString());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void start(int port) throws  TTransportException {
			System.out.println("Server Starting");
			TNonblockingServerSocket socket = new TNonblockingServerSocket(port);
			InvokeServiceHandler handler = new InvokeServiceHandler(module, service_id2);
			ServiceClient.invokeAPI(sys_meta + this.service_id2 + "/api", "post",  handler.getAPIRule());
			final InvokeService.Processor processor = new InvokeService.Processor(handler);
			THsHaServer.Args arg = new THsHaServer.Args(socket);
			// 高效率的、密集的二进制编码格式进行数据传输
			// 使用非阻塞方式，按块的大小进行传输，类似于 Java 中的 NIO
			arg.protocolFactory(new TBinaryProtocol.Factory());
			arg.transportFactory(new TFramedTransport.Factory());
			arg.processorFactory(new TProcessorFactory(processor));
			TServer server = new THsHaServer(arg);
			System.out.println("#服务启动-使用:非阻塞&高效二进制编码");

			JSONObject body = new JSONObject();
			body.put("status", "running");
			body.put("message", "running success");
			body.put("debug", "");
			ServiceClient.invokeAPI(sys_meta + this.service_id2 + "/deploy", "put",  body);
			
			server.serve();
	}
	
	private void inspect() throws Exception{
		File[] files = ServerOperator.getFiles(".jar", path);
		URL[] urls = new URL[files.length] ;
		for(int i = 0; i < files.length; i++) {
				urls[i] = files[i].toURI().toURL();
		}
		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		// Add the conf dir to the classpath
		// Chain the current thread classloader
		URLClassLoader urlClassLoader = new URLClassLoader(urls,  currentThreadClassLoader);
		// Replace the thread classloader - assumes
		// you have permissions to do so
		Thread.currentThread().setContextClassLoader(urlClassLoader);
		String module_name = service.getString("module_name");
		module = urlClassLoader.loadClass(module_name);
		if(!Service.class.isAssignableFrom(module)){
			throw new Exception("module_name " + module + " not inherite from okapi.client.Service");
		}
	}

	private void deploy() throws Exception {
		File deploy = new File(path + ".deploy");
		if (!deploy.exists()) {
			System.out.println("Directory Creation:" + (new File(path)).mkdirs());
			okapi.client.Response rspDownload = ServiceClient.invokeAPI(
					sys_store + "okapi/program/" + this.service_id2).get();
			if (rspDownload.getCode() == 200) {
				String downloadPath = "/tmp/";
				ServerOperator.writeByteBuffer(service.getString("name"), downloadPath,
						ByteBuffer.wrap((byte[]) rspDownload.getBody()));
				ServerOperator.unZip(downloadPath + service.getString("name"), path);
				deploy.createNewFile();
			} else {
				throw new Exception("Download File Error: " + rspDownload.toString());
			}
		}
	}

	public static void main(String args[]) {
		System.out.println("UserClassPath:" + System.getProperty("java.class.path"));
		System.out.println("SystemClassPath:" + System.getProperty("sun.boot.class.path"));
		if (args.length > 1) {
			Server srv = null;
			JSONObject body = new JSONObject();
			try {
				srv = new Server(args[0]);
				srv.deploy();
				srv.inspect();
				srv.start(Integer.parseInt(args[1]));
			} catch(UnsupportedClassVersionError ex) {
				ex.printStackTrace();
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));		
				body.put("status", "exited");
				body.put("message", "Java版本过高，请使用Java1.7编译API");
				body.put("debug", sw.toString());
			} catch (Exception ex) {
				ex.printStackTrace();
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));				
				body.put("status", "exited");
				body.put("message", ex.getMessage());
				body.put("debug", sw.toString());
			} finally {
				System.out.println("process exit");
				if(srv != null){
					okapi.client.Response r = ServiceClient.invokeAPI(sys_meta + srv.service_id2 + "/deploy", "put",  body).get();
					System.out.println(r.getCode());
				}
			}
		} else {
			System.out.println("Command Line Arguments Must Be More!");
		}
	}
}