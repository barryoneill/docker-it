# hbc-docker-it

This library offers traits that developers can mix into their scala tests, which:

- Spin up preconfigured services in docker containers alongside your test
    - Right now that's just [localstack](https://github.com/localstack/localstack) and [redis](https://redis.io/), 
    but we hope for many others to be added.
    - Or you can quickly configure your own! - The docker heavy lifting 
    is provided by the excellent [docker-it-scala](https://github.com/whisklabs/docker-it-scala) project.
- Offer complementing matchers for each container trait which offers fluent-style matching, keeping tests readable and reducing boilerplate further. 
- Offer utility classes which provide some basic operations for the related service, facilitating pre-test setup. 

## Getting Started
 
Add the test dependency:
 
```sbt
libraryDependencies += "com.hbc" %% "hbc-docker-it" % "0.9.5" % Test
```

**TODO: Right now the dependency is published to HBC JFrog repo.  Until(if) this gets published externally, 
you'll need credentials to that repo for sbt to find it. Alternatively, clone this repo locally and run `sbt publishLocal`
to create a local copy.**:see_no_evil:  

Add the `DockerSuite` trait to your test, then add one or more `*Container` traits for each container you wish to run. 

E.g., to run a suite with both Redis and Localstack, your suite should be similar to:
 
```scala

class MyTestSuite extends WordSpec
  with DockerSuite with RedisContainer with LocalStackContainer {
  ...
}

``` 

When your test starts, the redis container and localstack containers will start, and will be killed after the test runs.

Some points:
- The test will not begin until all containers are *ready*, i.e. the services that the containers
offer have been confirmed started. 
    - In the case of the LocalStack container, that can be ~15 seconds!
- If you don't have the relevant docker images cached, docker will have to fetch them, resulting in a much longer first-run. 
- The containers map the known service ports to ephemeral ports.  That way, if a test JVM is aborted or 
     some other critical error leaves a container behind alive, subsequent runs won't fail with in-use ports.
   
## Interacting With The Services

#### An Example!
As new traits and util methods get implemented, we'll add coverage to [DockerSuiteTest](src/test/scala/com/hbc/dockerit/DockerSuiteTest.scala), so if you just want to see (copy and paste) an example, it's a good starting point.

### trait `RedisContainer` 

Brings in:

- `redisPort:Int`: the ephemeral port mapped to the redis service. Pass this to your production code under test.
- `redis:com.twitter.finagle.redis.Client` into scope, the [twitter/finagle](https://github.com/twitter/finagle) client configured to communicate with the service. 
    - Use this in your test to do any test data prep (use `RedisUtil(redis)` for some common convenience methods) 
- Some fluent matchers on the `redis` client:

```scala
// redis should only contain these keys
redis should haveOnlyKeys("one", "two")

// other keys may exist
redis should haveKeys("human", "dog")              

// keys should exist that match the pattern
redis should haveKeysMatching("*o*")       

// redis "get dog" should return "woof"         
redis should haveValueOnGet("dog", "woof")

// redis "get user" should result in the circe encoded JSON for User("Homer","Simpson")             
redis should haveEncodedValueOnGet("user", User("Homer","Simpson"))
```

### trait `LocalStackContainer` 

The LocalStack container offers many services, but right now we're only starting with Kinesis (Feel free to contribute!).  This trait introduces:  

- `portWebUI:Int`: the ephemeral port mapped to localstack's web UI
- `portKinesis:Int`: same, but for the Kinesis service
- `portCloudwatch:Int`: same, but for the CloudWatch service
- `dummyAWSCreds:AWSCredentialsProvider`: LocalStack doesn't need credentials, but the AWS clients do, so you can pass this in when building your clients.   
  
The `LocalstackContainer` trait brings in the `KinesisMatchers` trait, which introduces:

- `kinesis:AmazonKinesis`: an AWS Kinesis client, configured to talk to localstack's service.  Pass this 
    to your production code (or use for your own test setup).  
    - Use `KinesisUtil(kinesis)` for some common convenience methods)
- Some fluent matchers on the `kinesis` client:

```scala
// the kinesis stream should contain the following (circe JSON encoded) payloads
kinesis should havePendingEvents("mystream", Seq(User("Homer", "Simpson"), User("Moe","Szyslak")))
```

The `LocalstackContainer` trait brings in the `CloudWatchMatchers` trait, which introduces:

- `cloudwatch:AmazonCloudWatch`: an AWS cloudwatch client, configured to talk to localstack's cloudwatch service.

TODO(barry): This trait is a WIP.  Localstack provides support for Cloudwatch under the hood with moto (the mocking lib for
AWS's boto client), which appears to be rather limited right now.  Much trial and error. 

### Troubleshooting

- **Problem**: I see the following error almost immediately after starting my test.
    ```
    [info]   A timeout occurred waiting for a future to complete. Queried 11 times, sleeping 15 milliseconds between each query. (LocalstackContainer.scala:27)
    ```
  **Solution**: Your code is trying to interact with the container before it is ready.  Don't use any of the utils or any of
  the clients (that the traits bring into scope) outside of your tests, i.e. before the `beforeAll()` is complete. Any references
  outside the tests should be `lazy`. If you've implemented a `beforeAll()`, don't forget to make a `super.beforeAll()` call.        
  
   