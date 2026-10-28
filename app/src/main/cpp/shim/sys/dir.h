// Bionic has no <sys/dir.h> (a legacy BSD/glibc alias of <dirent.h>). DeSmuME's bundled
// libfat includes it, so redirect to the standard header. Found first because
// the shim directory is the first include path.
#pragma once
#include <dirent.h>
