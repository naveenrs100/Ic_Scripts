#!/bin/bash
#
# Fernando Ipar - 2002 -  fipar@acm.org
# file-system based mutual exclusion lock 
# for shell scripts
# this script is released under the GNU GPL. 
# see the file COPYING for more info, 
# or lynx to http://www.gnu.org

#
# mutex-wait
# this version has active wait
#

# known failure points and other issues: 
# -if  you don't have create permission on the directory of the lock
#   you're trying to use 
# 

LOCKD="/tmp"
MAXCNT=5000 #the most times we loop before we decide it's a deadlock
#and we give up on the lock

# mnemonic return codes (for easy verification in client scripts)
export LOCK_FREE=4 #the lock is free, that is, the file does not exist
export LOCK_BUSY=3 #the lock is busy, that is, the file exists and the owner is running
export LOCK_ORPHAN=0 #the lock is busy, but the owner is no longer on the system

#remove path from the lock's file name, in case the caller mistakenly included it
#and adds $LOCKD as the root for all locks
sanitizeLockName()
{
	lock=$1
	lock=$(basename $1)
	echo $LOCKD/$lock
}

# attempts to obtain the file lock
# __with active wait__
# i write my pid to the file to make
# sure i'm the owner of the lock
# returns 0 on success or 1 on dealock detection
getLock()
{

	lock=$(sanitizeLockName $1 2>/dev/null)
	
	[ -z "$1" ] && {
		echo "usage: getLock <name>">&2
		return 1
	}
	
	
	#maxcnt=${2:-$MAXCNT}
	maxcnt=${MAXCNT}
	gotlock=1
	cnt=0
	
	# Change: AIX requires that the argument to sleep is a positive integer,
	#	so the scale is changed from 6 to 0
	while [ $gotlock -ne 0 ]; do
		while [ -f $lock ]; do
			isOrphan $lock && rm -f $lock
			sleep $( echo "scale=0; ${RANDOM}/10000" | bc)
			cnt=$((cnt+1))
			[ $cnt -ge $maxcnt ] && return 1
		done
		echo $$>$lock
		sleep $( echo "scale=0; ${RANDOM}/100000" | bc)
		[ $(cat $lock 2>/dev/null) -eq $$ ] && gotlock=0
	done

}


# releases the lock
# returns non-zero exit code if the client is not the
# owner of the lock
release()
{

	lock=$(sanitizeLockName $1 2>/dev/null)
	
	[ -z "$1" ] && {
		echo "usage: release <name>">&2
		return 1
	}
	
	[ $(cat $lock) -eq $$ ] && rm -f $lock || return 1
}


# verifies if the given lock exists, and if so, 
# if it is an orphan lock (if the process that acquired
# it is no longer on the system)
isOrphan()
{

	lock=$(sanitizeLockName $1)
	
	[ -f $lock ] || return $LOCK_FREE
	[ -d /proc/$(cat $lock) ] && return $LOCK_BUSY || return $LOCK_ORPHAN
}


# returns the pid of the owner of the given lock (stdout)
# or a non-zero exit code if the lock does not exist
getOwnerPid()
{

	lock=$(sanitizeLockName $1)
	
	[ -f $lock ] && echo $(cat $lock) || return 1
}
