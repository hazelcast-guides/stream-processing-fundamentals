package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.*;

import java.io.IOException;

public class MachineStatus implements Portable {
    private String serialNumber;
    private short  averageBitTemp10s;
    private long eventTime;

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public short getAverageBitTemp10s() {
        return averageBitTemp10s;
    }

    public void setAverageBitTemp10s(short averageBitTemp10s) {
        this.averageBitTemp10s = averageBitTemp10s;
    }

    @Override
    public String toString() {
        return "MachineStatus{" +
                "serialNumber='" + serialNumber + '\'' +
                ", averageBitTemp10s=" + averageBitTemp10s +
                ", eventTime=" + eventTime +
                '}';
    }

    @Override
    public int getFactoryId() {
        return PortableHelper.MACHINE_SHOP_PORTABLE_FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return PortableHelper.MACHINE_STATUS_ID;
    }

    @Override
    public void writePortable(PortableWriter portableWriter) throws IOException {
        portableWriter.writeString("serialNumber", serialNumber);
        portableWriter.writeShort("averageBitTemp10s", averageBitTemp10s);
        portableWriter.writeLong("eventTime", eventTime);
    }

    @Override
    public void readPortable(PortableReader portableReader) throws IOException {
        this.serialNumber = portableReader.readString("serialNumber");
        this.averageBitTemp10s = portableReader.readShort("averageBitTemp10s");
        this.eventTime = portableReader.readLong("eventTime");
    }
}
