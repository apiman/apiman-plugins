package io.apiman.plugins.auth3scale.util.report.batchedreporter;

import java.net.URI;

public interface ReportToSend {
	String data();
	String encoding();
	URI endpoint();
}
