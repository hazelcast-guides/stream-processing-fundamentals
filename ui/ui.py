import os
import sys
import threading

from hazelcast import HazelcastClient
from hazelcast.config import ReconnectMode
from hazelcast.proxy.base import EntryEvent
from hazelcast.proxy.map import BlockingMap
from hazelcast.serialization.api import CompactSerializer, CompactReader, CompactWriter


# the following environment variables are required
#
# HZ_SERVERS
# HZ_CLUSTER_NAME
#

class MachineStatusEvent:
    def __init__(self):
        self.serial_num = ""
        self.event_time = 0
        self.bit_rpm = 0
        self.bit_temp = 0
        self.bit_position_x = 0
        self.bit_position_y = 0
        self.bit_position_z = 0


class MachineStatusEventSerializer(CompactSerializer[MachineStatusEvent]):
    def read(self, reader: CompactReader) -> MachineStatusEvent:
        result = MachineStatusEvent()
        result.serial_num = reader.read_string("serial_num")
        result.event_time = reader.read_int64("event_time")
        result.bit_rpm = reader.read_int32("bit_rpm")
        result.bit_temp = reader.read_int16("bit_temp")
        result.bit_position_x = reader.read_int32("bit_position_x")
        result.bit_position_y = reader.read_int32("bit_position_y")
        result.bit_position_z = reader.read_int32("bit_position_z")
        return result

    def write(self, writer: CompactWriter, event: MachineStatusEvent) -> None:
        writer.write_string("serial_num", event.serial_num)
        writer.write_int64("event_time", event.event_time)
        writer.write_int32("bit_rpm", event.bit_rpm)
        writer.write_int16("bit_temp", event.bit_temp)
        writer.write_int32("bit_position_x", event.bit_position_x)
        writer.write_int32("bit_position_y", event.bit_position_y)
        writer.write_int32("bit_position_z", event.bit_position_z)

    def get_type_name(self) -> str:
        return "machine_status_event"

    def get_class(self):
        return MachineStatusEvent


def get_required_env(name: str) -> str:
    if name not in os.environ:
        sys.exit(f'Please provide the "{name} environment variable."')
    else:
        return os.environ[name]


def map_listener(expected_val: str, done: threading.Event):
    def inner_func(e: EntryEvent):
        if e.value == expected_val:
            done.set()

    return inner_func


def wait_for(imap: BlockingMap, expected_key: str, expected_val: str, timeout: float) -> bool:
    done = threading.Event()
    imap.add_entry_listener(
        include_value=True,
        key=expected_key,
        added_func=map_listener(expected_val, done),
        updated_func=map_listener(expected_val, done)
    )

    curr_val = imap.get(expected_key)
    if curr_val is not None and curr_val == expected_val:
        return True

    return done.wait(timeout)


if __name__ == '__main__':
    hz_cluster_name = get_required_env('HZ_CLUSTER_NAME')
    hz_servers = get_required_env('HZ_SERVERS').split(',')
    hz = HazelcastClient(
        cluster_name=hz_cluster_name,
        cluster_members=hz_servers,
        async_start=False,
        reconnect_mode=ReconnectMode.ON)

    event_map = hz.get_map('machine_events').blocking()
    system_activities_map = hz.get_map('system_activities').blocking()
    wait_for(system_activities_map, 'LOADER_STATUS', 'FINISHED', 3 * 60 * 1000)
    print("The loader has finished, proceeding")

    selected_serial_nums = hz.sql.execute(
        """SELECT serialNum FROM machine_profiles WHERE 
           location = 'Los Angeles' AND 
           block = 'A' """
    ).result()

    for row in selected_serial_nums:
        print(row['serialNum'])
