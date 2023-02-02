## Getting to Know the Lab Environment

_Note:the best way to view this guide is directly on GitHub at https://github.com/hazelcast-guides/stream-processing-fundamentals_.

Run the following commands to start your lab environment.
```shell
mvn install
docker compose up -d
```
Run `docker compose ps` to verify you have 5 services up and running.  You 
may also see a 6th service called `refdata_loader` which exits after it has 
loaded some `MachineProfile` objects into the `machine_profiles` map. The 
output should look like this:

```shell
NAME                                               IMAGE                               COMMAND                  SERVICE             CREATED             STATUS              PORTS
stream-processing-fundamentals-cli-1               hazelcast/hazelcast:5.2.1           "tail -f"                cli                 4 minutes ago       Up 4 minutes        5701/tcp
stream-processing-fundamentals-event_generator-1   openjdk:11                          "java -jar /project/…"   event_generator     4 minutes ago       Up 4 minutes
stream-processing-fundamentals-hz-1                hazelcast/hazelcast:5.2.1           "hz start"               hz                  4 minutes ago       Up 4 minutes        5701/tcp
stream-processing-fundamentals-mc-1                hazelcast/management-center:5.2.1   "bash ./bin/mc-start…"   mc                  4 minutes ago       Up 4 minutes        8081/tcp, 0.0.0.0:8080->8080/tcp, 8443/tcp
stream-processing-fundamentals-ui-1                stream-processing-fundamentals-ui   "/bin/sh -c 'python3…"   ui                  4 minutes ago       Up 4 minutes        5701/tcp, 0.0.0.0:8050->8050/tcp
```

Each service is described briefly below.

### hz
A Hazelcast node

### mc
Hazelcast Management Center. You can acess the UI at http://localhost:8080.

### event_generator
This is a java program that emulates traffic from a set of machines. It puts `MachineStatusEvent` objects into the 
`machine_events` map.  It also listens for control signals on the `machine_controls` map.

### ui
This is a Python program that allows you to monitor the bit temperature of a selected group of machines.  It listens 
on the `machine_events` map using a predicate to receive only events for the selected machine. You can access the UI 
at http://localhost:8050.  The machines are grouped by "Location" and "Block". To see a set of machines enter values 
for location and block (just hit enter or tab out of the fields, there is no button).  Valid locations are 
"San Antonio", and "Los Angeles".  Valid blocks are "A" and "B".  _The simulation has been programmed so that machines 
in block "A" are very likely to run hot._  

![UI](resources/ui.png)

### cli
This container is only used for deploying jobs to the cluster.  

## Get to Know the Data

Data in Hazelcast clusters can be accessed via SQL.  For more details, see https://docs.hazelcast.com/hazelcast/latest/sql/get-started-sql

