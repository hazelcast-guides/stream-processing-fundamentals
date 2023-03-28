import logging
import os
import random
import sys
import threading

import locust
from hazelcast import HazelcastClient
from hazelcast.config import ReconnectMode
from hazelcast.proxy.base import EntryEvent
from hazelcast.proxy.map import BlockingMap

import viridian


# TODO most of the Hazelcast related code here should be pulled out into a common module
def get_required_env(name: str) -> str:
    if name not in os.environ:
        sys.exit(f'Please provide the "{name} environment variable."')
    else:
        return os.environ[name]


def wait_map_listener_fun(expected_val: str, done: threading.Event):
    def inner_func(e: EntryEvent):
        if e.value == expected_val:
            print("event with expected value observed", flush=True)
            done.set()

    return inner_func


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


def connect_to_hazelcast() -> HazelcastClient:
    if viridian.viridian_config_present():
        hz = viridian.configure_from_environment(async_start=False,
                                                 reconnect_mode=ReconnectMode.ON)
    else:
        hz_cluster_name = get_required_env('HZ_CLUSTER_NAME')
        hz_servers = get_required_env('HZ_SERVERS').split(',')
        hz = HazelcastClient(
            cluster_name=hz_cluster_name,
            cluster_members=hz_servers,
            async_start=False,
            reconnect_mode=ReconnectMode.ON
        )

    return hz


logging.basicConfig(level=logging.INFO)
hz = connect_to_hazelcast()
print('Connected to Hazelcast', flush=True)
system_activities_map = hz.get_map('system_activities').blocking()
profile_map = hz.get_map('machine_profiles').blocking()
wait_for(system_activities_map, 'LOADER_STATUS', 'FINISHED', 3 * 60 * 1000)
serial_numbers = profile_map.key_set()
print(f'Retrieved {len(serial_numbers)} serial numbers')
hz.shutdown()
print('Disconnected from Hazelcast', flush=True)


class TestUser(locust.HttpUser):
    wait_time = locust.constant_throughput(1)

    @locust.task
    def get_random_status(self):
        sn = random.choice(serial_numbers)
        self.client.get(f'/machinestatus?sn={sn}', name='machinestatus?sn=[sn]')
