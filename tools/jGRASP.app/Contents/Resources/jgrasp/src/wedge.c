#if (defined (__GLIBC__) && ((__GLIBC__ > 2) || (__GLIBC__ == 2) && \
      (__GLIBC_MINOR__ >= 1)))
#define GLIBC_21
#endif

#if defined(GLIBC_21) || (defined(ATT) && !defined(__sgi)) || \
      defined(__MVS__) || (defined(SYSV) && defined(i386))
#define USE_USG_PTYS
#endif

#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/errno.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <sys/ioctl.h>
#if defined(__osf__) || (defined(linux) && defined(GLIBC_21)) \
      || defined(__LSB__)
#include <pty.h>
#endif
#ifdef USE_USG_PTYS
#include <sys/stropts.h>
#endif

#if defined(__LSB__) || defined(__osf__) || (defined(__GLIBC__) && !defined(USE_USG_PTYS)) || \
      defined(__FreeBSD__) || defined(__NetBSD__) || defined(__OpenBSD__) || \
      defined(__APPLE__)
#define USE_OPENPTY 1
#if defined(HAVE_UTIL_H) || defined(__APPLE__)
   #include <util.h>
#endif
#endif

#ifndef TIOCSWINSZ
#define TIOCSWINSZ 0x5414
#endif

#ifdef __LSB__
#ifndef i386
__asm__(".symver memcpy,memcpy@GLIBC_2.2.5");
#else
__asm__(".symver fclose,fclose@GLIBC_2.1");
#endif
#endif


static void processEnvironment(char *env, int *addAppletviewerClasspath,
      char **notFoundMessage);
static int openPseudoTerm(int *masterFdOut, int *slaveFdOut);
static int getTtyp(int *masterFdOut, int *slaveFdOut);
static void initPty(int termFd, int noncanonical);
static int safeOpen(const char *path, int oflag);
static int safeRead(int fildes, void *buf, size_t nbyte);
static int safeWrite(int fildes, void *buf, size_t nbyte);
static int safeClose(int fd);
static char *quoteArg(char *arg);
static void printACS(char *cmd, char **args);
static int safeExecvp(const char *file, char *const argv[]);
static void printMessage(FILE *stream, char *message, char *lineStart);
static void cleanPath(char *path);
static ino_t getInode(char *path);
static int check(const char *cmd);
static char *findExe(const char *cmd);
static void setEnv(const char *name, const char *value);
static void showDupError(const char *arg);



#define VERBOSE 128
#define SHOW_CLASSPATH 256
#define NONCANON 1024

#define WTW_ADD_APPLETVIEWER_CLASSPATH 1

/* Change working directory to argv[3], process environment commands
   in argv[1] and run argv[4] with arguments argv[5] argv[6] ...
   argv[argc - 1]. */
