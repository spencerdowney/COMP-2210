
#include <X11/Intrinsic.h>
#include <X11/Shell.h>
#include <sys/errno.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/* Native client-server single instance shell for jGRASP. */

/* Compile with something like: gcc -L/usr/X11/lib -lX11 linux_exec.c */

extern int errno;

#define NOT_RUNNING 0
#define SENT 1
#define NO_REPLY 2
static int openInExisting(char *filename, char lang, Window window,
      Display *display, int line, int wait);
static int sendMessage(char *command, Window win, Display *display,
      int wait);
static void mainLoop(Display *display, Window window, int childPid,
      int outFd);
static int initGC(Display *display, Window window);
static void draw(Display *display, Window window, XEvent *event);
static void clientMessage(Display *display, XEvent *event, int outFd,
      int childPid);

static Atom GRASP_ACTIVE, GRASP_REPLY, WM_PROTOCOLS, WM_DELETE_WINDOW;

static char *msg = "Closing this window will shut down jGRASP.";
static GC gc_ = NULL;
static int ascent = 20;

static char *usage_ = "\n\nUsage:  %s  <-l line_number>  <-g language>  "
      "<-j jgrasp_home>\n"
      "  <-a jgrasp_java> <-e jgrasp_extensions (deprecated)> "
      "<-cp jgrasp_classpath> <-Jjvm_arg>*\n"
      "  <filename>\n\n"
      "   See the jGRASP HTML documentation on \"Running jGRASP\" for a\n"
      "   description of these flags.\n\n";


