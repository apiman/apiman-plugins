package io.apiman.plugins.auth3scale.authrep;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public interface ReportEncoder {
    default String encode() {

        return null;
    }
}