int main(int argc, char *argv[]) {
   int masterFd;
   int slaveFd;
   int numRead;
   int totalRead;
   pid_t pid;
   pid_t parentPid;
   pid_t javaPid;
   char buffer[101];
   int flags;
   int verbose;
   int addAppletviewerClasspath;
   char *notFoundMessage;

   // Send output and input flag characters. Space and space indicates
   // no flags.
   fprintf(stderr, "  ");

   if (argc < 5) {
      fprintf(stderr, " ----jGRASP wedge: Wrong number of arguments.\n");
      return 1;
   }

   if (sscanf(argv[2], "%d", &flags) != 1) {
      fprintf(stderr, " ----jGRASP wedge internal error: "
            "bad flags argument to wedge.\n");
      return 4;
   }
   verbose = (flags & VERBOSE) != 0;

   if (chdir(argv[3]) != 0) {
      fprintf(stderr, " ----jGRASP wedge: Unable to change working "
            "directory to \"%s\".\n", argv[3]);
      return 2;
   }

   processEnvironment(argv[1], &addAppletviewerClasspath,
         &notFoundMessage);

   /* Open pseudoterminals for i/o. */
   if (!openPseudoTerm(&masterFd, &slaveFd)) {
      fprintf(stderr, " ----jGRASP wedge: Unable to open pty.\n");
      return 3;
   }
   initPty(masterFd, (flags & NONCANON) != 0);

   /*** Sleep 100ms. Java seems to have trouble if the process
        finished too quickly. */
   usleep(100000);

   parentPid = getpid();
   javaPid = getppid();

   /* fork - child forks target while parent does nothing. */
   pid = fork();
   if (pid == 0) { /* Child process - waiting wedge. */
      int targetPid;
      char *path;
   
      pid = getpid();
   
      /* The "waiting wedge" must not be shut down by Java, or
       * by a write to a dead pipe, or the target process will
       * be stranded. Do this before the fork to avoid the
       * highly unlikely event that the "waiting wedge" is killed
       * and the target starts before there is a chance to do it
       * after the fork. **/
      #ifdef SIGHUP
      signal(SIGHUP, SIG_IGN);
      #endif
      #ifdef SIGQUIT
      signal(SIGQUIT, SIG_IGN);
      #endif
      #ifdef SIGTERM
      signal(SIGTERM, SIG_IGN);
      #endif
      #ifdef SIGPIPE
      signal(SIGPIPE, SIG_IGN);
      #endif
   
      /* fork - parent collects stderr and stdout output from child,
         and feeds stdin to child, while child becomes the executed
         process. */
      targetPid = fork();
   
      if (targetPid != 0) {
         /* Parent process. */
         int haveExitStatus = 0;
         int exitStatus;
         int waitResult;
         int inputClosed = 0;
      
         /* Convert stdin to non-blocking. */
         fcntl(0, F_SETFL, O_NDELAY);
      
         /* Read until parent is dead and pty is empty. */
         while (1) {
            /* Read until pty is empty, an error occurs, or
               about 1000 characters are read.
               Maximum output rate assuming 10ms timer precision
               ~= 1000 chars / .01 sec = 100,000 cps. */
            totalRead = 0;
            while (totalRead < 1000 && (numRead = safeRead(masterFd,
                  buffer, 100)) > 0) {
               /* Feed output to standard error. */
               safeWrite(2, buffer, numRead);
               totalRead += numRead;
            }
         
            /* Feed input to pty. */
            if (!inputClosed) {
               while ((numRead = safeRead(0, buffer, 100)) > 0) {
                  safeWrite(masterFd, buffer, numRead);
               }
               if (numRead == 0) {
                  inputClosed = 1;
                  if ((flags & NONCANON) == 0) {
                     // Note that we have set 4 as the EOF character.
                     buffer[0] = 4;
                     safeWrite(masterFd, buffer, 1);
                  }
               }
            }
         
            if (kill(javaPid, 0) != 0 || kill(parentPid, 0) != 0) {
               int count = 0;
               /* jGRASP died or parent died. */
               /* Soft kill the target process. */
               kill(targetPid, SIGHUP);
               /* Wait about 5 seconds to see if the target process */
               /* will shut down. */
               while (waitpid(targetPid, &exitStatus, WNOHANG) == 0
                     && count++ < 50) {
                  usleep(100000);
               }
               if (waitpid(targetPid, &exitStatus, WNOHANG) == 0) {
                  /* Target process is still alive, kill it the hard
                     way. */
                  kill(targetPid, SIGKILL);
               }
               exit(0);
            }
         
            if ((waitResult = waitpid(targetPid, &exitStatus, WNOHANG))
                  != 0) {
               if (waitResult != -1) {
                  haveExitStatus = 1;
               }
               else {
                  fprintf(stderr, "\n ----jGRASP wedge: error waiting for "
                        "target. Target will be killed.\n"
                        " ----   error # %d \"%s\".\n", errno,
                        strerror(errno));
                  kill(targetPid, SIGKILL);
               }
               break;
            }
         
            /* Prevent busy-wait by sleeping 10ms. */
            usleep(10000);
         }
      
         /* Finish reading. */
         while ((numRead = safeRead(masterFd, buffer, 100)) > 0) {
            /* Feed output to standard error. */
            safeWrite(2, buffer, numRead);
         }
      
      
         fflush(stderr);
      
         if (haveExitStatus) {
            if (WIFEXITED(exitStatus)) {
               int exitCode = WEXITSTATUS(exitStatus);
               if (verbose || exitCode != 0) {
                  if (exitCode < 10) {
                     fprintf(stderr, "\n ----jGRASP wedge: exit code for "
                           "process is %d.\n", exitCode);
                  }
                  else {
                     fprintf(stderr, "\n ----jGRASP wedge: exit code for "
                           "process is %d (0x%X).\n", exitCode, exitCode);
                  }
               }
               else {
                  fprintf(stderr, "\n");
               }
            }
            else if (WIFSIGNALED(exitStatus)) {
               fprintf(stderr, "\n ----jGRASP wedge: process died "
                     "on signal %d.\n", WTERMSIG(exitStatus));
            }
            else {
               fprintf(stderr, "\n ----jGRASP wedge warning: could "
                     "not get exit code for process.\n");
            }
         }
         else {
            fprintf(stderr, "\n ----jGRASP wedge warning: could "
                  "not get exit status for process.\n");
         }
      
         /* Close the ptys. */
         safeClose(masterFd);
         safeClose(slaveFd);
      
         fclose(stderr);
      }
      else {
         /* Child process - will become target. */
         
         /* Restore signal handlers. */
         #ifdef SIGHUP
         signal(SIGHUP, SIG_DFL);
         #endif
         #ifdef SIGQUIT
         signal(SIGQUIT, SIG_DFL);
         #endif
         #ifdef SIGTERM
         signal(SIGTERM, SIG_DFL);
         #endif
         #ifdef SIGPIPE
         signal(SIGPIPE, SIG_DFL);
         #endif
      
         if (verbose) {
            fprintf(stderr, " ----jGRASP wedge: pid for process is %d  "
                  "pids for wedge are %d and %d.\n", getpid(), parentPid,
                  pid);
            if ((flags & SHOW_CLASSPATH) != 0) {
               char *classpath = getenv("CLASSPATH");
               if (classpath == NULL)
                  classpath = "";
               fprintf(stderr, " ----jGRASP wedge: CLASSPATH"
                     " is \"%s\".\n", classpath);
            }
         
            fprintf(stderr, " ----jGRASP wedge: working directory is %s\n",
                  argv[3]);
         }
      
         /* Redirect stdout and stderr and stdout to the pty. */
         close(1);    /* Close stdout. */
         if (dup(slaveFd) == -1) { /* stdout fd becomes slaveFd. */
            showDupError("stdout");
            fflush(stderr);
            fflush(stdout);
            _exit(0);
         }
         close(2);    /* Close stderr. */
         if (dup(slaveFd) == -1) { /* stderr fd becomes slaveFd. */
            showDupError("stderr");
            fflush(stderr);
            fflush(stdout);
            _exit(0);
         }
         /** Redirect stdin to the pty. **/
         close(0);
         if (dup(slaveFd) == -1) {
            showDupError("stdin");
            fflush(stderr);
            fflush(stdout);
            _exit(0);
         }
      
         if (slaveFd > 2) {
            close(slaveFd);
         }
      
         /***initPty_(1);*/
         if (!isatty(1)) {
            fprintf(stderr,
               " ----jGRASP wedge: warning: using a non-pty to collect "
               "output.\n"
               "     Output may not flow smoothly.\n"
               "     Some output may be lost if process does not flush "
               "buffers.\n\n");
         }
         if (addAppletviewerClasspath) {
            int i;
            // Don't add the classpath if the command already contains one.
            for (i = 5; i < argc; i++) {
               if (strcmp(argv[i], "-J-cp") == 0 || strcmp(argv[i],
                     "-J-classpath") == 0) {
                  addAppletviewerClasspath = 0;
                  break;
               }
            }
         }
      
         argv[4] = findExe(argv[4]);
         if (addAppletviewerClasspath) {
            int i;
            char **args;
            char *cp;
            char *classpath = getenv("CLASSPATH");
            if (classpath == NULL) {
               classpath = "";
            }
            classpath = strdup(classpath);
            
            cleanPath(classpath);
         
            if (strlen(classpath) == 0) {
               free(classpath);
               if (verbose) {
                  printACS(argv[4], argv + 5);
               }
               safeExecvp(argv[4], argv + 4);
            }
            else {
               args = malloc((argc - 5 + 4) * sizeof(char *));
               args[0] = argv[4];
               args[1] = "-J-cp";
               cp = malloc((strlen("-J") + strlen(classpath) + 1) *
                     sizeof(char));
               sprintf(cp, "-J");
               strcat(cp, classpath);
               free(classpath);
               args[2] = cp;
               for (i = 5; i < argc; i++) {
                  args[i - 5 + 3] = argv[i];
               }
               args[argc - 5 + 3] = NULL;
               if (verbose) {
                  printACS(argv[4], args + 1);
               }
               safeExecvp(argv[4], args);
            }
         }
         else {
            if (verbose) {
               printACS(argv[4], argv + 5);
            }
            safeExecvp(argv[4], argv + 4);
         }
         /* Execution failed. */
         fprintf(stderr, " ----jGRASP wedge: could not execute  %s\n",
               argv[4]);
         fprintf(stderr, " ----   error number %d.\n", errno);
         switch(errno) {
            case EACCES:
               fprintf(stderr, " ----   \n ----   Access to target "
                     "denied.\n"
                     " ----   \n ----   Check permissions on target.\n");
               break;
            case EPERM:
               fprintf(stderr, " ----   \n ----   Target requires "
                     "superuser access.\n");
               break;
            case E2BIG:
               fprintf(stderr, " ----   \n ----   Argument list is too "
                     "big.\n");
               break;
            case ENOEXEC:
               fprintf(stderr, " ----   \n ----   Target does not appear "
                     "to be an executable for this system.\n");
               break;
            case ENAMETOOLONG:
               fprintf(stderr, " ----   \n ----   Path to target is "
                     "too long.\n");
               break;
            case ENOENT:
               fprintf(stderr, " ----   \n ----   Target does not exist "
                     "or is not on PATH.\n");
               if (notFoundMessage != NULL) {
                  fprintf(stderr, " ----   ");
                  printMessage(stderr, notFoundMessage, " ----   ");
                  fprintf(stderr, "\n"); }
               break;
            case ENOMEM:
               fprintf(stderr, " ----   \n ----   Not enough memory to "
                     "execute target.\n");
               break;
            case ENOTDIR:
               fprintf(stderr, " ----   \n ----   Path to target "
                     "includes a non-directory.\n");
               break;
            case ELOOP:
               fprintf(stderr, " ----   \n ----   Too many symbolic "
                     "links in path to target.\n");
               break;
            case EIO:
               fprintf(stderr, " ----   \n ----   I/O error running "
                     "target.\n");
               break;
            case ENFILE:
               fprintf(stderr, " ----   \n ----   Too many files open on "
                     "system.\n");
               break;
         }    
            
         if (argv[4][0] != '/') {
            path = getenv("PATH");
            if (path == NULL) {
               path = "";
            }
            fprintf(stderr, " ----   \n ----   PATH is \"%s\".\n", path); }
      
         free(argv[4]);
         fflush(stderr);
         fflush(stdout);
         _exit(0);
      
      }
   }
   
   else { /* Parent process - initial wedge. */
      int waitResult, exitStatus;
   
      /* Do nothing, just wait for the child to die. */
      while (1) {
         if ((waitResult = waitpid(pid, &exitStatus, WNOHANG)) != 0) {
            //fprintf(stderr, "\n***wait result %d %d\n", wait_result,
            //      errno);
            if (waitResult == -1) {
               fprintf(stderr, "\n ----jGRASP wedge: error waiting for "
                     "second half of wedge.\n"
                     " ----   Target may remain running.\n"
                     " ----   error # %d \"%s\".\n", errno,
                     strerror(errno));
            }               
            break;
         }
         usleep(10000);
      }
      return 0;
   }

   return 0;
}