int main(int argc, char *argv[]) {
   Display *display;
   int screenNum;
   Screen *screen;
   Window window, owner;
   int pid;
   int pid2;
   char lang = ' ';
   int infd[2];
   int line = -1;
   int i;
   char *jgraspHome = NULL;
   char *javaCommand = NULL;
   char *jgraspClasspath = NULL;
   char *jgraspExtensions = NULL;
   char *filename = NULL;
   char *jgraspHomeFromShell = NULL;
   char *javaCommandFromShell = NULL;
   char *jgraspClasspathFromShell = NULL;
   char *jgraspExtensionsFromShell = NULL;
   char *javaBinDir = NULL;
   int len;
   int last;
   char **javaArgs;
   char **jgraspArgs;
   int numJavaArgs = 0;
   int numJgraspArgs = 0;

   jgraspHome = getenv("JGRASP_HOME");
   javaCommand = getenv("JGRASP_JAVA");
   jgraspClasspath = getenv("JGRASP_CLASSPATH");
   jgraspExtensions = getenv("JGRASP_EXTENSIONS");

   javaArgs = malloc(sizeof(char *) * argc);
   jgraspArgs = malloc(sizeof(char *) * argc);

   /* Process arguments. The startup arguments will override corresponding
      environment variables. */
   for (i = 1; i < argc; i++) {
      last = (i == argc - 1);
      if (!last && !strcmp(argv[i], "-l")) {
         sscanf(argv[++i], "%d", &line);
      }
      else if (!last && !strcmp(argv[i], "-g")) {
         lang = argv[++i][0];
      }
      else if (!last && !strcmp(argv[i], "-j")) {
         jgraspHome = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-a")) {
         javaCommand = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-cp")) {
         jgraspClasspath = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-e")) {
         jgraspExtensions = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-d")) {
         jgraspArgs[numJgraspArgs++] = argv[i];
         jgraspArgs[numJgraspArgs++] = argv[++i];
      }
      else if (!strcmp(argv[i], "-nosplash")) {
         jgraspArgs[numJgraspArgs++] = argv[i];
      }
      else if (!last && !strcmp(argv[i], "-Ij")) {
         jgraspHomeFromShell = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-Ia")) {
         javaCommandFromShell = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-Icp")) {
         jgraspClasspathFromShell = argv[++i];
      }
      else if (!last && !strcmp(argv[i], "-Ie")) {
         jgraspExtensionsFromShell = argv[++i];
      }
      else if ((len = strlen(argv[i])) > 2 && !strncmp(argv[i], "-J", 2)) {
         javaArgs[numJavaArgs++] = argv[i] + 2;
      }
      else if (!last && !strcmp(argv[i], "-IJ")) {
         // Java args until next -IJ.
         while (++i < argc && strcmp(argv[i], "-IJ")) {
            javaArgs[numJavaArgs++] = argv[i];
         }
      }
      else if (!strcmp(argv[i], "-h") || !strcmp(argv[i], "-help")) {
         fprintf(stderr, usage_, argv[0]);
         exit(0);
      }
      else if (last) {
         filename = argv[i];
      }
      else {
         fprintf(stderr, "\n\nBad argument \"%s\" to jgrasp.", argv[i]);
         fprintf(stderr, usage_, argv[0]);
         exit(1);
      }
   }
   /* Startup arguments from our startup shell override the others. */
   if (jgraspHomeFromShell != NULL) {
      jgraspHome = jgraspHomeFromShell;
   }
   if (javaCommandFromShell != NULL) {
      char *tmp;
      javaCommand = javaCommandFromShell;
      javaBinDir = strdup(javaCommand);
      tmp = strrchr(javaBinDir, '/');
      if (tmp != NULL) {
         *tmp = '\0';
      }
      else {
         free(javaBinDir);
         javaBinDir = NULL;
      }
   }
   if (jgraspClasspathFromShell != NULL) {
      jgraspClasspath = jgraspClasspathFromShell;
   }
   if (jgraspExtensionsFromShell != NULL) {
      jgraspExtensions = jgraspExtensionsFromShell;
   }

   if (jgraspExtensions != NULL) {
      jgraspArgs[numJgraspArgs++] = "-e";
      jgraspArgs[numJgraspArgs++] = jgraspExtensions;
   }


   /* Open display. */
   if ((display = XOpenDisplay(NULL)) == NULL) {
      fprintf(stderr,
         	"\n\njGRASP exec error: Unable to open display.\n\n");
      free(javaArgs);
      free(jgraspArgs);
      exit(1);
   }

   /* Get screen info. */
   screenNum = DefaultScreen(display);
   screen = DefaultScreenOfDisplay(display);

   /* Create window. */
   window = XCreateSimpleWindow(display, RootWindow(display, screenNum),
         0, 0, 300, 50, 0, BlackPixel(display, screenNum),
         WhitePixel(display, screenNum));
   XSync(display, False);

   /* Set up to receive client messages. */
   XSelectInput(display, window, SubstructureRedirectMask | ExposureMask);
   XSync(display, False);

   if ((GRASP_ACTIVE = XInternAtom(display, "_JGRASP_ACTIVE", False))
         == None
         || (GRASP_REPLY = XInternAtom(display, "_JGRASP_REPLY", False))
         == None) {
      fprintf(stderr, "\n\njGRASP exec error: Unable to setup"
         " communications.\n\n");
      free(javaArgs);
      free(jgraspArgs);
      exit(1);
   }

   if ((owner = XGetSelectionOwner(display, GRASP_ACTIVE)) != None) {
      int response;
      if (filename == NULL) {
         if ((response = sendMessage("raise:\n", window, display, 10))
               == SENT) {
            free(javaArgs);
            free(jgraspArgs);
            exit(0);
         }
      }
      else if ((response =
            openInExisting(filename, lang, window, display, line, 10))
            == SENT) {
         free(javaArgs);
         free(jgraspArgs);
         exit(0);
      }
      if (response == NO_REPLY) {
         fprintf(stderr, "\n\njGRASP exec: No response from jgrasp "
               "session after 10 seconds.\n"
               "   Taking over session.\n\n");
      }
   }

   if (jgraspHome == NULL) {
      fprintf(stderr, "\n\njGRASP exec error: The directory where jGRASP "
            "is installed must be specified\n"
            "by   -j jgrasp_home   or by a   JGRASP_HOME   environment "
            "variable.\n\n");
      free(javaArgs);
      free(jgraspArgs);
      exit(1);
   }

   XSetSelectionOwner(display, GRASP_ACTIVE, window, CurrentTime);

   if (filename != NULL) { 
      /* Send a message to myself and don't wait for a response. */
      openInExisting(filename, lang, window, display, line, 0);
   }

   if (pipe(infd) == -1) {
      fprintf(stderr, "\n\njGRASP exec error: Unable to open pipe.\n\n");
      free(javaArgs);
      free(jgraspArgs);
      exit(2);
   }

   if ((pid = fork()) < 0) {
      fprintf(stderr,
         	"\n\njGRASP exec error: Unable to fork process.\n\n");
      free(javaArgs);
      free(jgraspArgs);
      exit(1);
   }

   if (pid == 0) { /* Child process - start jGRASP. */
      char linebuf[20];
      struct stat statBuffer;
      char **args;
      int arg;
      int numExtraJgraspArgs;
   
      /* stdin becomes pipe. */
      close(0);
      if (dup(infd[0]) == -1) {
         fprintf(stderr,
            	"\n\njGRASP exec error: Unable to redirect stdin.");
         fprintf(stderr, "   error number %d.\n", errno);
         switch(errno) {
            case EBADF:
               fprintf(stderr, "   File descriptor not valid.\n");
               break;
            case EMFILE:
               fprintf(stderr, "   Too many file descriptors in use.\n");
               break;
         }
         exit(1);
      }
   
      sprintf(linebuf, "%d", line);
   
      if (chdir(jgraspHome) != 0) {
         fprintf(stderr, "\n\njGRASP exec error: Unable to change working "
               "directory to %s.\n\n", jgraspHome);
         exit(1);
      }
   
      if (javaCommand == NULL) {
         javaCommand = "java";
      }
      if (jgraspClasspath == NULL) {
         if (javaCommand[0] == '/') {
            // Full path to java command specified, but not classpath.
            char *tmp;
            jgraspClasspath = malloc(strlen(javaCommand) + 50);
            sprintf(jgraspClasspath, "jgrasp.jar:%s", javaCommand);
            tmp = strrchr(jgraspClasspath, '/');
            sprintf(tmp, "/../lib/tools.jar");
            if (stat(jgraspClasspath + 11, &statBuffer) != 0) {
               // tools.jar not found.
               jgraspClasspath="jgrasp.jar";
            }
         }
         else {
            jgraspClasspath="jgrasp.jar";
         }
      }
   
      numExtraJgraspArgs = (javaBinDir == NULL)?0:2;
      args = malloc(sizeof(char *) * (6 + numJavaArgs + numJgraspArgs +
            numExtraJgraspArgs));
      arg = 0;
      args[arg++] = javaCommand;
      for (i = 0; i < numJavaArgs; i++) {
         args[arg++] = javaArgs[i];
      }
      free(javaArgs);
      args[arg++] = "-classpath";
      args[arg++] = jgraspClasspath;
      args[arg++] = "Grasp";
      args[arg++] = "-i";
      for (i = 0; i < numJgraspArgs; i++) {
         args[arg++] = jgraspArgs[i];
      }
      free(jgraspArgs);
      if (numExtraJgraspArgs == 2) {
         args[arg++] = "-j";
         args[arg++] = javaBinDir;
      }
      args[arg] = NULL;
      while (execvp(javaCommand, args) == -1 && errno == EINTR) {
      }
   
      // Exec failed, try our JRE distribution.
      if (stat("jdk/bin/java", &statBuffer) == 0) {
         args[0] = "jdk/bin/java";
         while (execvp("jdk/bin/java", args) == -1 && errno == EINTR) {
         }
         fprintf(stderr, "\n\njGRASP exec error: errno %d.\n", errno);
      }
      else if (errno == ENOENT) {
         fprintf(stderr, "\n\njGRASP exec error: command \"%s\" not "
            	"found.\n", javaCommand);
      }
      else {
         fprintf(stderr, "\n\njGRASP exec error: errno %d.\n", errno);
      }
      fprintf(stderr, "Unable to run jgrasp.\n\n");
      exit(1);
   }

   /* Parent process. */
   free(javaArgs);
   free(jgraspArgs);

   /* Fork again and exit the parent. */
   if ((pid2 = fork()) < 0) {
      fprintf(stderr, "\n\njGRASP exec error: Unable to fork second "
            "process.\n\n");
      exit(1);
   }

   if (pid2 != 0) {
      exit(0);
   }
   /* Set up delete response. */
   WM_DELETE_WINDOW = XInternAtom(display, "WM_DELETE_WINDOW", False);
   XSetWMProtocols(display, window, &WM_DELETE_WINDOW, 1);
   WM_PROTOCOLS = XInternAtom(display, "WM_PROTOCOLS", False);

   XSetIconName(display, window, "jGRASP exec");
   XStoreName(display, window, "jGRASP exec");
   if (gc_ == NULL) {
      initGC(display, window);
   }
   {
      XWMHints hints;
      hints.flags = StateHint;
      hints.initial_state = IconicState;
      XSetWMHints(display, window, &hints);
   }
   XMapWindow(display, window);
   XIconifyWindow(display, window, screenNum);
   mainLoop(display, window, pid, infd[1]);

   return 0;
}


