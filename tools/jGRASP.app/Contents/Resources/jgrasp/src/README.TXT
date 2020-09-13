The optional native parts of jGRASP are provided
in binary form for 32 and 64 bit Windows, Intel
Linux, and Mac OS X. If you are
using one of those systems, you do not need to
build these sources.

Building the native parts of jGRASP is no longer
required for nearly complete funtionality on any
system. The native wedge will make killing user
programs (using the "End" button in the jGRASP Run
I/O window) more reliable. Without this, you may
rarely need to kill runaway programs or processes
launched by user programs using OS tools (such as
"kill" from the command line). The native startup
shell allows you to pop up files in an already
running instance of jGRASP as
   jGRASP somefile
on systems that have X Windows, and by clicking
on associated files if your OS supports that.
Otherwise, building these native executables will
provide no additional functionality.

Run
   ./configure
followed by
   ./Make.sh
to build the native wedge and startup shell on
UNIX and UNIX-like systems.

Ubuntu (non-Intel) users: if the startup shell doesn't
build and you don't have X headers, you may need to
  sudo apt-get install libxt-dev
Other Linux users: you may also need to get an XT
development package.

Report problems to grasp_AT_eng_DOT_auburn_DOT_edu .
