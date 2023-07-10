# Overview

ACME operates 1000’s of machines.  Each publishes several data points each second. Measurements include things like
bit temperature and RPM. Breakage is expensive. We want to go beyond maintenance schedules and monitor the information
in real time. Each machine has its own parameters for acceptable bit temperature, which are stored in a
*machine_profiles* IMap.  If excessive bit temperatures are caught in time, breakage can be averted by immediately
reducing the cutting speed.  Our Pipeline will do this by sending "green" / "orange" / "red" signals to the machines
via the *machine_controls* IMap.  A schematic of the lab is shown below.

![schematic](resources/pipeline.png)

# Walk Through

- [ ] Deploy a Viridian cluster.  Navigate to the advanced setup 
tab and download the file containing the keys.  You will also need the 
other information that is on this tab.

    ![advanced_setup](resources/advanced_setup.png).

- [ ] Unzip the file, which will be called `hzcloud_xxxx_keys` and move the 
unpacked directory into the main project directory.

- [ ] Edit `viridian.env` according to the instructions in the file.
- [ ] Make sure everything is built: `mvn clean install`
- [ ] Start the local components. `docker compose -f viridian.compose.yaml up -d`. 
At this point you can view the UI at http://localhost:8050 and you can show the data using 
the SQL browser in Viridian. Try `select * from machine_profiles` and 
`select * from machine_events`
- [ ] **Open `cli/viridian_submitjob.sh` and edit it so that it will submit the 
solution and not the skeleton** 
- [ ] Submit the job: `docker compose -f viridian.compose.yaml run submit_job`


You should now see, on the UI, that the machines are bring actively controlled by
the Jet Pipeline.  You can also see the signals that are sent via the `machine_controls` map.



