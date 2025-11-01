

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