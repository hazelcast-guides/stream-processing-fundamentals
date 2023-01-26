package hazelcast.platform.solutions.machineshop.domain;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class MachineStatusEvent {
    private String serialNum;
    private long eventTime;
    private int bitRPM;
    private short bitTemp;
    private int bitPositionX;
    private int bitPositionY;
    private int bitPositionZ;

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public int getBitRPM() {
        return bitRPM;
    }

    public void setBitRPM(int bitRPM) {
        this.bitRPM = bitRPM;
    }

    public short getBitTemp() {
        return bitTemp;
    }

    public void setBitTemp(short bitTemp) {
        this.bitTemp = bitTemp;
    }

    public int getBitPositionX() {
        return bitPositionX;
    }

    public void setBitPositionX(int bitPositionX) {
        this.bitPositionX = bitPositionX;
    }

    public int getBitPositionY() {
        return bitPositionY;
    }

    public void setBitPositionY(int bitPositionY) {
        this.bitPositionY = bitPositionY;
    }

    public int getBitPositionZ() {
        return bitPositionZ;
    }

    public void setBitPositionZ(int bitPositionZ) {
        this.bitPositionZ = bitPositionZ;
    }

    @Override
    public String toString() {
        return "MachineStatusEvent{" +
                "serialNum='" + serialNum + '\'' +
                ", timestamp=" + eventTime +
                ", bitRPM=" + bitRPM +
                ", bitTemp=" + bitTemp +
                ", bitPositionX=" + bitPositionX +
                ", bitBitPositionY=" + bitPositionY +
                ", bitPositionZ=" + bitPositionZ +
                '}';
    }

    public static class Serializer implements CompactSerializer<MachineStatusEvent> {

        @Override
        public MachineStatusEvent read(CompactReader reader) {
            MachineStatusEvent result = new MachineStatusEvent();
            result.setSerialNum(reader.readString("serialNum"));
            result.setEventTime(reader.readInt64("eventTime"));
            result.setBitRPM(reader.readInt32("bitRPM"));
            result.setBitTemp(reader.readInt16("bitTemp"));
            result.setBitPositionX(reader.readInt32("bitPositionX"));
            result.setBitPositionY(reader.readInt32("bitPositionY"));
            result.setBitPositionZ(reader.readInt32("bitPositionZ"));
            return result;
        }

        @Override
        public void write(CompactWriter writer, MachineStatusEvent object) {
            writer.writeString("serialNum", object.getSerialNum());
            writer.writeInt64("eventTime", object.getEventTime());
            writer.writeInt32("bitRPM", object.getBitRPM());
            writer.writeInt16("bitTemp", object.getBitTemp());
            writer.writeInt32("bitPositionX", object.getBitPositionX());
            writer.writeInt32("bitPositionY", object.getBitPositionY());
            writer.writeInt32("bitPositionZ", object.getBitPositionZ());
        }

        @Override
        public String getTypeName() {
            return "machine_status_event";
        }

        @Override
        public Class<MachineStatusEvent> getCompactClass() {
            return MachineStatusEvent.class;
        }
    }
}