static void mainLoop(Display *display, Window win, int childPid,
      int outFd) {
   XEvent event;
   int status;

   while (1) {
      while (XPending(display)) {
         XNextEvent(display, &event);
         if (event.type == ClientMessage) {
            clientMessage(display, &event, outFd, childPid);
         }
         else if (event.type == Expose) {
            draw(display, win, &event);
         }
      }
      waitpid(childPid, &status, WNOHANG);
      if (kill(childPid, 0) == 0) {
         usleep(10000);
      }
      else {
         break;  /* jGRASP has died. */
      }
   }
}


static int initGC(Display *display, Window window) {
   XGCValues gcvals;
   XFontStruct *fontStruct;
   gc_ = XCreateGC(display, window, 0, &gcvals);
   if (gc_ == NULL) {
      return 0;
   }
   if ((fontStruct = XLoadQueryFont(display,
         "-*-helvetica-bold-r-*-*-*-120-*-*-*-*-*-1")) == NULL) {
      if ((fontStruct = XLoadQueryFont(display,
            "-*-courier-bold-r-*-*-*-120-*-*-*-*-*-1")) == NULL) {
         if ((fontStruct = XLoadQueryFont(display,
               "-*-*-bold-r-*-*-*-120-*-*-*-*-*-1")) == NULL) {
            fontStruct = XLoadQueryFont(display,
                  "-*-*-*-*-*-*-*-120-*-*-*-*-*-1");
         }
      }
   }
   if (fontStruct) {
      int width;
      XSetFont(display, gc_, fontStruct->fid);
      ascent = fontStruct->ascent;
      width = XTextWidth(fontStruct, msg, strlen(msg));
      XResizeWindow(display, window, width + 10, ascent
            + fontStruct->descent + 10);
      XFreeFont(display, fontStruct);
   }
   return 1;
}


