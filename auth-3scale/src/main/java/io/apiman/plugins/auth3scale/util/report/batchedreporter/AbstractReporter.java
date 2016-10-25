package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.plugins.auth3scale.authrep.AuthRepConstants;
import io.apiman.plugins.auth3scale.authrep.apikey.ApiKeyReportData;
import io.apiman.plugins.auth3scale.authrep.appid.AppIdAuthReporter;
import io.apiman.plugins.auth3scale.util.ParameterMap;

public abstract class AbstractReporter<T extends ReportData> {
    private IAsyncHandler<Void> fullHandler;

    protected final Map<Integer, ConcurrentLinkedQueue<T>> reports = new ConcurrentHashMap<>(); // TODO LRU?
    protected static final int DEFAULT_LIST_CAPAC = 800;
    protected static final int FULL_TRIGGER_CAPAC = 500;
    protected static final int MAX_RECORDS = 1000;

    public AbstractReporter() {
    }

    public abstract List<ReportToSend> encode();

    public abstract AbstractReporter<T> addRecord(T record);

    public AbstractReporter<T> setFullHandler(IAsyncHandler<Void> fullHandler) {
        this.fullHandler = fullHandler;
        return this;
    }

    protected void full() {
        fullHandler.handle((Void) null);
    }

    protected <Value> ParameterMap setIfNotNull(ParameterMap in, String k, Value v) {
        if (v == null)
            return in;
        in.add(k, v);
        return in;
    }
}
