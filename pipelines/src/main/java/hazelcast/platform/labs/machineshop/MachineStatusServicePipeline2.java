package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.datamodel.Tuple5;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import hazelcast.platform.labs.machineshop.domain.Names;
import hazelcast.platform.labs.machineshop.domain.StatusServiceResponse;

import java.util.Map;

public class MachineStatusServicePipeline2 {

    private static String categorizeTemp(short temp, short warningLimit, short criticalLimit){
        String result;
        if (temp >  criticalLimit)
            result = "red";
        else if (temp >  warningLimit)
            result = "orange";
        else
            result = "green";

        return result;
    }

    /*
     * Write your Pipeline here.
     *
     * DataStructureDefinitions
     *
     *   GenericRecord of MachineStatusEvent;
     *     GenericRecord machineEvent;
     *     String serialNum = machineEvent.getString("serialNum);
     *     long eventTime = machineEvent.getInt64("eventTime");
     *     short bitTemp = machineEvent.getInt16("bitTemp");
     *
     *   GenericRecord of MachineProfile
     *     GenericRecord profile;
     *     String serialNum = profile.getString("serialNum");
     *     String location = profile.getString("location");
     *     String block = profile.getString("block");
     *     short warningTemp = profile.getInt16("warningTemp");
     *     short criticalTemp = profile.getInt16("criticalTemp");
     *
     * Useful References:
     *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStage.html
     */
    public static Pipeline createPipeline(String inputMapName, String outputMapName){
        // TODO  Using GenericRecord in these Pipelines is ugly.  However, if any of the events have to cross
        // nodes, they will be serialized and deserialized, at which point, items that were portable or compact
        // will become GenericRecords.  What can be done about this ?  Maybe generate a GenericRecord wrapper ?


        Pipeline pipeline = Pipeline.create();

        // results is a Tuple4 (requestId, serialNum, warningTemp, criticalTemp)
        StreamStage<Tuple4<String, String, Short, Short>> limits =
                pipeline.readFrom(Sources.<String, String>mapJournal(
                        inputMapName, JournalInitialPosition.START_FROM_CURRENT))
                        .withoutTimestamps()
                        .groupingKey(Map.Entry::getValue)
                        .<GenericRecord,Tuple4<String, String,Short, Short>>mapUsingIMap(
                                Names.PROFILE_MAP_NAME,
                                (entry, gr) -> Tuple4.tuple4(
                                        entry.getKey(),
                                        entry.getValue(),
                                        gr != null ? gr.getInt16("warningTemp") : 0,
                                        gr != null ? gr.getInt16("criticalTemp"): 0
                        ));

        // now look up the current average temperature and append that to the input event
        // returns tuple (requestId, serialNum, warningTemp, criticalTemp, averageTemp)
        StreamStage<Tuple5<String,String, Short, Short, Short>> rawdata =
                limits.<String, GenericRecord, Tuple5<String, String, Short, Short, Short>>mapUsingIMap(
                        Names.STATUS_SUMMARY_MAP_NAME,
                        Tuple4::f1,
                        (t4, summary) -> Tuple5.tuple5(
                                t4.f0(),
                                t4.f1(),
                                t4.f2(),
                                t4.f3(),
                                summary != null ? summary.getInt16("averageBitTemp10s") : -1
        ));

        // now format the response as a StatusServiceResponse GenericRecord
        ServiceFactory<?, ClassDefinition> classDefinitionServiceFactory =
                ServiceFactories.sharedService(ctx -> StatusServiceResponse.CLASS_DEFINITION);

        // package up the results into a GenericRecord of StatusServiceResponse and write to the output map
        rawdata.mapUsingService(classDefinitionServiceFactory, (cdef, event) ->{
            String requestId = event.f0();
            String sn = event.f1();
            Short warningTemp = event.f2();
            Short criticalTemp = event.f3();
            Short averageTemp =  event.f4();
            String status = categorizeTemp(averageTemp, warningTemp, criticalTemp);
           return Tuple2.tuple2(
                   requestId,
                   GenericRecordBuilder.portable(cdef)
                           .setString("serialNumber", sn)
                           .setInt16("averageBitTemp10s", averageTemp)
                           .setInt16("warningTemp", warningTemp)
                           .setInt16("criticalTemp", criticalTemp)
                           .setString("status", status).build());
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
