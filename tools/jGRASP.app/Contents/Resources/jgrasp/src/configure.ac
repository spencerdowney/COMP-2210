AC_INIT(jGRASP Wedge and Startup, 1.0, grasp_AT_eng_DOT_auburn_DOT_edu)

cflags_save="$CFLAGS"
AC_PROG_CC
CFLAGS="$cflags_save"

AC_CHECK_LIB(util, openpty)
AC_CHECK_HEADER([util.h], openpty,
      [ LIBS="-lutil $LIBS"
        AC_SUBST(DEFINES, "-DHAVE_UTIL_H")],
      [ AC_SUBST(DEFINES, "")])
AC_PATH_XTRA()

if test "$no_x" != yes ; then
   AC_SUBST(HAVE_X, "yes")
fi

AC_CONFIG_COMMANDS([unused], [chmod +x Make.sh])

AC_CONFIG_FILES([Make.sh])
AC_OUTPUT()