static void processEnvironment(char *env, int *addAppletviewerClasspath,
      char **notFoundMessage) {
   int onVal;
   int envLen = strlen(env);
   unsigned int pos = 0;
   char type;
   int valStart;
   unsigned int bufferSize = 1000;
   char *buffer;
   char *currentVal;
   int i;
   char ch;
   char ch2;
   unsigned int endPos;

   *notFoundMessage = NULL;
   *addAppletviewerClasspath = 0;
   if (envLen <= 2) {
      return;
   }
   /* Process enviroment. */
   onVal = 0;
   buffer = (char *)malloc(bufferSize);
   for (i = 0; i < envLen; i++) {
      if (pos + 1 >= bufferSize) {
         bufferSize *= 2;
         buffer = (char *)realloc(buffer, bufferSize);
      }
      ch = env[i];
      if (env[i] == '\\') {
         ch2 = env[++i];
         if (onVal && (ch2 == 'n' || ch2 == 'e')) {
            onVal = 0;
            buffer[pos] = '\0';
            endPos = pos;
            pos = 0;
            if (type == 'i') {
               if (strcmp(buffer, "NOT_FOUND_MESSAGE") == 0) {
                  *notFoundMessage = buffer + valStart;
               }
               else if (strcmp(buffer, "ADD_APPLETVIEWER_CLASSPATH")
                     == 0) {
                  *addAppletviewerClasspath = 1;
               }
            }
            else {
               if (type != 'r') {
                  buffer[valStart - 1] = '\0';
                  currentVal = getenv(buffer);
                  if (currentVal != NULL) {
                     int size = strlen(currentVal);
                     if (bufferSize < size + endPos + 1) {
                        bufferSize = size + endPos + 1;
                        buffer = (char *)realloc(buffer, bufferSize); }
                     if (type == 'a') {
                        memmove(buffer + valStart + size,
                           buffer + valStart, endPos - valStart);
                        memcpy(buffer + valStart, currentVal, size);
                        buffer[endPos + size] = '\0';
                     }
                     else {
                        sprintf(buffer + endPos, "%s", currentVal);
                     }
                  }
               }
               setEnv(buffer, buffer + valStart);
            }
            continue;
         }
         else if (!onVal && (ch2 == 'r' || ch2 == 'p' || ch2 == 'a'
               || ch2 == 'i')) {
            onVal = 1;
            buffer[pos++] = '\0';/*'=';*/
            valStart = pos;
            type = ch2;
            continue;
         }
         else if (ch2 == 'd') {
            ch = '\"';
         }
         else if (ch2 == 'q') {
            ch = '\'';
         }
         else {
            ch = ch2;
         }
      }
      buffer[pos++] = ch;
   }
   free(buffer);
}



