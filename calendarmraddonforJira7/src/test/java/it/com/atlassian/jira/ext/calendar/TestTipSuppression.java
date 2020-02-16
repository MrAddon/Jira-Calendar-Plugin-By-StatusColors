package it.com.atlassian.jira.ext.calendar;

import com.atlassian.jira.functest.framework.BaseJiraFuncTest;
import com.atlassian.jira.functest.framework.RestoreBlankInstance;
import com.atlassian.sal.api.xsrf.XsrfHeaderValidator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RestoreBlankInstance
public class TestTipSuppression extends BaseJiraFuncTest {

    private WebResource resource;

    @Before
    public void setUp() throws Exception {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter("admin", "admin"));
        resource = client.resource(UriBuilder.fromUri(environmentData.getBaseUrl().toURI())
                .path("rest")
                .path("calendar-plugin")
                .path("1.0")
                .path("suppressedTips")
                .build()
        );
    }

    @Test
    public void shouldSuppressTips() throws Exception {
        ClientResponse response = resource
                .header(XsrfHeaderValidator.TOKEN_HEADER, "no-check")
                .entity(getSuppressionForm())
                .post(ClientResponse.class);

        assertThat(response.getClientResponseStatus(), equalTo(OK));
    }

    private MultivaluedMap<String, String> getSuppressionForm() {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("tipKey", "browseProjectCalendarTab");
        return form;
    }

    @Test
    public void shouldProtectFromXsrf() throws Exception {
        ClientResponse response = resource.entity(getSuppressionForm()).post(ClientResponse.class);

        assertThat(response.getClientResponseStatus(), equalTo(NOT_FOUND));
    }
}