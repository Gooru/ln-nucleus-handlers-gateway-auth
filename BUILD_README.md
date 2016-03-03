Building nucleus-handlers-gateway-auth
==============

## Prerequisites

- Gradle 2.7
- Java 8
- Redis 3.2.0

## Running Build

The default task is *shadowJar* which is provided by plugin. So running *gradle* from command line will run *shadowJar* and it will create a fat jar in build/libs folder. Note that there is artifact name specified in build file and hence it will take the name from directory housing the project.

Once the far Jar is created, it could be run as any other Java application.

## Running the Jar as an application

Following command could be used, from the base directory.

Note that any options that need to be passed onto Vertx instance need to be passed at command line e.g, worker pool size etc

> java -classpath ./build/libs/nucleus-handlers-gateway-auth-0.1-snapshot-fat.jar: -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory io.vertx.core.Launcher -conf src/main/resources/nucleus-auth.json -cluster -instances 4

The project already has dependency for hazelcast included. Currently, there is no cluster specific configuration done. That needs to be included in real deployment.

## Running Redis Server

Follow installation instructions give at http://redis.io/download and start the redis server.

## Using Nucleus Auth for Authorization (Only DEV/TEST environment)

This verticle handles authorization for all requests coming on /api/nucleus/*. In order to use this verticle, you need to have session token key present in redis so that authorization will work in proper way. (We are manually inserting the session token key in redis for time being till our actual auth systems is being ready.

Once you started redis server, go to command line and type 'redis-cli' to login to redis. Then execute below command to insert session token. 

> set YTU4Yzg1OTctMzFiNS00YTQ0LWFmOTMtZDQ0MDE2NDI2NTgyOlRodSBGZWIgMDQgMjM6MzE6NDAgVVRDIDIwMTY6MTQ1NDYyODcwMDc2NA== '{"user_id":"a58c8597-31b5-4a44-af93-d44016426582","username":"my-user-name","client_id":"ba956a97-ae15-11e5-a302-f8a963065976","provided_at":1454478312796,"prefs":{"taxonomy":[{"standard_framework_code":"CSS","default_subject_code":"GUT.M"}]},"cdn_urls":{"profile.cdn":"//profile.gooru.org.com"},"access_token_validity":86400}' EX 3600

Syntax for above command is -> set [session-token] [value] EX [expiry-time]

where value is JSON and expiry-time is in seconds

* For Production, proper auth system should be in place which will actually insert session token key in redis. 
