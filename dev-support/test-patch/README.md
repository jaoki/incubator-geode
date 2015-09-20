test-patch
======================

Overview
-----------------------------
test-patch is to test a patch file attached to a jira ticket to identify any compilation errors, and unit test failures, and more.
The result is reported back to the correspoinding ticket, and if it is all good, the Geode developers can review it.

Preconditions
------------------------------
run geode/compile docker.


How to run test-patch
------------------------------
./dev-support/test-patch/test-patch.sh --patch-dir=${TEST_PATCH_WORK} ${JIRA_NUMBER}

