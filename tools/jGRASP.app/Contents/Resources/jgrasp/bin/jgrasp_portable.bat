@rem Portable drive startup file for Windows.
@rem To specifiy a Java installation on the portable drive
@rem on Windows NT/XP/Vista/7/8, you can use the -a flag with
@rem the drive variable. For example, if Java is installed
@rem in "jdk1.8.0_112" at the root of the drive, you would add:
@rem  -a %~d0jdk1.8.0_112\bin\java.exe
@rem
@rem You can also use winconfig_portable.bat to choose the
@rem portable-drive-relative Java installation and other
@rem startup settings.

start jgrasp64 -p auto