static void draw(Display *display, Window window, XEvent *event) {
   //XExposeEvent *e = (XExposeEvent *)event;
   if (gc_ == NULL) {
      if (!initGC(display, window)) {
         return;
      }
   }
   XDrawString(display, window, gc_, 5, ascent + 5, msg, strlen(msg));
}


static void clientMessage(Display *display, XEvent *event, int outFd,
      int childPid) {
   XClientMessageEvent *e;
   XClientMessageEvent reply;
   Window sender;
   Atom retType;
   int retFmt;
   unsigned long length;
   unsigned long bytesToGo;
   unsigned char *data;
   int i;

   e = (XClientMessageEvent *)event;
   if (e->message_type == WM_PROTOCOLS) {
      if (*((int *)&(e->data)) == WM_DELETE_WINDOW) {
         if (write(outFd, "exit\n", 5) == -1) {
            fprintf(stderr, "jGRASP exec: failed to send exit message "
                  "to jGRASP.\n");
         }
         for (i = 0; i < 5; i++) {
            if (kill(childPid, 0) != 0) {
               break;
            }
            sleep(1);
         }
         if (kill(childPid, 0) == 0) {
            fprintf(stderr, "jGRASP exec: jGRASP still alive after five "
                  "seconds. Sending a hard kill.\n");
            kill(childPid, SIGKILL);
         }
         exit(0);
      }
   }
   else if (e->message_type == GRASP_ACTIVE) {
      /* Request to open a file. */
      sender = (Window)(e->data.l[0]);
      if (XGetWindowProperty(display, sender, GRASP_ACTIVE, 0, 400,
            True, GRASP_ACTIVE, &retType, &retFmt, &length,
            &bytesToGo, &data) == Success) {
         if (length > 0) {
            *((char *)data + (length - 1)) = '\0';
            if (write(outFd, data, length - 1)) {
               fprintf(stderr, "jGRASP exec: failed to send file open "
                     "message to jGRASP.\n");
            }
         }
         XFree((char *)data);
      }
   
      reply.type = ClientMessage;
      reply.send_event = True;
      reply.display = display;
      reply.window = sender;
      reply.message_type = GRASP_REPLY;
      reply.format = 32;
      XSendEvent(display, sender, False, SubstructureRedirectMask,
            (XEvent *)&reply);
   }
}


