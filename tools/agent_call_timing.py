"""Time an agent-style call (adb shell cmd app_function execute-app-function) to the sample app
when its process is dead (cold), alive (warm), and dead with a heavy Application.onCreate.

Usage: python3 tools/agent_call_timing.py [runs]   (Android 17 device/emulator, debug build installed)

Times are measured on the host around the adb command, so they include adb's own round trip.
Compare the scenarios with each other, not with numbers from a different setup.
Prints every run plus the median."""
import json, os, statistics, subprocess, sys, time

ADB = os.environ.get("ADB", "adb")
PKG = "com.promode.agenticandroid.shopping"
FN = f"{PKG}.BaseShoppingFunctionService#getItems"


def adb(*args, check=True):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=check).stdout


def call():
    t0 = time.perf_counter()
    out = adb("shell", "cmd", "app_function", "execute-app-function", "--package", PKG,
              "--function", FN, "--parameters", "'{}'")
    return (time.perf_counter() - t0) * 1000, out


def set_heavy(enabled):
    import base64
    xml = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
           f"    <boolean name=\"heavy_init\" value=\"{'true' if enabled else 'false'}\" />\n</map>\n")
    b64 = base64.b64encode(xml.encode()).decode()
    adb("shell", "am", "force-stop", PKG)
    adb("shell", f"run-as {PKG} sh -c 'mkdir -p shared_prefs && echo {b64} | base64 -d > shared_prefs/demo_settings.xml'")
    got = adb("shell", f"run-as {PKG} cat shared_prefs/demo_settings.xml")
    assert f'value="{str(enabled).lower()}"' in got and "version='1.0'" in got, got


def pid():
    return adb("shell", "pidof", PKG, check=False).strip() or "-"


def run(label, runs, cold, heavy):
    set_heavy(heavy)
    if not cold:
        call()  # start the process once (untimed) so every timed run is warm
    times = []
    for _ in range(runs):
        if cold:
            adb("shell", "am", "force-stop", PKG)
            time.sleep(1.5)
        before = pid()
        ms, out = call()
        times.append(ms)
        print(f"  {label}: {ms:7.1f} ms  pid before={before} after={pid()}  out={json.loads(out)}")
        if not cold:
            time.sleep(0.5)
    print(f"{label}: median {statistics.median(times):.1f} ms over {runs} runs\n")


if __name__ == "__main__":
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 5
    adb("shell", "am", "force-stop", PKG)
    call()  # warm up adb + system paths once, not counted
    run("cold, normal init", n, cold=True, heavy=False)
    run("warm (process alive)", n, cold=False, heavy=False)
    run("cold, heavy init (+800 ms)", n, cold=True, heavy=True)
    set_heavy(False)
