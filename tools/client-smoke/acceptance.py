#!/usr/bin/env python3
"""Disposable CI only. Child process groups are the only processes terminated.

MINECRAFT-499 / Wroosbit gap 1: boots a real disposable server and a real disposable client
(headless, via Xvfb) with Layered Slopes installed, has the client join over LAN, and waits for
AcceptanceClient (compiled only with -PclientAcceptance) to report whether the mod actually
classloads on the client and whether its grass-tint color handler fires a real biome color at
render time. Pattern adapted from Dynamic Vehicles' own tools/client-smoke/acceptance.py.
"""
import json
import os
from pathlib import Path
import signal
import subprocess
import time

root = Path(__file__).resolve().parents[2]
server = root / 'run-acceptance-server'
client = root / 'run-acceptance-client'
# Refuse to reuse a world/profile, including any prior failed attempt's state.
for path in (server, client):
    path.mkdir(exist_ok=False)
(server / 'eula.txt').write_text('eula=true\n')
(server / 'server.properties').write_text(
    'server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\nenforce-secure-profile=false\n'
    'level-name=acceptance-disposable\nlevel-type=minecraft:flat\ngamemode=creative\n'
    'spawn-protection=0\nview-distance=4\nsimulation-distance=4\nmax-players=1\n')
(client / 'options.txt').write_text(
    'onboardAccessibility:false\nrenderDistance:4\nsimulationDistance:4\nmaxFps:30\n'
    'pauseOnLostFocus:false\n')
processes = []
logs = []
try:
    def start(task, output, *args):
        log = open(root / output, 'w')
        logs.append(log)
        process = subprocess.Popen(
            ['./gradlew', task, '-PclientAcceptance', '--no-daemon', '-Dorg.gradle.jvmargs=-Xmx1G', *args],
            cwd=root, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        processes.append(process)
        return process

    server_process = start('runServer', 'client-acceptance-server.log')
    deadline = time.monotonic() + 360
    while time.monotonic() < deadline:
        text = (root / 'client-acceptance-server.log').read_text(errors='replace')
        if 'Done (' in text:
            break
        if server_process.poll() is not None:
            raise RuntimeError('server exited before readiness')
        time.sleep(2)
    else:
        raise RuntimeError('server startup timeout')

    client_process = start('runClient', 'client-acceptance-client.log', '-PquickPlayMp=127.0.0.1:25565')
    deadline = time.monotonic() + 600
    while client_process.poll() is None and time.monotonic() < deadline:
        if (client / 'client-acceptance-failure.txt').exists():
            raise RuntimeError((client / 'client-acceptance-failure.txt').read_text())
        time.sleep(5)
    if client_process.poll() is None:
        raise RuntimeError('client acceptance timeout')
    if (client / 'client-acceptance-failure.txt').exists():
        raise RuntimeError((client / 'client-acceptance-failure.txt').read_text())
    result = json.loads((client / 'client-acceptance-result.json').read_text())
    if result.get('passed') is not True:
        raise RuntimeError('client did not report success')
    text = (root / 'client-acceptance-server.log').read_text(errors='replace')
    if 'LAYERED_ACCEPTANCE_SERVER_PASS' not in text:
        raise RuntimeError('server never confirmed its acceptance fixture ran')
    print(json.dumps(result))
finally:
    for process in reversed(processes):
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
    for log in logs:
        log.close()
    for path in (root / 'client-acceptance-client.log', client / 'logs/latest.log',
                 root / 'client-acceptance-server.log'):
        if path.exists():
            print('ACCEPTANCE_DIAGNOSTIC ' + str(path), flush=True)
            print('\n'.join(path.read_text(errors='replace').splitlines()[-100:]), flush=True)
