# Change: Extra gauge/DPF sensors + estimated fuel rate

## Why

Session review on 2026-09-12 (log + screenshot) surfaced several gaps: the Zegary tab's THR tile always read ~81% (turned out to be the intake throttle/EGR valve, not the driver's accelerator pedal — SAE PID `0x11` is not equivalent to pedal position on this diesel); FUEL/INST tiles were always empty because Mode 01 PID `5E` (fuel rate) returns `NO DATA` on this ECU; small tile fonts were hard to read while driving; the RPM arc had no label and could be mistaken for a speed gauge; and the DPF tab was only using 6 of the 10 Mode 22 DIDs already confirmed responsive by the existing discovery probe.

## What Changes

- Add SAE Mode 01 accelerator pedal position (`0x49`) as a new **GAS** tile, giving a real pedal-input reading alongside the existing throttle/EGR-valve tile.
- Add a wide set of standard Mode 01 PIDs to `GaugePids.pollList` for diag-log coverage: engine load, intake MAP, intake air temp, MAF, timing advance, fuel level, run time since start, barometric pressure, ambient air temp, relative accelerator pedal, commanded EGR, EGR error.
- Add an approximate fuel-rate estimate derived from MAF (fixed diesel AFR assumption) used only when the direct PID `5E` reading is `NO DATA`; FUEL/INST tiles show a `~` prefix when the value is estimated rather than measured.
- Add 5 more Mode 22 DPF DIDs already confirmed responsive via the discovery probe (`223276` km since DPF replace, `223278` avg km between regen, `22327A` avg regen duration, `223047` interrupted regen count, `223035` DPF ΔP sensor voltage) to `DpfPids.pollList` for logging.
- Increase MiniBar/Pill tile font sizes on the Zegary dashboard for readability while driving.
- Label the RPM arc on the gauge cluster to avoid it being read as a speed indicator.

## Capabilities

- Modified: `dpf`
- Modified: `sensor-search` (gauge poll list feeds diag logging shared with the sensor catalog)
