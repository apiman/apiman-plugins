package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPeriodicComponent;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.plugins.auth3scale.util.report.ReportResponseHandler;

public class BatchedReporter {
	// Change list to RingBuffer?
	//private HashMap<AbstractReportGroup.ReportTypeEnum, ReportList> reports = new LinkedHashMap<>(); // TODO concurrency implications?
	private Set<AbstractReporter<? extends ReportData>> reporters = new LinkedHashSet<>();
	
	private IPeriodicComponent periodic;
	private boolean started = false;
	private volatile boolean sending = false;

	private long timerId;
	
	private IHttpClientComponent httpClient;
	
	private static final int DEFAULT_REPORTING_INTERVAL = 5000;
	private static final int DEFAULT_INITIAL_WAIT = 5000;
	private int reportingInterval = DEFAULT_REPORTING_INTERVAL;
	
	Queue<ReportToSend> retryQueue = new ConcurrentLinkedQueue<>();
	
	public BatchedReporter() {
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void addReporter(AbstractReporter<? extends ReportData> reporter) {
		reporter.setFullHandler(isFull -> {
			send();
		});
		
		reporters.add(reporter);
	}
	
	public void start(IPeriodicComponent periodic, IHttpClientComponent httpClient) {
		if (started)
			throw new IllegalStateException("Already started");
		this.httpClient = httpClient;
		this.periodic = periodic;
		this.started = true;
		
		this.timerId = periodic.setPeriodicTimer(reportingInterval, DEFAULT_INITIAL_WAIT, id -> {
			send();
		});
	}
	
	private void send() {
		if (!sending) {
			sending = true;
			synchronized (this) {
				if (!sending) {
					doSend();
				}
			}
		}
	}
	
	// speed up / slow down (back-pressure mechanism?)
	private void doSend() {
		for (AbstractReporter<? extends ReportData> reporter : reporters) {
			ReportToSend sendIt = reporter.encode(); // doSend? also need to consider there may be too much left
			
			IHttpClientRequest post = httpClient.request(sendIt.getEndpoint().toString(), // TODO change to broken down components
					HttpMethod.POST, 
					new ReportResponseHandler(reportResult -> {
						// TODO IMPORTANT: invalidate any bad credentials!
						sending = false;
					}));
			
			post.addHeader("Content-Type", sendIt.getEncoding()); // TODO change to contentType
			System.out.println("Writing the following:" + sendIt.getData());
			post.write(sendIt.getData(), "UTF-8");
			post.end();
		}
	}
//	// TODO Depending on which platform this is run on, could this become thread unsafe?
}