Open up the management center (http://localhost:8080) and click on the "SQL Browser" button.

![MC](resources/MC_SQL.png)

_Machine Profiles_

Run `select * from machine_profiles` in the SQL Browser.
You will see that for each machine there is a serial number, information about the location and the warning and 
critical temperature limits for that particulare machine.
![profiles](resources/profiles.png)

_Machine Events_

Run `select * from machine_events`

![events](resources/machine_events.png)

This is the actual data coming from the machines.  

__Note:__ At any given time, only the latest event for each serial number 
appears in the map, however, the update events are all recorded into something called a _map journal_ that can be 
used as input to a Pipeline.  You can verify that the map is constantly being updated by navigating to the 
_machine\_events_ map in Management Center and observing the number of put operations.

![Puts](resources/puts_and_entries.png)

## Generic Records and Tuples

It is a best practice to avoid using POJOs in Pipelines if that POJO will be stored in a Hazelcast map.  It can cause 
class loading problems.  When using _Compact_ or _Portable_ serialization, Hazelcast automatically translates POJOs 
into _GenericRecord_ when accessed on the server side. 

In this lab, you will be working with _MachineProfile_ objects and _MachineEvents_, both of which are defined in 
 `common/src/main/java`.  However, those classes are not deployed with your job.  Intead , you will need to access 
them using GenericRecord.  For example
```java
GenericRecord machineEvent;
short bitTemperature = machineEvent.getInt16("bitTemp");
```
For more information on GenericRecord and accessing domain objects without domain classes see
https://docs.hazelcast.com/hazelcast/5.2/clusters/accessing-domain-objects.

It's common as data proceeds through your pipeline that its shape changes.  For example, you may look up the warning 
and critical temperatures for a particular machine and send them along with the original event to the next stage
in the pipeline.  There is no need to create special container classes for situations like this, you can use Tuples 
instead.  Here is an example.

```java
// create a 3-tuple that consists of the serial number and bit temperature from the event 
// and the warning temperature from the machine profile

GenericRecord p;
GenericRecord e;

Tuple3<String,Short,Short> newEvent = 
        Tuple3.tuple3(e.getString("serialNum"), e.getShort("bitTemp"), p.getShort("warningTemp"));

// now, if we want to access fields from the 3-tuple, we use f0(), f1() and f2()
short bitTemp = newEvent.f1();
```

## Deploy Your First Job

Open up  the `hazelcast.platform.solutions.machineshop.TemperatureMonitorPipeline` class in the `monitoring-pipeline` 
project and review the code there.  

The main method, shown below, is boilerplate that helps with deploying the job to a cluster.  
```java
    public static void main(String []args){
        Pipeline pipeline = createPipeline();
        pipeline.setPreserveOrder(true);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Temperature Monitor");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
```

You will do all of your work in the `createPipeline` method of this job. It always starts with creating a `Pipeline` 
object.  You then build up the Pipeline by adding stages to it.

```java
   public static Pipeline createPipeline(){
        Pipeline pipeline = Pipeline.create();
        // add your stages here
        return pipeline;
   }
```

__Notes:__ 
- The maven project uses the shade plugin to bundle all project dependencies but does not bundle hazelcast 
since those classes are already on the server. 
- Code with `com.hazelcast.*` package names cannot be deployed to a Viridian cluster.

### first iteration ...
It's time to modify and deploy our job.

Modify the logging output in the `TemperatureMonitorPipeline` and build the `monitoring-pipeline` project.
```shell
cd monitoring-pipeline
mvn package
```

deploy it: `submitjob.sh`

look for the log output in the Hazelcast logs: `docker compose logs hz`. You should 
see something like this:

```shell
stream-processing-fundamentals-hz-1  | 2023-02-01 21:11:44,357 [ INFO] [hz.hungry_lehmann.jet.blocking.thread-0] [c.h.j.i.c.WriteLoggerP]: [172.19.0.5]:5701 [dev] [5.2.1] [temp_monitor_161114/loggerSink#0] New Event SN=HYV569
stream-processing-fundamentals-hz-1  | 2023-02-01 21:11:44,370 [ INFO] [hz.hungry_lehmann.jet.blocking.thread-0] [c.h.j.i.c.WriteLoggerP]: [172.19.0.5]:5701 [dev] [5.2.1] [temp_monitor_161114/loggerSink#0] New Event SN=FXQ058
stream-processing-fundamentals-hz-1  | 2023-02-01 21:11:44,591 [ INFO] [hz.hungry_lehmann.jet.blocking.thread-0] [c.h.j.i.c.WriteLoggerP]: [172.19.0.5]:5701 [dev] [5.2.1] [temp_monitor_161114/loggerSink#0] New Event SN=RUO239
stream-processing-fundamentals-hz-1  | 2023-02-01 21:11:44,640 [ INFO] [hz.hungry_lehmann.jet.blocking.thread-0] [c.h.j.i.c.WriteLoggerP]: [172.19.0.5]:5701 [dev] [5.2.1] [temp_monitor_161114/loggerSink#0] New Event SN=DYQ714
```

Inspect the running job using the mangement center and, when you are done, cancel it.  The Hazelcast cluster will 
remain up and events will continue to flow.

![first job](resources/firstjob.png)

_Congratulations!  You've deployed your first pipeline_

### continue ..

Continue building the pipeline following the instructions in `TemperatureMonitorPipeline.java`

When you are done, look at the UI.  You should be able to tell that your job is now controlling the machines.

![job done](resources/jobdone.png)

## The End








