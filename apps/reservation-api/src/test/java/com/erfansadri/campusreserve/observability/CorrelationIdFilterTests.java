package com.erfansadri.campusreserve.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void acceptsAValidIncomingCorrelationIdAndCleansUpMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("request-42"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("request-42");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesAnInvalidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "invalid value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).matches(
                        value -> value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .matches(value -> value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"));
    }
}
