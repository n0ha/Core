#!/bin/bash
TIME=$(date +%Y%m%d_%H%M%S_%N)
LOG_FILE=publish-testing.log
# Close STDOUT file descriptor
exec 1<&-
# Close STDERR FD
exec 2<&-

# Open STDOUT as $LOG_FILE file for read and write.
exec 1<>$LOG_FILE

# Redirect STDERR to STDOUT
exec 2>&1

echo "start publishing: "$TIME

name_prefix=$1
snapshot_name=odn-$name_prefix

aptly repo remove testing "Name (% *)"
aptly repo add -force-replace=true  testing  debs/
aptly snapshot create $snapshot_name from repo testing
aptly publish drop wheezy-testing

#set not tty for gpg - because gpg needs this when it is working remotely, it will be fixed in aptly version 0.9
/usr/bin/env script -qfc "aptly publish snapshot -passphrase-file="/home/aptly/key.txt" -distribution=wheezy-testing  -architectures=i386,amd64  $snapshot_name"
echo "end publishing"

