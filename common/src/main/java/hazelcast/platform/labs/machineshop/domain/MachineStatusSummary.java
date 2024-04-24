package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.*;

import java.io.IOException;

public class MachineStatusSummary implements Portable {
    private String serialNumber;
    private short  averageBitTemp10s;
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
        return "MachineStatusSummary{" +
                "serialNumber='" + serialNumber + '\'' +
                ", averageBitTemp10s=" + averageBitTemp10s +
                '}';
    }

    public static final int ID = 3;

    @Override
    public int getFactoryId() {
        return MachineShopPortableFactory.ID;
    }

    @Override
    public int getClassId() {
        return MachineStatusSummary.ID;
    }

    @Override
    public void writePortable(PortableWriter portableWriter) throws IOException {
        portableWriter.writeString("serialNumber", serialNumber);
        portableWriter.writeShort("averageBitTemp10s", averageBitTemp10s);
    }

    @Override
    public void readPortable(PortableReader portableReader) throws IOException {
        this.serialNumber = portableReader.readString("serialNumber");
        this.averageBitTemp10s = portableReader.readShort("averageBitTemp10s");
    }

    public static ClassDefinition CLASS_DEFINITION =
            new ClassDefinitionBuilder(MachineShopPortableFactory.ID, MachineStatusSummary.ID)
                    .addStringField("serialNumber")
                    .addShortField("averageBitTemp10s")
                    .build();
}
