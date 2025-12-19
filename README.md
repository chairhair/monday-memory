

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

## How to run docker under here to get Redis + ElasticSearch

1) First, remove all docker containers

```
sudo apt remove docker docker-engine docker.io containerd runc
```

2) Add the GPG key

```
sudo apt update
sudo apt install ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

sudo chmod a+r /etc/apt/keyrings/docker.gpg

```

3) Add Docker's official repo

```
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

4) Install Docker Engine + Docker Compose

```
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

5) Add self to the docker group

```
sudo usermod -aG docker $USER
```

Exit out of the terminal, then get back in and start docker.

If you're on Linux, this is....
```
sudo systemctl start docker
```

Otherwise, install Docker-Desktop, Enable WSL integration, and BAM you're done

## How to generate a secret

Basically just run this:

`openssl rand -hex 32`

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
```
   server.ssl.enabled=true
   server.ssl.key-store=classpath:ssl/${SSL_KEY_STORE}
   server.ssl.key-store-password=${SSL_PASSWORD}
   server.ssl.key-store-type=PKCS12
   server.ssl.key-alias=${SSL_ALIAS}
```
Make sure that you ALSO run the following docker containers for your redis in-mem cache and ElasticSearch:
```
docker run --name mm-redis -p 6379:6379 -d redis:7
docker run --name mm-es -p 9200:9200 -e "discovery.type=single-node" -d docker.elastic.co/elasticsearch/elasticsearch:8.15.0
```

5) Check that you can curl your localhost
   curl -k https://localhost:8443/actuator/health

## To Run Postgres

To set it down, go `docker compose down -v`
To set it up, go `docker compose up -d`

## To Run the Stripe

1) Download `stripe_1.34.0_linux_x86_64.tar.gz` from the docs
2) Extract using `tar -xvf stripe_<rest of file>`
3) Now you can execute it
