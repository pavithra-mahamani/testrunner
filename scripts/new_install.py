#!/usr/bin/env python

import sys

sys.path = [".", "lib"] + sys.path
import threading
import Queue
import time
import random
import install_utils, install_constants
import logging.config
import os
from membase.api.exception import InstallException
import traceback

logging.config.fileConfig("scripts.logging.conf")
log = logging.getLogger()
q = Queue.Queue()


def node_installer(node, install_tasks):
    while True:
        install_task = install_tasks.get()
        if install_task is None:
            break
        else:
            do_install_task(install_task, node)
            install_tasks.task_done()


def on_install_error(install_task, node, err):
    node.queue.empty()
    log.error("Error {0} occurred on {1} during {2}".format(err, node.ip, install_task))


def do_install_task(task, node):
    try:
        if task == "uninstall":
            node.uninstall_cb()
        elif task == "install":
            node.install_cb()
        elif task == "init":
            node.init_cb()
        elif task == "cleanup":
            node.cleanup_cb()
        log.info("Done with %s on %s." % (task, node.ip))
    except Exception as e:
        on_install_error(task, node, e.message)


def validate_install(version):
    log.info("-" * 100)
    for node in install_utils.NodeHelpers:
        node.install_success = False
        if node.rest:
            node_status = node.rest.cluster_status()["nodes"]
            for item in node_status:
                if version in item['version'] and item['status'] == "healthy":
                    node.install_success = True
                    log.info("node:{0}\tversion:{1}\tstatus:{2}\tservices:{3}".format(item['hostname'],
                                                                                      item['version'],
                                                                                      item['status'],
                                                                                      item['services']))
    print_result()


def print_result():
    success = []
    fail = []
    for node in install_utils.NodeHelpers:
        if node.install_success:
            success.append(node.ip)
        elif not node.install_success:
            fail.append(node.ip)
    log.info("-" * 100)
    for _ in fail:
        log.error("INSTALL FAILED ON: \t{0}".format(_))
    log.info("-" * 100)
    for _ in success:
        log.info("INSTALL COMPLETED ON: \t{0}".format(_))
    log.info("-" * 100)


def do_install(params):
    # Per node, spawn one thread, which will process a queue of install tasks
    for server in params["servers"]:
        node_helper = install_utils.get_node_helper(server.ip)
        install_tasks = ["uninstall", "install", "init", "cleanup"]
        q = Queue.Queue()
        for _ in install_tasks:
            q.put(_)
        t = threading.Thread(target=node_installer, args=(node_helper, q))
        t.daemon = True
        t.start()
        node_helper.queue = q
        node_helper.thread = t

    force_stop = start_time + install_constants.INSTALL_TIMEOUT
    for node in install_utils.NodeHelpers:
        try:
            while node.queue.unfinished_tasks and time.time() < force_stop:
                time.sleep(install_constants.INSTALL_POLL_INTERVAL)
            else:
                raise InstallException
        except InstallException:
            if time.time() >= force_stop:
                log.error("INSTALL TIMED OUT AFTER {0}s.VALIDATING..".format(install_constants.INSTALL_TIMEOUT))
                break
    validate_install(params["version"])


def main():
    params = install_utils.process_user_input()
    install_utils.pre_install_steps()
    do_install(params)


if __name__ == "__main__":
    start_time = time.time()
    main()
    end_time = time.time()
    log.info("TOTAL INSTALL TIME = {0} seconds".format(round(end_time - start_time)))
    sys.exit()