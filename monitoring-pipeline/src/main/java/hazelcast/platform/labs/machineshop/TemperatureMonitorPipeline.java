package hazelcast.platform.labs.machineshop;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import hazelcast.platform.labs.machineshop.domain.Names;

import java.io.Serializable;
import java.util.Map;

public class TemperatureMonitorPipeline {

    /*
     * Used for stateful filtering.  Must be serializable as it is included in stream snapshot
     */
    public static class CurrentState implements Serializable {
        private String color;

        public CurrentState(){
            color="";
        }

        public String getColor(){
            return color;
        }

        public void setColor(String color){
            this.color = color;
        }

    }

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
         * At any time, you can add a logging sink to a stream stage to examine the contents.  Remove it when you
         * are done testing.
         */
        statusEvents.writeTo(Sinks.logger( event -> "New Event SN=" + event.getValue().getString("serialNum")));

        /*
         * Group the events by serial number. For each serial number, compute the average temperature over a 10s
         * tumbling window.
         *
         * The general template for aggregation looks like this:
         *
         * KeyedWindowResult result = statusEvents.groupingKey( GET KEY LAMBDA )
         *                                  .window( WINDOW DEFINITION )
         *                                  .aggregate(AggregateOperations.averagingLong( GET BIT TEMP LAMBDA);
         *
         * See:
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/WindowDefinition.html
         *   https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/aggregate/AggregateOperations.html
         *
         */
        StreamStage<KeyedWindowResult<String, Double>> averageTemps = null;

        /*
         * Look up the machine profile for this machine from the machine_profiles map.  Output a
         * 4-tuple (serialNum, avg temp, warning temp, critical_temp)
         *
         * Note that the incoming event already has a key associated with it (serial number).  That same
         * key will automatically be used to do the lookup on the machine_profile map. The general form is
         * averageTemps.mapUsingIMap( (p, w) -> LAMBDA RETURNING Tuple4)  where p is a MachineProfile as
         * GenericRecord and w is the KeyedWindowResult from the previous stage.
         *
         * See:
         *   "mapUsingIMap" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/KeyedWindowResult.html
         *    https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/datamodel/Tuple4.html
         */
        StreamStage<Tuple4<String,Double,Short,Short>> temperaturesAndLimits = null;

        /*
         * Using a simple "map" stage, categorize the temperature as "green", "red" or "orange" and
         * return a Tuple2 (serialNum, color).
         *
         * See:
         *   the "categorizeTemp" function at the top of this file
         *   "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStage.html
         */
        StreamStage<Tuple2<String,String>> labels = null;

        /*
         * We only want to write to the output map if the current color has changed.  This prevents flooding the
         * map listeners with irrelevant events.  We can use   StreamStageWithKey.filterStateful to do this.
         * The filter will remember the last value for each key.  The CurrentState class in this file should be
         * used to hold the remembered value.
         *
         * The solution will look like this:
         *   labels.groupingKey(GET SERIAL NUM LAMBDA).filterStateful(CurrentState::new, (cs, labels) -> FILTER LAMBDA)
         *   Where "cs" is the instance of current state related to this key and "labels" is the Tuple2 event from
         *   the previous stage.
         *
         * See:
         *    "filterStateful" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/StreamStageWithKey.html
         */
        StreamStage<Tuple2<String,String>> changedLabels = null;

        /*
         * Finally, we can sink the results directly to the "machine_controls" map.  Tuple2<K,V> also implements
         * Map.Entry<K,V> so we can just supply this directly to the IMap Sink.
         *
         * Create a Sink using Sinks.map (see reference) then finish the pipeline with
         * changedLabels.writeTo(mySink);
         *
         * See:
         *    "map" in https://docs.hazelcast.org/docs/5.2.0/javadoc/index.html?com/hazelcast/jet/pipeline/Sinks.html
         */
        Sink<Map.Entry<String,String>> sink = null;

        return pipeline;
    }
    public static void main(String []args){
        Pipeline pipeline = createPipeline();
        pipeline.setPreserveOrder(true);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Temperature Monitor");
        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(pipeline, jobConfig);
    }
}
