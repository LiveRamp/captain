Captain
============

>  O Captain! my Captain! our fearful trip is done!   
>  -- Walt Whitman, "O Captain! My Captain!"

### Overview
Captain is a distributed, light-weight java workflow engine designed for use in a microservice architecture. It's primary purpose is to make it easy compose microservices into a workflow. It is heavily opinionated towards building simple workflows. For example, it heavily encourages linear workflows, though one can dynamically build DAGs as necessary.

At LiveRamp, Captain supports multiple pipelines that regularly have on the order of tens of thousands of active requests at a time. We hypothesize that it's at least scalable up to another order of magnitude.

To this end, here are the goals of Captain:
1. abstract away coordination logic from individual services
2. reduce boilerplate around coordination, making it faster to compose existing tools to build new products
3. increase visibility into the steps in a pipeline
4. encourage simplicity in system design

How is Captain different from other workflow engines?
1. Captain relies on you to handle your persistence of a request and its metadata. You don't have to set up special DB for captain. For some this will be really valuable and others it will be a deal breaker.
2. Captain is really easy to set up. We've provided an [ExampleCaptainWorkflow](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java)
3. Like [daemon_lib](https://github.com/LiveRamp/daemon_lib), the core library has no infrastructure dependencies beyond access to a working directory on disk.
4. It's opinionated towards linear workflows. 

### Adding the dependency

In Maven, make your project section look like this:

```
<project>
<!-- All the other stuff -->

  <dependencies>
    <!-- All your other dependencies -->
    <dependency>
      <groupId>com.liveramp</groupId>
      <artifactId>captain</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>

  <repositories>
    <repository>
      <id>liveramp-repositories</id>
      <name>Liveramp Repositories</name>
      <url>http://repository.liveramp.com/artifactory/liveramp-repositories</url>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
      </snapshots>
    </repository>
  </repositories>
</project>
```

The repository section is necessary because this project has not been published to Maven Central yet.

### Getting Started

Captain requires only 3 inputs from the developer (4 if you are running it in a distributed fashion). We're going to focus on the barest bones approach in this section of to see how we can run Captain on a single node. In later sections we cover all of the other features of Captain and discuss more complicated use cases.

1. manifest: a list of the steps a request should follow in your workflow 
2. config producer: a way for captain to find requests to process 
3. request updater: a way for captain to interact with your persistence layer

All it takes to get Captain running is the following:
e.g.
```java
CaptainBuilder.of("example-captain-daemon", configProducer, manifestManager, requestUpdater)
        .setNotifier(daemonNotifier)
        .setConfigWaitTime(1, TimeUnit.SECONDS)
        .setNextConfigWaitTime(1, TimeUnit.SECONDS)
        .build()
        .run();
```
[You can find the rest of this example in ExampleCaptainWorkflow](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java).

Let's walk through what each of the components we're passing into this builder are.

#### Config Producer

Create a class the implements `CaptainConfigProducer`. This is your opportunity to tell Captain how to find requests to process. Commonly this will just be a db query to wherever you store requests or reading from some sort of event queue. Regardless of the implementation you're trying to pull the requests that are (or may be) ready to progress in your pipeline.

e.g. If I store my requests in a db table, I'll be looking for requests that have a status of `ready`, `pending`, `in_progress` or `completed` that are not in a step of `done` or `cancelled`. Usually you're going to ignore requests that are `quarantined` or `failed` or requests that you've already completed processing (e.g. step: `done` or `cancelled`).

Check out [ExampleConfigProducer](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java) for a simple example.

#### Request Updater

Create a class that implements `RequestUpdater` iface. The Request Updater is your opportunity to tell Captain how it can change the step and status on your request. 

In the case where you're interacting with a db or crud service, you're implementing pretty orthodox state changes like `setStatus`, `setStepAndStatus`, `quarantine`, etc.

Check out [ExampleRequestUpdater](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java) for an example.

#### Manifest

A manifest enumerates the steps (Waypoints) of a given Captain App.

e.g.
```
waypoint 1: ingest and parse data
waypoint 2: run analysis on data (let's say this calls some service to kick off a map-reduce job)
waypoint 3: report on output of analysis
``` 

##### Waypoint Components

For each Waypoint you provide Captain with an implementation of how it should execute the step. This is accomplished via three components:

###### Request Submitter

Implementing `CaptainRequestSubmitter` allows you to tell a Captain Waypoint how to build a config and then submit it. It takes in the id of your request, and it optionally returns a request handle (which will be explained in the next section)
An example can be found here in [MapReduceJobSubmitter](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java).

A pretty common pattern for a Submitter is that it will build a config by pulling information out of the db or talking to previously used services before submitting it to some new service.

###### Handle Persistor

Implementing `CaptainHandlePersistor` allows you to instruct Captain on how to save the id (or request handle) for the work that it triggered in the request submitter. It takes in a request handle and does not return anything. 

e.g. When one submits a request to the analytics service for it to kick off a spark job to do some analysis, the service returns a job id, so that the progress of that request may be tracked. The handler persistor allows the user to save that handle as they see fit. Here's a code An example can be found here in [MapReduceJobHandlePersistor](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java).

This class is not required in any Captain waypoint. If you do not need to track the request id of your work, you don't need to implement this class. If this is the case, you can just have your Request Submitter return null as it won't be read anywhere else.

###### Status Retriever

Implementing `CaptainStatusRetriever` allow you to tell a Captain Waypoint how to check the status of a request. It takes in an id and returns a `CaptainStatus`.

The most common use case is to poll a service to which you've submitted work as to the status of your request. Based on the status returned by the service, you tell Captain what status it should set for your request.

##### Waypoint Types

There are 3 types of waypoints. Each use some or all of these Waypoint Components described above (Request Submitter, Handle Persistor, Status Retriever)

###### Asynchronous Waypoint

This Waypoint submits work to a service and saves the request id it receives from the service. It then uses the status retriever to check back that that service fulfilled its request. It accepts a Request Submitter, Handle Persistor, and Status Retriver. The Handle Persistor is optional and can be skipped in the cases described in the Handle Persistor section.

e.g. I submit a request to the analytics service that kicks off some long running job. Save the job id. I poll until my job is done.

note: "submits work to a service" technically your waypoint can just do work in the process it's spun up on whatever local hardware its running on. Doing so subverts the point of Captain. The goal is to make coordination between components cheap so that we make good choices in separating our concerns. As a rule of thumb, the less code you're writing in the Captain classes, the more you're adhering to the intended goals of the project. Nothing is stopping you from doing horrible things, Captain's trying to give you every opportunity to make a good choice.

###### Synchronous Waypoint

This Wayoint submits work to a service, and then, as long as the submission of that work does not fail, moves the request on to the next step in your manifest.

It accepts a Request Submitter. 

It optionally accepts a Handle Persistor, in the case where you want to save a request id, but don't want to wait for that request to be processed. 

e.g. I submit a request to a service to report that new stats have been generated. As long as the service returns no error, I'm good to go.

###### Control Flow Waypoint
 
This Waypoint is designed for:  
1. forcing a request to wait for pre-conditions to be met before progressing in your pipeline. 
2. making it easy to add validations to your Captain workflow.

It accepts a Status Retriever only. Here are a couple examples of how it could be used:

e.g. Hold the request until the file is ready to be processed.
```java
  class IngestionComplete implements StatusRetriever {

    private MockIngestionService ingestionService;

    IngestionComplete(MockIngestionService ingestionService) {
      this.ingestionService = ingestionService;
    }

    @Override
    public CaptainStatus getStatus(long id) {
      // call to the ingestion team that's checks to see if the file is ready for us to process.
      boolean fileReady = ingestionService.isFileDone(id);
      if (fileReady) {
        return CaptainStatus.COMPLETED; // returning COMPLETED means we go forward.
      } else {
        return CaptainStatus.IN_PROGRESS; // IN_PROGRESS means we'll try again later.
      }
    }
  }
```

e.g. Quarantine a request that is misconfigured.

```java
  class ValidateFileSize implements StatusRetriever {

    private MockIngestionService ingestionService;

    IngestionComplete(MockIngestionService ingestionService) {
      this.ingestionService = ingestionService;
    }

    @Override
    public CaptainStatus getStatus(long id) {
      int fileSize = ingestionService.getFileSizeInGbs(id);
      if (fileSize < 10) {
        return CaptainStatus.COMPLETED; // returning COMPLETED means we go forward.
      } else {
        return CaptainStatus.QUARANTINED; // QUARANTINED means there's some manual intervention needed. alternatively
        // we could have used CaptainStatus.CANCELLED, which by convention we assume is an unrecoverable state. 
      }
    }
  }
```


##### Manifest Example

Here's an [example](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java) of a complete Manifest as constructed from the components described above.


##### Processing Multiple Manifests

Captain allows you to have a single Captain instance process a single manifest (e.g. all of your requests follow the exact same path) using `SingleAppManifestManager`. It also allows for a single instance to process multiple manifests (`MultiAppManifestManager`). This choice allows for separation of concerns and hardware as desired by the developer. Spinning up multiple Captain instances is not frictionless, but it does give a better guarantee that one pipeline won't affect another.

By providing an app type to captain and a map of app types to manifests, Captain handles multiple manifests. Here's an [example](https://github.com/LiveRamp/captain/tree/master/src/main/java/com/liveramp/captain/exception/ExampleCaptainWorkflow.java).

Captain currently forces you to use one of the two provided ManifestManagers: `SingleAppManifestManager` or `MultiAppManifestManager`. These provide convenient but light-weight abstractions for handling manifests.  

### More Advanced Usage

There are extensive javadoc comments on [CaptainBuilder](https://github.com/LiveRamp/captain/blob/master/src/main/java/com/liveramp/captain/daemon/CaptainBuilder.java). That's a good resource if you're trying to sort through the other knobs and dials in Captain. In the rest of this read me, we are going to focus on covering concepts that may warrant more discussion.

#### Running Captain in a Distributed Fashion

Captain was designed to be horizontally scalable. Depending on your implementation, however, we may need to cover one more concept. 

If you are running Captain on multiple nodes and the config producer is just calling `dequeue` from some sort of distributed queue with good concurrency guarantees, you shouldn't need to worry about anything in this section. The basic implementation that we discussed in Getting Started should be enough.

If, however, your config producer does something like this:
```
* query the db for any request that's not done
* pick the one that captain processed least recently
* rinse - repeat
```

Then when you try to run captain on multiple nodes you'll run into issues, because _how do you guarantee that two Captain nodes do not pick up the same request at the same time_? For example let's say we have Captain Node 01 (CN01) and CN02. If the both hit the db at the same time on a request that is ready to submit data to our fictional analytics service. Then it's possible they'll BOTH submit requests to the analytics service and we'll run 2 map-reduce jobs when we should have run 1. Obviously this compounds with more nodes. Captain offers a locking solution to handle this case.

#### Captain Locks (optional if running on only one node)
 
If you are running your Captain instance across multiple nodes, you'll need a lock to prevent different nodes from picking up requests that other nodes are already processing. There are two locks that you'll need to implement: `DaemonLock` and `RequestLock`. 

A `DaemonLock` is a lock that is designed to guarantee that the config producers of know two of your Captain nodes can run at the same time. This prevents 2 Captain nodes picking up the same request.

A `RequestLock` is a request level lock. It allows you to guarantee that no other node tries to pick up a node that you're currently processing. After the daemon lock releases, you still don't want another node to pull your request out of the db.

If you already work with Zookkeeper, you can use Captain's built-in locks. Call `setZkLocks` in `CaptainBuilder` and pass in your Zookkeeper `CuratorFramework`. Captain will do the rest.

If you have your own locking toolset, the provided interfaces should provide a pretty easy guide to plugging it into Captain.

#### Config Producer Caching

Again in the case where your config producer is just querying to some db, what you can run into is that Captain is making a lot of small queries on your db which can hurt your overall db health. 

If you suspect this to be the case, a common practice is to just implement a cache in your config producer. So instead of just pulling one request, try pulling in n requests (where n isn't going to blow your memory; a few hundred is pretty common at LiveRamp). Save these requests in some instance variable / cache in your config producer class, and only query the db every time you deplete the onboard cache.

##### Why are there so many factory interfaces in Captain?

All of the default waypoint implementations in captain offer two ways of creating a waypoint:
1. Using factories (`RequestSubmitterFactory`, `HandlePersistorFactory`, `StatusRetrieverFactory`, etc)
2. Not using factories (`RequestSubmitter`, `HandlePersistor`, `StatusRetriever`, etc)

Each request that gets processed by captain will call the `create` method of the corresponding manifest factory. That means that all the resources that are used to call `create` will be used by each of those requests. That can be problematic. Let's say one of your submitter looks as follows:

```
DummySubmitter(ServiceClient1 serviceClient1, ServiceClient2 serviceClient2) {
this.serviceClient1 = serviceClient1;
this.serviceClient2 = serviceClient2;
}
```
Whenever you initialize that submitter, you need to already have the service clients initialized (let's assume these clients eagerly connect and have some sort of persistent connection), meaning that you'll already have made a connection to that service. Let's say that you've decided to not use factories. In addition, you have 10 steps in your manifest and all of them make connections to different services. That means that every time captain runs, every captain request will make a connection to each of those services, when actually, they only need to make connections to the services that their corresponding current step is going to use.

Letting you pass in factories is a handy way to avoid this, since when calling `create` you would just initialize the factories and not the underlying object. But still, depending on how you initialize submitters/status retrievers/ handle persistors using factories might not be necessary. Especially if you're trying to share connections across waypoints or manifests, you may push the depedency up your hierarchy in which case the factory is less likely to be valuable. Do what works for your infra.


### Gotchas, Notes on the Iface, Miscellania

- Captain is a busy loop. Be careful what you log. You can very quickly nuke your inbox or your disk.
- Captain is a busy loop. Be thoughtful about interactions with external resources (e.g. db interactions).
- Captain is a busy loop. It provides various controls to prevent it from spinning too fast (e.g. `setConfigWaitTime`). Most of the defaults selected for these controls are intended to provide safe usage. That being said, as you're implementing Captain, be mindful that the busy loop is "spinning" at a reasonable rate. 
- parallelExecutions: num of requests a captain node should process in parallel. (5 - 20 appears to be pretty conventional. Ideally each time Captain "processes" (meaning any single waypoint) a request the amount of works its doing should take less than a second. So even 5 threads can move you through requests very quickly) 

Did you know Captain is a busy loop?

#### Emulating a DAG

When people think of a workflow-engine, they think of a DAG. Captain grew organically out of a few products at LiveRamp that really only required linear workflows (even if at the time they were represented by complicated DAGs). Due to this pedigree, Captain is opinionated towards linear workflows to enforce building simple workflows. Thus DAGs are not first-class citizens in Captain. Probably the biggest roadblock for DAG's feeling at home within Captain is that choice that it leaves the entire persistence layer to the consumer. Persisting the state of a DAG is simply more complex than persisting the state of a linear workflow. Captain was not really built with the DAG in mind, though you can emulate it using Captain. I hypothesize there are some significant infrastructure hurdles (that are certainly solvable) to making it such that DAGs are easy to use in Captain. That being said, let's talk about how you can emulate a DAG in Captain today. 

A couple approaches:
1. Use optional steps. 

Check out the Optional Steps section to understand how to use them. In short, you can use optional steps to emulate a DAG by designing the predicates for the optional to route the request as you might in a DAG.
2. Reduce your DAG to a linear flow manually

Here's an example of how that might be done:
```
Imagine you have two waypoints that take a long time and don't have dependencies on each other, so you want to run them in parallel. Let's say these workflows are contained separately behind service 1 and service 2. You might do the following...

waypoint 1: sync waypoint with handle persistor to service 1. it saves service 1 request id.
waypoint 2: sync waypoint with handle persistor to service 2. it saves service 2 request id.
waypoint 3: flowcontrol waypoint that uses service 1 request id to determine if service 1 has completed work and uses service 2 request id to determine if service 2 has completed work. only when both are complete does it progress.
```

There's a pretty low ceiling for which this will be practical. In the example provided where you essentially just want two long running steps to happen in parallel instead of serially, it's pretty doable. 

3. Dynamically build manifests

Captain provides a `DefaultManifestImpl` which accepts a list of waypoints. That said, `Manifest` and `ManifestFactory` are both interfaces that any consumer can implement. So you can pretty much do anything you want here by implementing your manifest such that `getNextStep` returns a step from a DAG structure instead of a list. Your capacity to manage complexity is the limit on this. 

#### Optional Steps

All waypoints can accept an optional predicate. If that predicate evaluates to false, that waypoint will be skipped. 

#### Maintainers

(alphabetical)
@cgardens
@h-wang94 
@jtpefaur
