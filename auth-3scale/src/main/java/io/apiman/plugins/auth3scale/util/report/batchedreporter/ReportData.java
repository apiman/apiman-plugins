package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import io.apiman.plugins.auth3scale.util.ParameterMap;

public interface ReportData {
    ParameterMap getUsage();
    ParameterMap getLog();
    int groupId();
}
