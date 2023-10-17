package hazelcast.platform.labs.machineshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import hazelcast.platform.labs.machineshop.domain.MachineStatusSummary;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.util.Map;
import java.util.Properties;

public class AggregationPipeline {

    public static Pipeline createPipeline(String kafkaBootsrapServers, String kafkaTopic){
        Pipeline pipeline = Pipeline.create();

        Properties kafkaConnectionProps = new Properties();
        kafkaConnectionProps.setProperty("bootstrap.servers", kafkaBootsrapServers);
        kafkaConnectionProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConnectionProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        StreamSource<Map.Entry<String, String>> kafkaSource =
                KafkaSources.kafka(kafkaConnectionProps, kafkaTopic);

        StreamStage<Map.Entry<String, String>> kafkaRecords =
                pipeline.readFrom(kafkaSource)
                        .withNativeTimestamps(2000)
                        .setName("read Kafka");

        ServiceFactory<?, ObjectMapper> objectMapperServiceFactory =
                ServiceFactories.sharedService(ctx -> new ObjectMapper());
        StreamStage<JsonNode> json = kafkaRecords.mapUsingService(objectMapperServiceFactory, (om, entry) ->  om.readTree(entry.getValue()))
                .setName("parse json");


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
        StreamStage<KeyedWindowResult<String, Double>> averageTemps =
                json.groupingKey(entry -> entry.get("serialNum").asText())
                    .window(WindowDefinition.tumbling(10000))
                    .aggregate(AggregateOperations.averagingLong(item -> item.get("bitTemp").asLong()))
                    .setName("Average Temps");

        // TODO - could I get away with not using GenericRecord here or will it cause class loading problems ?
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
                .writeTo(Sinks.map(Names.STATUS_SUMMARY_MAP_NAME));

        return pipeline;
    }

    // expects arguments: kafka bootstrap servers, kafka topic
    public static void main(String []args){
        if (args.length != 2){
            System.err.println("Please provide 2 arguments: kafka bootstrap servers and kafka topic");
            System.exit(1);
        }


        Pipeline pipeline = createPipeline(args[0], args[1]);
        pipeline.setPreserveOrder(false);   // nothing in here requires order
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Aggregator");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
