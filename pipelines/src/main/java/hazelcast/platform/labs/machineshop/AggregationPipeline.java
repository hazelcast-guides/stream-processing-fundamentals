package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import hazelcast.platform.labs.machineshop.domain.MachineStatusSummary;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.util.Map;

public class AggregationPipeline {

    public static Pipeline createPipeline(){
        Pipeline pipeline = Pipeline.create();

        /*
         * Read events from the "machine_events" map.  The key is serialNumber and the value is a GenericRecord
         * containing a MachineStatusEvent.
         */
        StreamStage<Map.Entry<String, GenericRecord>> statusEvents = pipeline.readFrom(
                        Sources.<String, GenericRecord>mapJournal(
                                Names.EVENT_MAP_NAME,
                                JournalInitialPosition.START_FROM_OLDEST))
                .withTimestamps(item -> item.getValue().getInt64("eventTime"), 1000)
                .setName("machine status events");

        /*
         * Group the events by serial number. For each serial number, compute the average temperature over a 10s
         * tumbling window.
         *
         * INPUT: Map.Entry<String, GenericRecord>
         *        The GenericRecord is a MachineStatusEvent. For the specific field names, see the comment
         *        at the top of this class.
         *
         * OUTPUT: KeyedWindowResult<String,Double>
         *       The key is the serial number and the value is the average temperature
         *
         */
        StreamStage<KeyedWindowResult<String, Double>> averageTemps = statusEvents
                .groupingKey( entry -> entry.getValue().getString("serialNum"))
                .window(WindowDefinition.tumbling(10000))
                .aggregate(AggregateOperations.averagingLong(item -> item.getValue().getInt16("bitTemp")))
                .setName("Average Temps");

        // TODO - could I get away with not using GenericRecord here since or will it cause class loading problems ?
        //        I will never read this on the server side so it might be OK

        /*
         * Make a MachineStatusSummary GenericRecord out of the event.
         */

        ServiceFactory<?, ClassDefinition> classDefinitionServiceFactory = ServiceFactories.sharedService(ctx -> MachineStatusSummary.CLASS_DEFINITION);

        averageTemps.mapUsingService(classDefinitionServiceFactory,
                        (cdef, kwr) -> GenericRecordBuilder.portable(cdef)
                                .setString("serialNumber", kwr.getKey())
                                .setInt16("averageBitTemp10s", kwr.getValue().shortValue()).build())
                .map(mss -> Tuple2.tuple2(mss.getString("serialNumber"), mss))
                .writeTo(Sinks.<String, GenericRecord>map(Names.STATUS_SUMMARY_MAP_NAME));

        return pipeline;
    }
    public static void main(String []args){
        Pipeline pipeline = createPipeline();
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Aggregator");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
