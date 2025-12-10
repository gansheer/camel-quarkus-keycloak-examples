


docker run quay.io/keycloak/keycloak start-dev

docker run --name mykeycloak -p 127.0.0.1:8080:8080 \
-e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
quay.io/keycloak/keycloak:latest \
start-dev

http://localhost:8080/admin