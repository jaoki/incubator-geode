#!/usr/bin/env python 

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import sys, os
import argparse
import subprocess

class TestPatchException(Exception):
    def __init__(self, message):
        super(TestPatchException, self).__init__(message)

class TestPatch:
    
    REPO_HOST = "github.com"
    KNOWN_FILE_LOCATION = os.path.expanduser("~") + "/.ssh/known_hosts"

    def runproc(self, cmd, expected_returncode=0):
        print "Executing: " + (" ".join(cmd))
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = proc.communicate()
        returncode = proc.returncode
        if returncode != expected_returncode:
            raise TestPatchException("return code was expected to be {0} but was {1} on {2}".format(expected_returncode, returncode, " ".join(cmd)))

        return returncode, stdout, stderr

    def print_banner(self, message):
        print ""
        print ""
        print "======================================================================"
        print "======================================================================"
        print message
        print "======================================================================"
        print "======================================================================"
        print ""
        print ""

    def parse_args(self, argv):
        parser = argparse.ArgumentParser(description="test-patch for Geode project")
        parser.add_argument(
                "--jira",
                action="store",
                required=True,
                help="jira ticket number to pickup a patch and test")
        parser.add_argument(
                "--jenkins",
                action="store_true",
                default=False,
                help="Run by Jenkins (runs tests and posts results to JIRA)")
        parser.add_argument(
                "--patch-dir",
                action="store",
                default="/tmp",
                help="The directory for working and output files (default '/tmp')")

        self.params = parser.parse_args(argv)
        pass

    # FIXME maybe not needed
    def repo_exists_in_known_hosts(self):
        with open(self.KNOWN_FILE_LOCATION, "r") as f:
            for line in f:
                if self.REPO_HOST in line:
                    return True
        return False

    # FIXME maybe not needed
    def setup_known_hosts(self):
        """
        avoid ssh fingerprint check for git
        """
        if os.path.exists(self.KNOWN_FILE_LOCATION):
            if repo_exists_in_known_hosts():
                print "{0} exists in {1} file".format(self.REPO_HOST, self.KNOWN_FILE_LOCATION)
                return

            print "{0} file exists".format(self.KNOWN_FILE_LOCATION)
            mode = "a"

        else:
            print "{0} file does not exist".format(self.KNOWN_FILE_LOCATION)
            mode = "w"
            
        curpath = os.path.abspath("~")
        print curpath

        (returncode, stdout, stderr) = self.runproc(["ssh-keyscan", self.REPO_HOST])
        with open(self.KNOWN_FILE_LOCATION, mode) as f:
            f.write(stdout)
        return
    
    def prepare_patch_dir(self):
        if not os.path.exists(self.params.patch_dir):
            os.mkdir(self.params.patch_dir)

        

    def checkout(self):
        returncode, stdout, stderr = self.runproc(["git", "status", "-s"])
        if stdout != "":
            print stdout
            raise TestPatchException("can't run in a workspace that contains the following modifications")

#         self.setup_known_hosts()

        returncode, stdout, stderr = self.runproc(["git", "checkout", "--", "."])
        returncode, stdout, stderr = self.runproc(["git", "clean", "-x", "-f", "-d"])
        returncode, stdout, stderr = self.runproc(["git", "pull"])
        raise Exception()



    def test(self):
        try:
            self.parse_args(sys.argv[1:])

            self.print_banner("Testing patch for " + self.params.jira + ".")
            self.checkout()
        except TestPatchException as e:
            print "[ERROR] " + str(e)
            sys.exit(2)

TestPatch().test()
