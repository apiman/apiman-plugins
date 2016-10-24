package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.util.List;

import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.plugins.auth3scale.util.ParameterMap;

public abstract class AbstractReporter<T extends ReportData> {
    private IAsyncHandler<Void> fullHandler;

    public AbstractReporter() {
    }

    //ring buffer?
    //remember multiple different plugin will be using this same infrastructure - need to be careful
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
