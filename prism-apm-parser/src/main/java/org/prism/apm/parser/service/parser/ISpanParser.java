package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.model.span.SpanInfo;
import java.util.List;

/**
 * A generic interface for parsing trace data.
 * Implementations of this interface can be conditionally activated based on configuration.
 */
public interface ISpanParser {

    /**
     * Parses a raw trace payload string into a list of SpanInfo objects.
     *
     * @param tracePayload The raw string content of the trace data.
     * @return A list of {@link SpanInfo} objects parsed from the payload. Returns an empty list if parsing fails.
     */
    List<SpanInfo> parse(String tracePayload);
}
