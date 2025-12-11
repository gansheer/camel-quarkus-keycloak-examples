package fr.gansheer.camel.quarkus.example;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Random;

public class Routes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        Random randomNumbers = new Random();
        int randomInt = randomNumbers.nextInt(10000);

        String realmHeader = "my-company-endpoint-config-" + randomInt;
        String userHeader = "my-user-endpoint-config-" + randomInt;
        String roleHeader = "my-role-endpoint-config-" + randomInt;

         String keycloakEndpoint = String.format(
            "keycloak:admin?serverUrl=%s&realm=master&username=%s&password=%s",
            "http://localhost:8080", "admin", "admin");

        from("timer://foo?repeatCount=1&delay=1000")
                .log("Let's do things in keycloak")
                .to("direct:keycloakUseCaseRoute")
                .log("Ze End");

        // Test operations endpoints
        from("direct:keycloakUseCaseRoute")

                // create realm
                .log("*** Let's create the realm " + realmHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=createRealm")
                .log("Result: ${body}")

                // get realm
                .log("*** let's get the realm " + realmHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setExchangePattern(ExchangePattern.InOut)
                .to(keycloakEndpoint + "&operation=getRealm")
                .log("got the realm ${body.realm}")

                // update realm
                .log("*** let's update the realm " + realmHeader)
                .convertBodyTo(RealmRepresentation.class)
                .log("converted to RealmRepresentation.class")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        RealmRepresentation payload = exchange.getMessage().getBody(RealmRepresentation.class);
                        payload.setDisplayName("Update realm " + realmHeader);
                        payload.setEditUsernameAllowed(true);
                        exchange.getMessage().setBody(payload, RealmRepresentation.class);
                    }
                })
                .to(keycloakEndpoint + "&operation=updateRealm&pojoRequest=true")
                .log("Result: ${body}")

                // Add a group

                // Add a user
                .log("*** let's create the user ")
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
                .log("*** let's search the user " + userHeader)
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
                .log("*** let's get the user ${headers.CamelKeycloakUserId}")
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=getUser")
                .log("got the user ${body}")


                // update user - updateUser
                .log("*** let's update the user ${headers.CamelKeycloakUserId}")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        UserRepresentation payload = exchange.getMessage().getBody(UserRepresentation.class);
                        String userId = exchange.getMessage().getHeader(KeycloakConstants.USER_ID, String.class);
                        payload.setUsername("user-name-updated-" + randomInt);
                        payload.setFirstName("FirstName-updated-" + randomInt);
                        payload.setLastName("LastName-updated-" + randomInt);
                        exchange.getMessage().setBody(payload, UserRepresentation.class);
                    }
                })
                .to(keycloakEndpoint + "&operation=updateUser&pojoRequest=true")
                .log("Result: ${body}")

                // create a role
                .log("*** let's create a role")
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.ROLE_NAME, constant(roleHeader))
                .to(keycloakEndpoint + "&operation=createRole")
                .log("created the role ${headers}")
                .log("Result: ${body}")

                // get role
                .log("*** let's get the role " + roleHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.ROLE_NAME, constant(roleHeader))
                .to(keycloakEndpoint + "&operation=getRole")
                .log("got the realm ${body}")

                // update role
                .log("*** let's update the role " + roleHeader)
                .convertBodyTo(RoleRepresentation.class)
                .log("converted to RoleRepresentation.class")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        RoleRepresentation payload = exchange.getMessage().getBody(RoleRepresentation.class);
                        payload.setDescription("role-description-updated-" + randomInt);
                        exchange.getMessage().setBody(payload, RoleRepresentation.class);
                    }
                })
                .to(keycloakEndpoint + "&operation=updateRole&pojoRequest=true")
                .log("Result: ${body}")

                // assign role to user - assignRoleToUser
                .log("*** let's assign the role " + roleHeader + " to the user ${headers.CamelKeycloakUserId}")
                .log("check the headers ${headers}")
                .log("we should have : " + KeycloakConstants.REALM_NAME + ", " + KeycloakConstants.ROLE_NAME + ", " + KeycloakConstants.USER_ID)
                .to(keycloakEndpoint + "&operation=assignRoleToUser")
                .log("Result: ${body}")

                // get the user role
                .log("*** let's get the roles for the user ${headers.CamelKeycloakUserId}")
                .log("check the headers ${headers}")
                .log("we should have : " + KeycloakConstants.REALM_NAME + ", " + KeycloakConstants.USER_ID)
                .to(keycloakEndpoint + "&operation=getUserRoles")
                .log("Result: ${body}")

                // remove role from user -
                .log("*** let's remove the role " + roleHeader + " from the user ${headers.CamelKeycloakUserId}")
                .log("check the headers ${headers}")
                .log("we should have : " + KeycloakConstants.REALM_NAME + ", " + KeycloakConstants.ROLE_NAME + ", " + KeycloakConstants.USER_ID)
                .to(keycloakEndpoint + "&operation=removeRoleFromUser")
                .log("Result: ${body}")

                // delete role
                .log("*** let's delete the role " + roleHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.ROLE_NAME, constant(roleHeader))
                .to(keycloakEndpoint + "&operation=deleteRole")
                .log("Result: ${body}")

                // delete user
                .log("*** let's delete the user " + userHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .setHeader(KeycloakConstants.USER_ID, constant(userHeader))
                .to(keycloakEndpoint + "&operation=deleteUser")
                .log("Result: ${body}")

                // delete realm
                .log("*** let's delete the realm " + realmHeader)
                .setHeader(KeycloakConstants.REALM_NAME, constant(realmHeader))
                .to(keycloakEndpoint + "&operation=deleteRealm")
                .log("Result: ${body}")
        ;


    }
}
