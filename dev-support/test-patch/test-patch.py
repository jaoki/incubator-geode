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

import sys
import argparse
import subprocess


class TestPatch:
    
    def runproc(self, cmd, expected_returncode=0):
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = proc.communicate()
        returncode = proc.returncode,
        if returncode != expected_returncode:
            raise Exception("error on " + cmd)

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

    def checkout(self):
        returncode, stdout, stderr = self.runproc(["git", "status", "-s"])
        if stdout != "":
            raise Exception("ERROR: can't run in a workspace that contains the following modifications")

        # avoid ssh fingerprint check
        returncode, stdout, stderr = self.runproc(["ssh-keyscan", "github.com"])
        with open("~/.ssh/known_hosts", "a") as f:
            f.write(stdout)

        returncode, stdout, stderr = self.runproc(["git", "checkout", "--", "."])
        returncode, stdout, stderr = self.runproc(["git", "clean", "-x", "-f", "-d"])
        returncode, stdout, stderr = self.runproc(["git", "pull"])



    def test(self):
        self.parse_args(sys.argv[1:])

        self.print_banner("Testing patch for " + self.params.jira + ".")
        self.checkout()

TestPatch().test()