static int openInExisting(char *filename, char lang, Window win,
      Display *display, int line, int wait) {
   char *command;
   char tmpdir[MAXPATHLEN + 1];
   int result;
   char *fn = NULL;

   if (strchr(filename, '%') != NULL) {
      int len = strlen(filename);
      int i;
      int j = 0;
      fn = filename;
      
      filename = malloc(len * 2 + 1);
      for (i = 0; i < len; i++) {
         if (fn[i] != '%') {
            filename[j++] = fn[i];
         }
         else {
            filename[j++] = '%';
            filename[j++] = ' ';
            filename[j++] = ' ';
            filename[j++] = ' ' + '%';
         }
      }
      filename[j] = '\0';
      fn = filename;
   }

   if (filename[0] == '/') {
      command = malloc(strlen(filename) + 50);
      sprintf(command, "open2:%c%d:%s\n", lang, line, filename);
   }
   else {
      if (!getcwd(tmpdir, MAXPATHLEN)) {
         sprintf(tmpdir, ".");
      }
      command = malloc(strlen(tmpdir) + strlen(filename) + 50);
      sprintf(command, "open2:%c%d:%s/%s\n", lang, line, tmpdir, filename);
   }

   result = sendMessage(command, win, display, wait);

   free(command);
   if (fn != NULL) {
      free(fn);
   }

   return result;
}


static int sendMessage(char *command, Window win, Display *display,
      int wait) {
   Window owner;
   XClientMessageEvent event;
   int reply;
   int sl;
   XEvent event2;

   XChangeProperty(display, win, GRASP_ACTIVE, GRASP_ACTIVE, 8,
      PropModeReplace, (unsigned char *)command, strlen(command) + 1);

   XSync(display, False);
   if ((owner = XGetSelectionOwner(display, GRASP_ACTIVE)) == None) {
      return NOT_RUNNING;
   }
   event.type = ClientMessage;
   event.send_event = True;
   event.display = display;
   event.window = owner;
   event.message_type = GRASP_ACTIVE;
   event.format = 32;
   event.data.l[0] = (long)win;
   if (!XSendEvent(display, owner, False,
         SubstructureRedirectMask, (XEvent *)&event)) {
      return NOT_RUNNING;
   }
   XSync(display, False);
   if (wait == 0) {
      return SENT;
   }
   /* Wait for confirmation. */
   reply = 0;  sl = 0;
   while (reply == 0) {
      while (XPending(display)) {
         XNextEvent(display, &event2);
         if (event2.type != ClientMessage) {
            continue;
         }
         if (event2.xclient.message_type == GRASP_REPLY) {
            reply = 1;
         }
      }
      if (reply == 0) {
         if (sl >= wait) {
            break;
         }
         sleep(1);
         sl++;
      }
   }

   if (reply == 0) {
      return NO_REPLY;
   }
   return SENT;
}
