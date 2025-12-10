package fr.gansheer.camel.quarkus.example;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Random;

public class Routes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        Random randomNumbers = new Random();

        String realmHeader = "my-company-endpoint-config-"+ randomNumbers.nextInt(10000);
        String userHeader = "my-user-endpoint-config-"+ randomNumbers.nextInt(10000);

         String keycloakEndpoint = String.format(
            "keycloak:admin?serverUrl=%s&realm=master&username=%s&password=%s",
            "http://localhost:8080", "admin", "admin");

        from("timer://foo?repeatCount=1&delay=1000")
                .log("Let's do things in keycloak")
                .to("direct:keycloakUseCaseRoute")
                .log("Ze End");

        // Test operations endpoints
        from("direct:keycloakUseCaseRoute")
                .log("Let's create the realm " + realmHeader)

                // create realm
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=createRealm")
                .log("Result: ${body}")

                // get realm
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setExchangePattern(ExchangePattern.InOut)
                .log("let's get the realm " + realmHeader)
                .to(keycloakEndpoint + "&operation=getRealm")
                .log("got the realm ${body.realm}")

                // update realm
                .convertBodyTo(RealmRepresentation.class)
                .log("converted to RealmRepresentation.class")
                .log("let's update the realm " + realmHeader)
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        RealmRepresentation payload = exchange.getMessage().getBody(RealmRepresentation.class);
                        payload.setDisplayName("Update realm " + realmHeader);
                        exchange.getMessage().setBody(payload, RealmRepresentation.class);
                    }
                })
                .to(keycloakEndpoint + "&operation=updateRealm&pojoRequest=true")
                .log("Result: ${body}")

                // Add a user
                .log("let's create the user ")
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.USERNAME, constant(userHeader))
                .setHeader(KeycloakConstants.USER_EMAIL, constant(userHeader + "@test.com"))
                .setHeader(KeycloakConstants.USER_FIRST_NAME, constant("Test"))
                .setHeader(KeycloakConstants.USER_LAST_NAME, constant("User"))
                .to(keycloakEndpoint + "&operation=createUser")
                // TODO find the ID in the reponse
                .log("created the user ${headers}")
                .log("Result: ${body}")

                // find user id -
                .log("let's search the user "+userHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.SEARCH_QUERY, constant(userHeader))
                .to(keycloakEndpoint + "&operation=searchUsers")
                .log("Result: ${body}")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {

                        List users = exchange.getMessage().getBody(List.class);
                        if (users != null && users.size() > 0) {
                            UserRepresentation user = (UserRepresentation) users.get(0);
                            exchange.getMessage().setHeader(KeycloakConstants.USER_ID, user.getId());
                        } else {
                            exchange.getMessage().setHeader(KeycloakConstants.USER_ID, "");
                        }

                    }
                })
                .log("user found, user id is: ${headers.CamelKeycloakUserId}")


                // get user
                .log("let's get the user ${headers.CamelKeycloakUserId}")
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=getUser")
                .log("got the user ${body}")


                // delete user
                .log("let's delete the user " + userHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=deleteUser")
                .log("Result: ${body}")

                // delete realm
                .log("let's delete the realm " + realmHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=deleteRealm")
                .log("Result: ${body}")
        ;


    }
}
