import os
import sys
import threading
import time

from hazelcast import HazelcastClient
from hazelcast.config import ReconnectMode
from hazelcast.proxy.base import EntryEvent
from hazelcast.proxy.map import BlockingMap
from hazelcast.serialization.api import CompactSerializer, CompactReader, CompactWriter
import hazelcast.predicate


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
        mse = MachineStatusEvent()
        mse.serial_num = reader.read_string("serialNum")
        mse.event_time = reader.read_int64("eventTime")
        mse.bit_rpm = reader.read_int32("bitRPM")
        mse.bit_temp = reader.read_int16("bitTemp")
        mse.bit_position_x = reader.read_int32("bitPositionX")
        mse.bit_position_y = reader.read_int32("bitPositionY")
        mse.bit_position_z = reader.read_int32("bitPositionZ")
        return mse

    def write(self, writer: CompactWriter, event: MachineStatusEvent) -> None:
        writer.write_string("serialNum", event.serial_num)
        writer.write_int64("eventTime", event.event_time)
        writer.write_int32("bitRPM", event.bit_rpm)
        writer.write_int16("bitTemp", event.bit_temp)
        writer.write_int32("bitPositionX", event.bit_position_x)
        writer.write_int32("bitPositionY", event.bit_position_y)
        writer.write_int32("bitPositionZ", event.bit_position_z)

    def get_type_name(self) -> str:
        return "machine_status_event"

    def get_class(self):
        return MachineStatusEvent


def get_required_env(name: str) -> str:
    if name not in os.environ:
        sys.exit(f'Please provide the "{name} environment variable."')
    else:
        return os.environ[name]


# returns a map listener that listens for a certain value using closure
def wait_map_listener_fun(expected_val: str, done: threading.Event):
    def inner_func(e: EntryEvent):
        if e.value == expected_val:
            print("event with expected value observed", flush=True)
            done.set()

    return inner_func


def logging_entry_listener(entry: EntryEvent):
    print(f'GOT {entry.key}:{entry.value}', flush=True)


def wait_for(imap: BlockingMap, expected_key: str, expected_val: str, timeout: float) -> bool:
    done = threading.Event()
    imap.add_entry_listener(
        include_value=True,
        key=expected_key,
        added_func=wait_map_listener_fun(expected_val, done),
        updated_func=wait_map_listener_fun(expected_val, done)
    )

    curr_val = imap.get(expected_key)
    if curr_val is not None and curr_val == expected_val:
        return True

    return done.wait(timeout)



# Run this app with `python app.py` and
# visit http://127.0.0.1:8050/ in your web browser.

from dash import Dash, html, dcc
import plotly.express as px
import pandas as pd

app = Dash(__name__)

# assume you have a "long-form" data frame
# see https://plotly.com/python/px-arguments/ for more options
df = pd.DataFrame({
    "Fruit": ["Apples", "Oranges", "Bananas", "Apples", "Oranges", "Bananas"],
    "Amount": [4, 1, 2, 2, 4, 5],
    "City": ["SF", "SF", "SF", "Montreal", "Montreal", "Montreal"]
})

fig = px.bar(df, x="Fruit", y="Amount", color="City", barmode="group")

app.layout = html.Div(children=[
    html.H1(children='Hello Dash'),

    html.Div(children='''
        Dash: A web application framework for your data.
    '''),

    dcc.Graph(
        id='example-graph',
        figure=fig
    )
])

if __name__ == '__main__':
    hz_cluster_name = get_required_env('HZ_CLUSTER_NAME')
    hz_servers = get_required_env('HZ_SERVERS').split(',')
    hz = HazelcastClient(
        cluster_name=hz_cluster_name,
        cluster_members=hz_servers,
        async_start=False,
        reconnect_mode=ReconnectMode.ON,
        compact_serializers=[MachineStatusEventSerializer()]
    )
    print('Connected to Hazelcast', flush=True)

    machine_controls_map = hz.get_map('machine_controls').blocking()
    event_map = hz.get_map('machine_events').blocking()
    system_activities_map = hz.get_map('system_activities').blocking()
    wait_for(system_activities_map, 'LOADER_STATUS', 'FINISHED', 3 * 60 * 1000)
    print("The loader has finished, proceeding", flush=True)

    selected_serial_nums = hz.sql.execute(
        """SELECT serialNum FROM machine_profiles WHERE
           location = 'Los Angeles' AND
           block = 'A' """
    ).result()

    sn_list = "','".join([r["serialNum"] for r in selected_serial_nums])
    query = f"serialNum in ('{sn_list}')"
    print(f'adding entry listener WHERE {query}', flush=True)
    event_map.add_entry_listener(
        include_value=True,
        predicate=hazelcast.predicate.sql(query),
        added_func=logging_entry_listener,
        updated_func=logging_entry_listener)

    print("Listener added", flush=True)

    app.run_server(debug=True)
