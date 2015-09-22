test-patch
======================

Overview
-----------------------------
test-patch is to test a patch file attached to a jira ticket to identify any compilation errors, and unit test failures, and more.
The result is reported back to the correspoinding ticket, and if it is all good, the Geode developers can review it.

Preconditions
------------------------------
run geode/compile docker image. See /dev-tools/docker/compile

TODO
----------------------
wget https://bootstrap.pypa.io/get-pip.py
sudo python get-pip.py
pip install requests




How to run test-patch
------------------------------
./dev-support/test-patch/test-patch.py --patch-dir=${TEST_PATCH_WORK} --jira ${JIRA_NUMBER}

