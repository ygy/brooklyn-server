package brooklyn.rest.resources;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.location.Location;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.rest.domain.*;
import brooklyn.rest.testing.BrooklynRestResourceTest;
import brooklyn.rest.testing.mocks.RestMockSimpleEntity;
import brooklyn.rest.util.JsonUtils;
import brooklyn.test.entity.TestApplication;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class UsageResourceTest extends BrooklynRestResourceTest {

    private final ApplicationSpec simpleSpec = ApplicationSpec.builder().name("simple-app").
            entities(ImmutableSet.of(new EntitySpec("simple-ent", RestMockSimpleEntity.class.getName()))).
            locations(ImmutableSet.of("localhost")).
            build();

    @Test
    public void testListApplicationUsages() throws InterruptedException {
        DateFormat format = new SimpleDateFormat(JsonUtils.DATE_FORMAT);
        Date start = new Date();
        ClientResponse response = client().resource("/v1/applications")
                .post(ClientResponse.class, simpleSpec);
        TaskSummary createTask = response.getEntity(TaskSummary.class);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

        response = client().resource("/v1/usage/applications").get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Statistic> usage = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usage.isEmpty());

        response = client().resource("/v1/usage/applications?start=" + format.format(start)).get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        usage = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usage.isEmpty());
    }

    @Test
    public void testGetApplicationUsages() {
        ClientResponse response = client().resource("/v1/applications")
                .post(ClientResponse.class, simpleSpec);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        TaskSummary createTask = response.getEntity(TaskSummary.class);
        
        response = client().resource("/v1/usage/applications/x").get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Statistic> usages = response.getEntity(new GenericType<List<Statistic>>() {
        });
        assertTrue(usages.isEmpty());

        response = client().resource("/v1/usage/applications/" + createTask.getEntityId()).get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usages.isEmpty());
        
        Statistic usage = usages.get(0);
        assertEquals(usage.getId(), createTask.getEntityId());
        assertEquals(usage.getStatus(), Status.RUNNING);

        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?start=1970-01-01T00:00:00-0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usages.isEmpty());
        
        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?start=9999-01-01T00:00:00+0100").get(ClientResponse.class);
        assertTrue(response.getStatus() >= 500, "end defaults to NOW, so future start should fail");
        
        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?start=9999-01-01T00:00:00%2B0100&end=9999-01-02T00:00:00%2B0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertTrue(usages.isEmpty());

        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?end=9999-01-01T00:00:00+0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usages.isEmpty());

        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?start=9999-01-01T00:00:00+0100&end=9999-02-01T00:00:00+0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertTrue(usages.isEmpty());

        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?start=1970-01-01T00:00:00-0100&end=9999-01-01T00:00:00+0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usages.isEmpty());
        
        response = client().resource("/v1/usage/applications/" + createTask.getEntityId() + "?end=1970-01-01T00:00:00-0100").get(ClientResponse.class);
        usages = response.getEntity(new GenericType<List<Statistic>>() {});
        assertTrue(usages.isEmpty());
    }

    @Test
    public void testGetMachineUsages() {
        ClientResponse response = client().resource("/v1/usage/machines").get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Statistic> usage = response.getEntity(new GenericType<List<Statistic>>() {});
        assertTrue(usage.isEmpty());
    }

    @Test(enabled=false)
    public void testGetMachineUsageWithMachines() {
        Location location = getManagementContext().getLocationRegistry().resolve("localhost");
        assertTrue(location instanceof LocalhostMachineProvisioningLocation);
        ((LocalhostMachineProvisioningLocation) location).provisionMore(5);
        
        ClientResponse response = client().resource("/v1/applications").post(ClientResponse.class, simpleSpec);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        
        response = client().resource("/v1/usage/machines").get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Statistic> usage = response.getEntity(new GenericType<List<Statistic>>() {});
        assertFalse(usage.isEmpty());
    }

    @Test
    public void testGetMachineUsageForApp() {
        ClientResponse response = client().resource("/v1/usage/machines?application=x").get(ClientResponse.class);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Statistic> usage = response.getEntity(new GenericType<List<Statistic>>() {});
        assertTrue(usage.isEmpty());
    }

    @Override
    protected void setUpResources() throws Exception {
        addResources();
    }
}