/* Open a terminal for unidirectional use. */
static int openPseudoTerm(int *masterFdOut, int *slaveFdOut) {
   int result;
   int masterFd;
   int slaveFd;
   char *slaveName;

#if defined(USE_OPENPTY)
   if (openpty(&masterFd, &slaveFd, NULL, NULL, NULL) == 0) {
      fcntl(masterFd, F_SETFL, O_NDELAY | O_NONBLOCK);
      *masterFdOut = masterFd;
      *slaveFdOut = slaveFd;
      return 1;
   }
#endif
   masterFd = -1;

#if defined(USE_USG_PTYS)
   #ifdef __GLIBC__
   if ((masterFd = getpt()) >= 0) {
      grantpt(masterFd);
      unlockpt(masterFd);
      slaveName = ptsname(masterFd);
      }
   #else
   if ((masterFd = safeOpen("/dev/ptmx", O_RDWR)) > 0) {
      grantpt(masterFd);
      unlockpt(masterFd);
      slaveName = ptsname(masterFd);
   }
   #endif
#elif defined(AIXV3)
   if ((masterFd = safeOpen("/dev/ptc", O_RDWR)) > 0) {
      slaveName = ttyname(masterFd);
   }
#elif defined(__sgi) && (OSMAJORVERSION >= 4)
   slaveName = _getpty(&masterFd, O_RDWR | O_NOCTTY | O_NDELAY, 0600, 0);
   if (slaveName == NULL) {
      masterFd = -1;
   }
#endif

   if (masterFd >= 0) {
      if ((slaveFd = safeOpen(slaveName, O_RDWR)) > 0) {
      #if defined(I_PUSH) && (defined(USE_USG_PTYS) || defined(AIXV3))
         ioctl(slaveFd, I_PUSH, "ptem");
      #endif
      
         fcntl(masterFd, F_SETFL, O_NDELAY | O_NONBLOCK);
         *masterFdOut = masterFd;
         *slaveFdOut = slaveFd;
         return 1;
      }
      /* Failed to open slave. */
      close(masterFd);
   }

   /* Try a ttyp. */
   if (getTtyp(masterFdOut, slaveFdOut)) {
      return 2;
   }

   return 0;
}




