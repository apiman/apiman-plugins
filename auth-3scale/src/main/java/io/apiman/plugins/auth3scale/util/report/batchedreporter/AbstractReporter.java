package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import io.apiman.gateway.engine.async.IAsyncHandler;

public abstract class AbstractReporter<T extends ReportToSend> {
	private IAsyncHandler<Void> fullHandler;
	
	public AbstractReporter() {
	}
	
	//ring buffer?
	//remember multiple different plugin will be using this same infrastructure - need to be careful
	public abstract ReportToSend encode();
	public abstract AbstractReporter<T> addRecord(T record);
	
	protected AbstractReporter<T> setFullHandler(IAsyncHandler<Void> fullHandler) {
		this.fullHandler = fullHandler;
		return this;
	}
	
	protected void full() {
		fullHandler.handle((Void) null);
	}
}
