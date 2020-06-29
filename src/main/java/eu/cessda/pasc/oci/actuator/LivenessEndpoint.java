package eu.cessda.pasc.oci.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Endpoint(id = "liveness")
@Component
public class LivenessEndpoint {
    @ReadOperation
    public String testLiveness() {
        return "{\"status\":\"UP\"}";
    }
}