/* Open a tty style terminal.
   Returns non-zero on success. */
static char masterName[] = "/dev/ptyxx";
static char slaveName[] = "/dev/ttyxx";
static char index1Chars[] = "pqrstuvwxyz";
static char index2Chars[] = "0123456789abcdef";
static int getTtyp(int *masterFdOut, int *slaveFdOut) {
   int masterFd, slaveFd;
   char *c3, *c4;

   for (c3 = index1Chars; *c3 != 0; c3++) {
      for (c4 = index2Chars; *c4 != 0; c4++) {
         masterName[8] = slaveName[8] = *c3;
         masterName[9] = slaveName[9] = *c4;
         /* Try to open the master. */
         if ((masterFd = safeOpen(masterName, O_RDWR)) >= 0) {
            /* If slave can be opened, we are done. */
            if ((slaveFd = safeOpen(slaveName, O_RDWR)) >= 0) {
            #if defined(I_PUSH) && (defined(USE_USG_PTYS) || defined(AIXV3))
               ioctl(slaveFd, I_PUSH, "ptem");
            #endif
            
               fcntl(masterFd, F_SETFL, O_NDELAY | O_NONBLOCK);
               *masterFdOut = masterFd;
               *slaveFdOut = slaveFd;
               return 1;
            }
         
            /* Slave inaccessible. Close master and keep going. */
            close(masterFd);
         }
      }
   }

   return 0;
}




