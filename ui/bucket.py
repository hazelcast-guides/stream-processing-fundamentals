import threading
import pandas as pd


class Bucket:

    # contents of the dict:
    # {
    #   "colname" : ( [templist], [timestamplist])
    # }
    #

    def __init__(self):
        self.lock = threading.Lock()
        self.data = dict()
        self.watermark = pd.to_datetime(0, unit='s')

    def add(self, sn: str, temp: int, event_epoch_time: int):
        with self.lock:
            t_event = pd.to_datetime(int(event_epoch_time/1000), unit='s')
            print(f"GOT {sn} {t_event} {temp}", flush=True)
            if t_event > self.watermark:
                self.watermark = t_event

            temps, times = self.data.setdefault(sn, ([], []))
            temps.append(temp)
            times.append(t_event)

    def harvest(self) -> pd.DataFrame:
        with self.lock:
            threshold = self.watermark - pd.Timedelta(4, "s")
            print(f"threshold is {threshold}", flush=True)
            new_data = dict()
            for k, v in self.data.items():
                temps = v[0]
                times = v[1]
                selected = [t < threshold for t in times]  #array of booleans
                temps = [t[0] for t in zip(temps, selected) if t[1]]
                times = [t[0] for t in zip(times, selected) if t[1]]
                new_data[k] = pd.Series(temps, times)

            new_df = pd.DataFrame(new_data)
            print(f'newdf is {new_df}', flush=True)
            self.data.clear()
            return new_df
