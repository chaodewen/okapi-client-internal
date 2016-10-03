package okapi.client;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TNonblockingTransport;

import okapi.gen.InvokeService.AsyncClient.InvokeAPI_call;

public class InvokeAPICallback implements AsyncMethodCallback<InvokeAPI_call> {
	private ThriftInvokeFuture io = new ThriftInvokeFuture();
	TNonblockingTransport transport;
	public InvokeAPICallback(TNonblockingTransport transport) {
		this.transport = transport;
	}
	// 返回结果
	@Override
	public void onComplete(InvokeAPI_call response) {
		try {
			okapi.gen.Response rsp = response.getResult();
			Response mrsp = new Response(rsp.getCode(), rsp.getHeaders(), rsp.getBody());
			io.offer(mrsp);
		} catch (TException e) {
			e.printStackTrace();
		} finally {
			this.transport.close();
		}
	}
	// 返回异常
	@Override
	public void onError(Exception exception) {
		exception.printStackTrace();
		io.offer(new Response(601, exception.toString()));
		this.transport.close();
		System.out.println("onError");
	}
	public InvokeFuture getOutput() {
		return io;
	}
}