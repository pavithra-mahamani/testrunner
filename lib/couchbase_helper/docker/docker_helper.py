import gevent
from gevent.queue import Queue
from gevent import Greenlet
from lib.gevent import GeventTask
import docker


class DockerManager():
    # start/wake docker instance
    # list running images/containers
    # issue cmd
    # check status
    # stream logs handle errors
    # terminate docker instance when finished or when timeout expires
    def __init__(self):
       self.client = docker.from_env()
       self.container = None

    def list_containers(self):
        for image in self.client.images.list():
            print(image.id)

    def start_container(self, name, env, detach=True, autoremove=True):
        self.container = self.client.containers.run(name, environment=env, detach=detach, autoremove=autoremove)
        return self.container

    def get_container_log(self):
        if self.container:
            return self.container.logs()

    def wait_for_msg_in_log(self, msgs):
        found = []
        for line in self.container.logs(stream=True):
            for msg in msgs:
                if msg in line.decode():
                    print("Msg found ", msg)
                    found.append(msg)
        if len(found) != len(msgs):
            return False
        return True

    def stop_container(self):
        self.container.stop()

    def receive(self, message):
        print(message)
        pong.inbox.put('ping')
        gevent.sleep(0)

class DockerMonitor(GeventTask):
    def receive(self, message):
        print(message)
        ping.inbox.put('pong')
        gevent.sleep(0)

ping = Pinger()
pong = Ponger()

ping.start()
pong.start()

ping.inbox.put('start')
gevent.joinall([ping, pong])

