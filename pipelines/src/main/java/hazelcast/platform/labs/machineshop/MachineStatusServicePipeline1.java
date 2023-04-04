package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import hazelcast.platform.labs.machineshop.domain.Names;
import hazelcast.platform.labs.machineshop.domain.StatusServiceResponse;

import java.util.Map;

public class MachineStatusServicePipeline1 {

    public static Pipeline createPipeline(String inputMapName, String outputMapName){

        // TODO  Using GenericRecord in these Pipelines is ugly.  However, if any of the events have to cross
        // nodes, they will be serialized and deserialized, at which point, items that were portable or compact
        // will become GenericRecords.  What can be done about this ?  Maybe generate a GenericRecord wrapper ?


        Pipeline pipeline = Pipeline.create();

        // results is a Map.Entry<String, String> The key is the requestId and the value is the serialNumber

        StreamStage<Map.Entry<String,String>> requests = pipeline
                .readFrom(Sources.<String, String>mapJournal(inputMapName, JournalInitialPosition.START_FROM_CURRENT))
                .withoutTimestamps();

        // now look up the current average temperature and append that to the input event
        // returns tuple (requestId, serialNum, averageTemp)
        StreamStage<Tuple3<String,String, Short>> rawdata = requests
                .groupingKey(Map.Entry::getValue)
                .<GenericRecord, Tuple3<String, String, Short>>mapUsingIMap(
                        Names.STATUS_SUMMARY_MAP_NAME,
                        (request, profile) -> Tuple3.tuple3(
                                request.getKey(),
                                request.getValue(),
                                profile != null ? profile.getInt16("averageBitTemp10s") : -1 ));

        // now format the response as a StatusServiceResponse GenericRecord
        ServiceFactory<?, ClassDefinition> classDefinitionServiceFactory =
                ServiceFactories.sharedService(ctx -> StatusServiceResponse.CLASS_DEFINITION);

        // package up the results into a GenericRecord of StatusServiceResponse and write to the output map
        rawdata.mapUsingService(classDefinitionServiceFactory, (cdef, raw) ->{
            String requestId = raw.f0();
            String sn = raw.f1();
            Short averageTemp =  raw.f2();
           return Tuple2.tuple2(
                   requestId,
                   GenericRecordBuilder.portable(cdef)
                           .setString("serialNumber", sn)
                           .setInt16("averageBitTemp10s", averageTemp)
                           .setInt16("warningTemp", (short) 0)
                           .setInt16("criticalTemp", (short) 0)
                           .setString("status", "unknown")
                           .build());
        }).writeTo(Sinks.map(outputMapName));

        return pipeline;
    }
    public static void main(String []args){
        if (args.length != 2){
            System.err.println("Please supply exactly 2 parameters: input map name, output map name");
            System.exit(1);
        }
        Pipeline pipeline = createPipeline(args[0], args[1]);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("MachineStatusService");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
