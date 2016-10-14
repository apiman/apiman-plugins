package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.LinkedHashSet;
import java.util.List;
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
		
		this.timerId = periodic.setPeriodicTimer(reportingInterval, DEFAULT_INITIAL_WAIT, id -> {
			System.out.println("tick! " + id + System.currentTimeMillis());
			send();
		});
		started = true;
	}
	
	public void stop() {
		periodic.cancelTimer(timerId);
	}
	
	// Avoid any double sending weirdness.
	private void send() {
		System.out.println("calling send " + itemsOfWork + " and sending is " + sending);
		if (!sending) {
			synchronized (this) {
				if (!sending) {
					sending = true;
					doSend();
				}
			}
		}
	}
	
	private volatile int itemsOfWork = 0;
	
	// speed up / slow down (primitive back-pressure mechanism?)
	private void doSend() {
		//System.out.println("calling doSend " + itemsOfWork);
		//new RuntimeException().printStackTrace();
		for (AbstractReporter<? extends ReportData> reporter : reporters) {
			List<ReportToSend> sendItList = reporter.encode(); // doSend? also need to consider there may be too much left

			for (ReportToSend sendIt : sendItList) {
				itemsOfWork++;
				System.out.println("Sending :" + itemsOfWork);

				IHttpClientRequest post = httpClient.request(sendIt.getEndpoint().toString(), // TODO change to broken down components
						HttpMethod.POST, 
						new ReportResponseHandler(reportResult -> {
							// TODO IMPORTANT: invalidate any bad credentials!
							//sending = false; //TODO wrong, wrong wrong!
							itemsOfWork--;
							System.out.println("Attempted to send report: Report was successful? " + reportResult.getResult().success() + " " + itemsOfWork );
							checkFinishedSending();
						}));
				post.addHeader("Content-Type", sendIt.getEncoding()); // TODO change to contentType
				System.out.println("Writing the following:" + sendIt.getData());
				post.write(sendIt.getData(), "UTF-8");
				post.end();
			}
		}
		checkFinishedSending();
	}
	
	// TODO Depending on which platform this is run on, could this become thread unsafe?
	// TODO Do we actually care if multiple send operations overlap?
	private void checkFinishedSending() {
		if (itemsOfWork<=0) {
			itemsOfWork=0;
			sending = false;
		}
	}
}
