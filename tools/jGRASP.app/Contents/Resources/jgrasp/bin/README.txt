Use jgrasp.exe to start jGRASP on Windows systems.
Note to computer lab administrators: if you have
installed by unzipping rather than using the exe
installer, you should use jgrasp64.exe on 64 bit
systems.

On Linux/UNIX, use the  jgrasp  script in this
directory to start jGRASP. "java" must be on your
system path for the script to work. You can run
create_linux_launcher.sh  in this directory to
create a desktop launcher for jGRASP.

For running from a portable drive on Windows, use
jgrasp_portable.bat  . See the comments in
jgrasp_portable.bat for more instructions on
running from a portable drive.

If you are on a non-Intel Linux system or a UNIX
system, you may want to build
the optional native parts of jGRASP. Doing this
will improve the ability to kill user programs
and make it possible to open associated files in
jGRASP by clicking and to open files from the
command line. To do this, go to the "src"
directory in this installation and follow the
instructions in "README.txt" file there.