/* Initialize a terminal to be used for output collection
   only. */
static void initPty(int termFd, int noncanonical) {
   struct termios mode;
   struct winsize winSize;

   if (!isatty(termFd)) { /* No initialization necessary for pipe. */
      return;
   }

   #ifdef TIOCGETA
   ioctl(termFd, TIOCGETA, (char *)&mode);
   #elif defined(TCGETS)
   ioctl(termFd, TCGETS, (char *)&mode);
   #else
   tcgetattr(termFd, &mode);
   #endif        

   if (noncanonical) {
      mode.c_lflag = 0; /* No signal processing. */
   }
   else {
      mode.c_lflag = ICANON;
      mode.c_cc[VEOF] = 4;
   }
   mode.c_oflag = 0; /* No post-processing of output. */
   mode.c_cflag = B9600 | CS8 | CREAD; /* 8 bit chars. */

   #ifdef TIOCSETA
   ioctl(termFd, TIOCSETA, (char *)&mode);
   #elif defined(TCSETS)
   ioctl(termFd, TCSETS, (char *)&mode);
   #else
   tcsetattr(termFd, TCSANOW, &mode);
   #endif

   /* We could use the width of the message window
      instead of 80 characters. */
   winSize.ws_row = (unsigned short)80;
   winSize.ws_col = (unsigned short)10;
   ioctl(termFd, TIOCSWINSZ, (char *)&winSize);
}


static int safeOpen(const char *path, int oflag) {
   int retval;
   while ((retval = open(path, oflag)) == -1 && errno == EINTR) {
   }
   return retval;
}


static int safeRead(int fildes, void *buf, size_t nbyte) {
   int retval;
   while ((retval = read(fildes, buf, nbyte)) == -1 && errno == EINTR) {
   }
   return retval;
}


static int safeWrite(int fildes, void *buf, size_t nbyte) {
   int retval;
   while ((retval = write(fildes, buf, nbyte)) == -1 && errno == EINTR) {
   }
   return retval;
}


static int safeClose(int fd) {
   int retval;
   while ((retval = close(fd)) == -1 && errno == EINTR) {
   }
   return retval;
}


static char *quoteArg(char *arg) {
   char *result = (char *)malloc(strlen(arg) * 2 + 2);
   int pos = 0;
   int len;
   int i;
   
   result[pos++] = '"';
   len = strlen(arg);
   for (i = 0; i < len; i++) {
      char ch = arg[i];
      if (ch == '"' || ch == '\\') {
         result[pos++] = '\\';
      }
      result[pos++] = ch;
   }
   result[pos++] = '"';
   return result;
}


static void printACS(char *cmd, char **args) {
   int a;
 
   fprintf(stderr, " ----jGRASP wedge2: actual command sent [");
   if (strchr(cmd, ' ') == NULL && strchr(cmd, ' ') == NULL) {
      fprintf(stderr, "%s", cmd);
   }
   else {
      char *arg = quoteArg(cmd);
      fprintf(stderr, "%s", arg);
      free(arg);
   }

   for (a = 0; args[a] != NULL; a++) {
      fprintf(stderr, " ");
      if (strchr(args[a], ' ') == NULL && strchr(args[a], ' ') == NULL) {
         fprintf(stderr, "%s", args[a]);
      }
      else {
         char *arg = quoteArg(args[a]);
         fprintf(stderr, "%s", arg);
         free(arg);
      }
   }
   fprintf(stderr, "].\n");

}


static int safeExecvp(const char *file, char *const argv[]) {
   while (execvp(file, argv) == -1 && errno == EINTR) {
   }
   return 1;
}


static void printMessage(FILE *stream, char *message, char *lineStart) {
   int i;
   int messageLen;
 
   messageLen = strlen(message);
   for (i = 0; i < messageLen; i++) {
      if (message[i] == '\\') {
         if (i + 1 < messageLen) {
            i++;
            if (message[i] == 'n') {
               fprintf(stream, "\n%s", lineStart);
            }
            else {
               fprintf(stream, "%c", message[i]);
            }
         }
         else {
            fprintf(stream, "\\");
         }
      }
      else {
         fprintf(stream, "%c", message[i]);
      }
   }
}


