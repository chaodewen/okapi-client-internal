package okapi.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThriftInvokeFuture extends InvokeFuture {

	private BlockingQueue<Response> invokeAPIOutput = new ArrayBlockingQueue<Response>(1);

	public ThriftInvokeFuture() {
	}
	public ThriftInvokeFuture(Response rsp) {
		System.out.println("InvokeAPIOutput Constructure:" + this.invokeAPIOutput.offer(rsp));
	}
	
	public boolean offer(Response response) {
		return this.invokeAPIOutput.offer(PublicClientOperator.repackBody(response));
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
	     return !this.invokeAPIOutput.isEmpty();
	}

	@Override
	public Response get()  {
		try {
			return this.invokeAPIOutput.take();
		} catch (Exception e) {
			e.printStackTrace();
			return new Response(601, e.toString());
		}
	}

	@Override
	public Response get(long timeout, TimeUnit unit)  {
		try {
			Response rsp=  this.invokeAPIOutput.poll(timeout, unit);
			if(rsp == null){
				return new Response(601, "timed out");
			}else{
				return rsp;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response(601, e.toString());
		}
	}

}
