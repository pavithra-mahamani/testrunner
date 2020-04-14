import docker
from docker import Client
from requests.exceptions import ConnectionError


class DockerManager(object):
    def __init__(self, tag):
        self.tag = tag
        self.environment = None
        self.client = Client(base_url='unix://var/run/docker.sock')
        self.handle = None

    def start(self, env):
        self.environment = env
        try:
            self.client = docker.from_env()
            self.client.cre
            self.handle = self.client.containers.run("cb:latest",
                                                        environment=self.environment,
                                                        detach=True)
        except ConnectionError as e:
            print('Error connecting to docker service, please start/restart it:', e)

    def _list_images(self):
        for image in self.client.images.list():
            print(image.id)

    def check_status(self):
        pass

    def stream_logs(self):
        pass

    def terminate(self):
        pass

class JavaSDKClient(object):
    def __init__(self, server, bucket, params):
        self.server = server
        self.bucket = bucket
        self.params = params
        # 1 docker image per bucket, identified by server_bucket tag
        self.docker_instance = DockerManager(server.ip + '_' + bucket)
        env = self.params_to_environment()
        self.docker_instance.start(env)

    def params_to_environment(self):
        _environment = {}
        _environment["CLUSTER"] = self.server.ip
        _environment["USERNAME"] = self.server.rest_username
        _environment["PASSWORD"] = self.server.rest_password
        _environment["BUCKET"] = self.bucket
        _environment["SCOPE"] = self.params.scope
        _environment["COLLECTION"] = self.params.collection
        _environment["N"] = self.params.num_ops
        _environment["PC"] = self.params.percent_create
        _environment["PU"] = self.params.percent_update
        _environment["PD"] = self.params.percent_delete
        _environment["L"] = self.params.load_pattern
        _environment["DPX"] = self.params.key_prefix
        _environment["DSX"] = self.params.key_suffix
        _environment["DT"] = self.params.json_template
        _environment["O"] = self.params.print_sdk_logs

        return _environment

    def do_ops(self):
        pass

# environment = {"CLUSTER":"172.23.106.121",
#                "BUCKET":"default",
#                "SCOPE":"_default",
#                "COLLECTION":"default",
#                "OP":"verify",
#                "N":1000
#                }
# client = docker.from_env()
# res = client.containers.run("cb:latest", environment=environment,detach=True)
#
#
# blocking_events=["EndpointConnectionFailedEvent", "SelectBucketFailedEvent"]
# #res.wait()
# for line in res.logs(stream=True):
#     print(line.strip())
#     for event in blocking_events:
#         if event in line.decode():
#             print("exiting because of ", event)
#             exit(-1)
