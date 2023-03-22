package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.map.impl.journal.MapEventJournal;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import hazelcast.platform.labs.machineshop.domain.MachineStatusSummary;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.io.Serializable;
import java.util.Map;

public class MachineStatusServicePipeline {

    private static String categorizeTemp(double temp, short warningLimit, short criticalLimit){
        String result;
        if (temp > (double) criticalLimit)
            result = "red";
        else if (temp > (double) warningLimit)
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

        // results is a Tuple3 (serialNum, warningTemp, criticalTemp)
        StreamStage<Tuple3<String, Short, Short>> results =
                pipeline.readFrom(Sources.<String, String>mapJournal(
                        inputMapName, JournalInitialPosition.START_FROM_CURRENT))
                        .withoutTimestamps()
                        .groupingKey( entry -> entry.getValue())
                        .<GenericRecord,Tuple3<String,Short, Short>>mapUsingIMap(Names.PROFILE_MAP_NAME, (entry, gr) -> Tuple3.tuple3(
                                entry.getValue(),
                                gr.getInt16("warningTemp"),
                                gr.getInt16("criticalTemp")
                        ));

        ServiceFactory<?, ClassDefinition> classDefinitionServiceFactory =
                ServiceFactories.sharedService(ctx -> MachineStatusSummary.CLASS_DEFINITION);

        results.mapUsingService(classDefinitionServiceFactory, (cdef, event) ->{
           Tuple2<String,GenericRecord> t2 = Tuple2.tuple2(
                   event.f0(),
                   GenericRecordBuilder.portable(cdef)
                           .setString("serialNumber", event.f0())
                           .setInt16("averageBitTemp10s", (short) 0)
                           .setInt16("warningTemp", event.f1())
                           .setInt16("criticalTemp", event.f2())
                           .setString("status", "green").build());
           return t2;
        }).writeTo(Sinks.map(outputMapName));

        return pipeline;
    }
    public static void main(String []args){
        Pipeline pipeline = createPipeline("IN", "OUT");

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("MachineStatusService");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
