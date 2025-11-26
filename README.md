

### To activate temporary database via H2

Use the following under your .env environment
```
DB_URL=jdbc:h2:mem:testdb
DB_DRIVER_CLASS=org.h2.Driver
DB_USER=sa
DB_PASSWORD=
```

### To start gradle

This is one way to start gradle, but first you'll want to load up your environment. To do so,
type the following. Do this....
```
./load_bash_env.sh
```
or this...
```
source ./.env
```

From there, you can execute your gradle library like so:
```
gradle clean bootRun
```

Sometimes, you may need to refresh your dependencies. To do so, input the following 
commands:

```
gradle build --refresh-dependencies -x test --no-build-cache
```


### Security Notes
When making a new authorization or configuration, make sure you keep configurations split. For example:

**Scope** will be for external authentications that may be used within the application (Think OAuth2).
**Role** is used explicitly for defined application authorities (Think of classic sprint roles)

As such, when we're looking to authorize certain features, we need to use the following syntax.

For Roles, use...
```
@PreAuthorize("hasRole('PRO_MONTHLY')")
```

For Scope, use...
```
@PreAuthorize("hasAuthority('SCOPE_LOGIN')")
```

## How to turn on HTTPS for MM Backend

1) Generate a self-signed cert:
```
keytool -genkeypair \
   -alias mm-local \
   -keyalg RSA \
   -keysize 2048 \
   -storetype PKCS12 \
   -keystore mm-local.p12 \
   -validity 3650 \
   -dname "CN=localhost, OU=Dev, O=MondayMemory, L=Nowhere, S=Nowhere, C=US" \
   -storepass <YOUR PASSWORD HERE>
```
2) Store that under your /src/main/resources/ssl/mm-local.p12
3) Make sure that you're writing the environment properties under your .env (Step 4 will explain the next steps) 
4) Wire HTTPs into your application.properties and make sure you're running local like so:
   server.ssl.enabled=true
   server.ssl.key-store=classpath:ssl/${SSL_KEY_STORE}
   server.ssl.key-store-password=${SSL_PASSWORD}
   server.ssl.key-store-type=PKCS12
   server.ssl.key-alias=${SSL_ALIAS}
5) Check that you can curl your localhost
   curl -k https://localhost:8443/actuator/health
