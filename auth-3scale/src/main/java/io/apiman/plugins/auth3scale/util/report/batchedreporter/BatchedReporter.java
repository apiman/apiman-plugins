package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.EvictingQueue;

import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IPeriodicComponent;
import io.apiman.gateway.engine.components.http.HttpMethod;
import io.apiman.gateway.engine.components.http.IHttpClientRequest;
import io.apiman.plugins.auth3scale.util.report.ReportResponseHandler;
import io.apiman.plugins.auth3scale.util.report.ReportResponseHandler.ReportResponse;

public class BatchedReporter {
    private static final int DEFAULT_REPORTING_INTERVAL = 5000;
    private static final int DEFAULT_INITIAL_WAIT = 5000;    
    private static final int DEFAULT_RETRY_QUEUE_MAXSIZE = 10000;
    private int reportingInterval = DEFAULT_REPORTING_INTERVAL;
    
    // Change list to RingBuffer?
    private Set<AbstractReporter<? extends ReportData>> reporters = new LinkedHashSet<>();
    private RetryReporter retryReporter = new RetryReporter();
    private IPeriodicComponent periodic;
    private IHttpClientComponent httpClient;
    private long timerId;

    private boolean started = false;
    private volatile boolean sending = false;

    public BatchedReporter() {
        reporters.add(retryReporter);
    }

    public boolean isStarted() {
        return started;
    }

    public BatchedReporter setReportingInterval(int millis) {
        this.reportingInterval = millis;
        return this;
    }

    public BatchedReporter addReporter(AbstractReporter<? extends ReportData> reporter) {
        reporter.setFullHandler(isFull -> {
            send();
        });

        reporters.add(reporter);
        return this;
    }

    public BatchedReporter start(IPeriodicComponent periodic, IHttpClientComponent httpClient) {
        if (started)
            throw new IllegalStateException("Already started");
        this.httpClient = httpClient;
        this.periodic = periodic;

        this.timerId = periodic.setPeriodicTimer(reportingInterval, DEFAULT_INITIAL_WAIT, id -> {
            System.out.println("tick! " + id + System.currentTimeMillis());
            send();
        });
        started = true;
        return this;
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

            for (final ReportToSend sendIt : sendItList) {
                itemsOfWork++;
                System.out.println("Sending :" + itemsOfWork);

                IHttpClientRequest post = httpClient.request(sendIt.getEndpoint().toString(), // TODO change to broken down components
                        HttpMethod.POST, 
                        new ReportResponseHandler(reportResult -> {
                            retryIfFailure(reportResult, sendIt);   
                            // TODO IMPORTANT: invalidate any bad credentials!
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

    private void retryIfFailure(IAsyncResult<ReportResponse> reportResult, ReportToSend report) {
        if (reportResult.isError()) { 
            // if (reportResult.getResult().isNonFatal()) {
            retryReporter.addRetry(report);
        }
    }

    // TODO Depending on which platform this is run on, could this become thread unsafe?
    // TODO Do we actually care if multiple send operations overlap?
    private void checkFinishedSending() {
        if (itemsOfWork<=0) {
            itemsOfWork=0;
            sending = false;
        }
    }
    
    private static class RetryReporter extends AbstractReporter<ReportData> {
        private Queue<ReportToSend> resendReports = EvictingQueue.create(DEFAULT_RETRY_QUEUE_MAXSIZE);

        @Override
        public List<ReportToSend> encode() {
            List<ReportToSend> copy = new LinkedList<>(resendReports);
            resendReports.clear(); // Some may end up coming back again if retry fails.
            return copy;
        }

        @Override
        public AbstractReporter<ReportData> addRecord(ReportData record) {
            throw new UnsupportedOperationException("Should not call #addRecord on special retry BatchedReporter");
        }
        
        // Notice that super.full() is never triggered, we just evict old records once limit is hit.
        public AbstractReporter<ReportData> addRetry(ReportToSend report) {
            resendReports.offer(report); 
            return this;
        }
    }



    // This will not be used concurrently
//    private final EvictingQueue<ReportToSend> retryQueue = EvictingQueue.<ReportToSend>create(DEFAULT_RETRY_QUEUE_MAXSIZE, DEFAULT_RETRY_QUEUE_MAXSIZE/8)
//                .warningHandler(a -> {
//                    System.err.println("Report retry queue is close to capacity. Records pending retry may soon be evicted and permanently lost.");
//                })
//                .fullHandler(b -> {
//                    System.err.println("Report retry queue full. Oldest retry reports will be evicted.");
//                });

//    
//    private static class EvictingQueue<T> extends ForwardingQueue<T> {
//        private final Queue<T> delegate;
//        private final int maxSize;
//        private final int warningRemaining;
//        private IAsyncHandler<Void> warningHandler;
//        private IAsyncHandler<Void> fullHandler;
//
//        private EvictingQueue(int maxSize, int warningRemaining) {
//          this.delegate = new ArrayDeque<T>(maxSize);
//          this.maxSize = maxSize;
//          this.warningRemaining = warningRemaining;
//        }
//
//        public static <T> EvictingQueue<T> create(int maxSize, int warningSize) {
//          return new EvictingQueue<T>(maxSize, warningSize);
//        }
//
//        public int remainingCapacity() {
//          return maxSize - size();
//        }
//
//        @Override protected Queue<T> delegate() {
//          return delegate;
//        }
//
//        @Override public boolean offer(T e) {
//          return add(e);
//        }
//
//        @Override public boolean add(T e) {
//          Objects.nonNull(e);  // check before removing
//          if (maxSize == 0) {
//            return true;
//          }
//          if (size() == maxSize) {
//            delegate.remove();
//            fullHandler.handle((Void) null); // Fire full handler
//          } else if (remainingCapacity() < warningRemaining) {
//              warningHandler.handle((Void) null);
//          }
//          delegate.add(e);
//          return true;
//        }
//
//        @Override public boolean addAll(Collection<? extends T> collection) {
//          return standardAddAll(collection);
//        }
//
//        @Override
//        public boolean contains(Object object) {
//          return delegate().contains(Objects.nonNull(object));
//        }
//
//        @Override
//        public boolean remove(Object object) {
//          return delegate().remove(Objects.nonNull(object));
//        }
//        
//        public EvictingQueue<T> fullHandler(IAsyncHandler<Void> fullHandler) {
//            this.fullHandler = fullHandler;
//            return this;
//        }
//        
//        public EvictingQueue<T> warningHandler(IAsyncHandler<Void> warningHandler) {
//            this.warningHandler = warningHandler;
//            return this;
//        }
//    }
}
