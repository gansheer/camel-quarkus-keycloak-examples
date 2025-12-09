package fr.gansheer.camel.quarkus.example;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.keycloak.KeycloakConstants;

import java.util.Random;

public class Routes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        Random randomNumbers = new Random();

        String realmHeader = "my-company-endpoint-config-"+ randomNumbers.nextInt(10000);

         String keycloakEndpoint = String.format(
            "keycloak:admin?serverUrl=%s&realm=master&username=%s&password=%s",
            "http://localhost:8080", "admin", "admin");

         from("timer://foo?repeatCount=1&delay=1000").to("direct:createRealm").log("should create realm");

        // Create the realm
        from("direct:createRealm")
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=createRealm")
                .log("creating realm");
    }
}
