import sys
sys.path = [".", "lib"] + sys.path
from remote.remote_util import RemoteMachineShellConnection, RemoteUtilHelper
from TestInput import TestInputServer

def build_url(params, server):
    _errors = []
    version = ''
    server = ''
    openssl = ''
    names = []
    url = ''
    direct_build_url = None
    debug_logs = True

    # replace "v" with version
    # replace p with product
    tmp = {}
    for k in params:
        value = params[k]
        if k == "v":
            tmp["version"] = value
        elif k == "p":
            tmp["version"] = value
        else:
            tmp[k] = value
    params = tmp

    ok = True
    if not "version" in params and len(params["version"]) < 5:
        _errors.append(errors["INVALID-PARAMS"])
        ok = False
    else:
        version = params["version"]

    if ok:
        if not "product" in params:
            _errors.append(errors["INVALID-PARAMS"])
            ok = False
    if ok:
        if not "server" in params:
            _errors.append(errors["INVALID-PARAMS"])
            ok = False
        else:
            server = check_server

    if ok:
        if "toy" in params:
            toy = params["toy"]
        else:
            toy = ""

    if ok:
        if "openssl" in params:
            openssl = params["openssl"]

    if ok:
        if "url" in params and params["url"] != ""\
           and isinstance(params["url"], str):
            direct_build_url = params["url"]
    if ok:
        if "linux_repo" in params and params["linux_repo"].lower() == "true":
            linux_repo = True
        else:
            linux_repo = False
    if ok:
        if "msi" in params and params["msi"].lower() == "true":
            msi = True
        else:
            msi = False
    if ok:
        if "debug_logs" in params and params["debug_logs"] in ["False", "false", False]:
            debug_logs = False

    if ok:
        mb_alias = ["membase", "membase-server", "mbs", "mb"]
        cb_alias = ["couchbase", "couchbase-server", "cb"]
        css_alias = ["couchbase-single", "couchbase-single-server", "css"]
        moxi_alias = ["moxi", "moxi-server"]
        cbas_alias = ["cbas", "server-analytics"]

        if params["product"] in cbas_alias:
            names = ['couchbase-server-analytics', 'server-analytics']
        elif params["product"] in mb_alias:
            names = ['membase-server-enterprise', 'membase-server-community']
        elif params["product"] in cb_alias:
            if "type" in params and params["type"].lower() in "couchbase-server-community":
                names = ['couchbase-server-community']
            elif "type" in params and params["type"].lower() in "couchbase-server-enterprise":
                names = ['couchbase-server-enterprise']
            else:
                names = ['couchbase-server-enterprise', 'couchbase-server-community']
        elif params["product"] in css_alias:
            names = ['couchbase-single-server-enterprise', 'couchbase-single-server-community']
        elif params["product"] in moxi_alias:
            names = ['moxi-server']
        else:
            ok = False
            _errors.append(errors["INVALID-PARAMS"])
        if "1" in openssl:
            names = ['couchbase-server-enterprise_centos6', 'couchbase-server-community_centos6', \
                     'couchbase-server-enterprise_ubuntu_1204', 'couchbase-server-community_ubuntu_1204']
        if "toy" in params:
            names = ['couchbase-server-enterprise']

    remote_client = RemoteMachineShellConnection(server)
    info = remote_client.extract_remote_info()
    server_os_type = info.distribution_version
    if info.distribution_type.lower() == "mac":
        macOS_name = info.distribution_version[:5]
        if macOS_name >= "10.10":
            server_os_type = "MacOS: {0} or ".format(MACOS_NAME[macOS_name])\
                                                 + info.distribution_version
        else:
            server_os_type = "MacOS " + info.distribution_version

    print "\n*** OS version of this server {0} is {1} ***"\
                        .format(remote_client.ip, server_os_type)
    if info.distribution_version.lower() == "suse 12":
        if version[:5] not in COUCHBASE_FROM_SPOCK:
            mesg = "%s does not support cb version %s \n" % \
                     (info.distribution_version, version[:5])
            remote_client.stop_current_python_running(mesg)
    if info.type.lower() == "windows":
        if "-" in version:
            msi_build = version.split("-")
            """
                In spock from build 2924 and later release, we only support
                msi installation method on windows
            """
            if "2k8" in info.windows_name:
                info.windows_name = 2008
            if msi_build[0] in COUCHBASE_FROM_SPOCK:
                info.deliverable_type = "msi"
            elif "5" > msi_build[0] and info.windows_name == 2016:
                log.info("\n========\n"
                    "         Build version %s does not support on\n"
                    "         Windows Server 2016\n"
                    "========"  % msi_build[0])
                os.system("ps aux | grep python | grep %d " % os.getpid())
                time.sleep(5)
                os.system('kill %d' % os.getpid())
        else:
            print "Incorrect version format"
            sys.exit()
    remote_client.disconnect()
    if ok and not linux_repo:
        timeout = 300
        if "timeout" in params:
            timeout = int(params["timeout"])
        releases_version = ["1.6.5.4", "1.7.0", "1.7.1", "1.7.1.1", "1.8.0"]
        cb_releases_version = ["1.8.1", "2.0.0", "2.0.1", "2.1.0", "2.1.1", "2.2.0",
                                "2.5.0", "2.5.1", "2.5.2", "3.0.0", "3.0.1", "3.0.2",
                                "3.0.3", "3.1.0", "3.1.1", "3.1.2", "3.1.3", "3.1.5","3.1.6",
                                "4.0.0", "4.0.1", "4.1.0", "4.1.1", "4.1.2", "4.5.0"]
        build_repo = MV_LATESTBUILD_REPO
        if toy is not "":
            build_repo = CB_REPO
        elif "server-analytics" in names:
            build_repo = CB_REPO.replace("couchbase-server", "server-analytics") + CB_VERSION_NAME[version[:3]] + "/"
        elif "moxi-server" in names and version[:5] != "2.5.2":
            """
            moxi repo:
               http://172.23.120.24/builds/latestbuilds/moxi/4.6.0/101/moxi-server..
            """
            build_repo = CB_REPO.replace("couchbase-server", "moxi") + version[:5] + "/"
        elif version[:5] not in COUCHBASE_VERSION_2 and \
             version[:5] not in COUCHBASE_VERSION_3:
            if version[:3] in CB_VERSION_NAME:
                build_repo = CB_REPO + CB_VERSION_NAME[version[:3]] + "/"
            else:
                sys.exit("version is not support yet")
        if 'enable_ipv6' in params and params['enable_ipv6']:
            build_repo = build_repo.replace(CB_DOWNLOAD_SERVER,
                                            CB_DOWNLOAD_SERVER_FQDN)

        for name in names:
            if version[:5] in releases_version:
                build = BuildQuery().find_membase_release_build(
                                         deliverable_type=info.deliverable_type,
                                         os_architecture=info.architecture_type,
                                         build_version=version,
                                         product='membase-server-enterprise')
            elif len(version) > 6 and version[6:].replace("-rel", "") == \
                                                CB_RELEASE_BUILDS[version[:5]]:
                build = BuildQuery().find_couchbase_release_build(
                                        deliverable_type=info.deliverable_type,
                                        os_architecture=info.architecture_type,
                                        build_version=version,
                                        product=name,
                                        os_version = info.distribution_version,
                                        direct_build_url=direct_build_url)
            else:
                builds, changes = BuildQuery().get_all_builds(version=version,
                                  timeout=timeout,
                                  direct_build_url=direct_build_url,
                                  deliverable_type=info.deliverable_type,
                                  architecture_type=info.architecture_type,
                                  edition_type=name,
                                  repo=build_repo, toy=toy,
                                  distribution_version=info.distribution_version.lower(),
                                  distribution_type=info.distribution_type.lower())
                build = BuildQuery().find_build(builds, name, info.deliverable_type,
                                           info.architecture_type, version, toy=toy,
                                 openssl=openssl, direct_build_url=direct_build_url,
                             distribution_version=info.distribution_version.lower(),
                                   distribution_type=info.distribution_type.lower())

            if build:
                if 'amazon' in params:
                    type = info.type.lower()
                    if type == 'windows' and version in releases_version:
                        build.url = build.url.replace("http://builds.hq.northscale.net",
                                                      "https://s3.amazonaws.com/packages.couchbase")
                        build.url = build.url.replace("enterprise", "community")
                        build.name = build.name.replace("enterprise", "community")
                    else:
                        """ since url in S3 insert version into it, we need to put version
                            in like ..latestbuilds/3.0.0/... """
                        cb_version = version[:5]
                        build.url = build.url.replace("http://builds.hq.northscale.net/latestbuilds",
                                    "http://packages.northscale.com/latestbuilds/{0}".format(cb_version))
                        """ test enterprise version """
                        #build.url = build.url.replace("enterprise", "community")
                        #build.name = build.name.replace("enterprise", "community")
                """ check if URL is live """
                url_valid = False
                remote_client = RemoteMachineShellConnection(server)
                url_valid = remote_client.is_url_live(build.url)
                remote_client.disconnect()
                if url_valid:
                    return build
                else:
                    sys.exit("ERROR: URL is not good. Check URL again")
        _errors.append(errors["BUILD-NOT-FOUND"])
    if not linux_repo:
        msg = "unable to find a build for product {0} version {1} for package_type {2}"
        raise Exception(msg.format(names, version, info.deliverable_type))

if __name__ == "__main__":
    params = {'num_nodes': 4, 'product': 'cb', 'fts_query_limit': '10000000', 'version': '5.5.0-2958', 'parallel': 'True'}
    server = TestInputServer("172.23.106.121")
    build_url(params, server)