static void cleanPath(char *path) {
   char *wd;
   int wdLen;
   char *follower;
   char *leader;
   char *endPath;
   int first;
   int done;
   ino_t wdStat;
   ino_t fStat;
   int haveWdStat;
   
   wd = getcwd(NULL, 0);
   if (wd == NULL) {
      return;
   }
   wdLen = strlen(wd);
   if (wdLen > 0 && wd[wdLen - 1] == '/') {
      wd[wdLen - 1] = '\0';
   }

   follower = path;
   leader = path;
   first = 1;
   done = 0;
   haveWdStat = 0;
   while (!done) {
      char *tmp;
   
      endPath = strchr(leader, ':');
      if (endPath == NULL) {
         endPath = leader + strlen(leader);
         done = 1;
      }
      else {
         *endPath = '\0';
      }
   
      // Eliminate empty and "." directories.
      if (strlen(leader) == 0 || strcmp(leader, ".") == 0) {
         leader = endPath + 1;
         continue;
      }
   
      // Eliminate whitespace directory.
      tmp = leader;
      for (; tmp < endPath; tmp++) {
         if (*tmp > ' ') {
            break;
         }
      }
      if (tmp == endPath) {
         leader = endPath + 1;
         continue;
      }
   
      // Eliminate current directory.
      if (!haveWdStat) {
         wdStat = getInode(wd);
         haveWdStat = 1;
      }
      if (wdStat != 0) {
         fStat = getInode(leader);
         if (fStat == wdStat) {
            leader = endPath + 1;
            continue;
         }
      }
   
      // Copy path.
      if (!first) {
         *follower++ = ':';
      }
      first = 0;
      for (; leader < endPath; leader++) {
         *follower++ = *leader;
      }
      leader = endPath + 1;
   }
   *follower = '\0';

   // Strip double quotes.
   follower = path;
   leader = path;
   while (*leader) {
      if (*leader != '"') {
         *follower++ = *leader;
      }
      leader++;
   }
   *follower = '\0';

   free(wd);
}


static ino_t getInode(char *path) {
   struct stat st;
   if (stat(path, &st) == 0) {
      return st.st_ino;
   }
   return 0;
}


static int check(const char *cmd) {
   struct stat st;

   if (stat(cmd, &st)) {
      return 0;
   }
   if (!S_ISREG(st.st_mode)) {
      return 0;
   }
   return !access(cmd, X_OK);
}


static char *findExe(const char *cmd) {
   char *PATH;
   char *path_buffer;
   char *pp;
   char *pos;
 
   if (cmd[0] == '/') {
      return strdup(cmd);
   }
   PATH = getenv("PATH");
   if (PATH == NULL) {
      return strdup(cmd);
   }
   PATH = strdup(PATH);
      
   pos = PATH;
   path_buffer = (char *)malloc(strlen(PATH) +
         strlen(cmd) + 1);
   while ((pp = strtok(pos, ":")) != NULL) {
      strcpy(path_buffer, pp);
      if (path_buffer[strlen(path_buffer) - 1] != '/') {
         strcat(path_buffer, "/");
      }
      strcat(path_buffer, cmd);
      if (check(path_buffer)) {
         free(PATH);
         return path_buffer;
      }
      pos = NULL;
   }
   free(PATH);
   free(path_buffer);
   return strdup(cmd);
}


static void setEnv(const char *name, const char *value) {
   char *str = malloc(strlen(name) + strlen(value) + 2);
   sprintf(str, "%s=%s", name, value);
   putenv(str);
}


/** Displays errors from dup().
 *
 *  fdname - name of the file descriptor that could not be duped. **/
static void showDupError(const char *fdname) {
   fprintf(stderr, " ----jGRASP wedge: Unable to redirect %s.", fdname);
   fprintf(stderr, " ----   error number %d.\n", errno);
   switch(errno) {
      case EBADF:
         fprintf(stderr, " ----   \n ----   File descriptor not valid.\n"
               " ----   \n ----   That's odd.\n");
         break;
      case EMFILE:
         fprintf(stderr, " ----   \n ----   Too many file descriptors "
               "in use.\n");
         break;
   }
}
