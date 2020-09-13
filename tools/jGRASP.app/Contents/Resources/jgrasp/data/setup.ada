SAe0
SAi0
SAz0
SAf0bash %<CY> -c "gnatmake %<D> -g %<D> %<FLAGS> -D %<DEF_BIN_DIR> -o %<RELE_DEF_BIN_DIR>%<proj_base>%<EXE_EXT> %<REL_MAIN_FILE>"
SAj0%<DEF_SRC_DIR>
SAA0GNU
SAg0gcc %<CY> -c -D %<REL_DEF_BIN_DIR> -o %<RELE_DEF_BIN_DIR>%<base>.o %D -g %D %FLAGS %file
SAk0%<DEF_SRC_DIR>
SAB0GNU
SAh0gcc %<CY> -c -gnatc %<FLAGS> %<file>
SAl0%<DEF_SRC_DIR>
SAC0GNU
SAp0%<DEF_BIN_DIR>%<SEP>%<proj_base>%<EXE_EXT> %<CY> %<ARGS>
SAr0%<DEF_BIN_DIR>
SAD0
SAw0gdb %<CYD> --args %<proj_base>%<EXE_EXT> %<ARGS>
SAy0%<DEF_BIN_DIR>
SAF0
SA10
SA40
SA50
SAI0
SAJ0
SAS0This runs gnatmake through bash, which is necessary on\012Cygwin 1.5 and higher. It does not support flags. On other\012systems, you can switch to the "gnat - generic" environment\012to avoid the extra overhead and gain flags capability.
SAU0PATH+=%<JGRASP_PATHS>%;\012PATH=+%;%<JGRASP_C_PATHS>\012CYGWIN=nodosfilewarning\012GCC_COLORS=\012-Run\012ADD_EXE_PATH==Y\012
cc -c %D -g %D %FLAGS %file
SAk0
SAB0f1-(\\S(?:\\s*\\S)*?):(\\d+):.*
SAh0gcc -c -gnatc %FLAGS %file
SAl0
SAC0f1-(\\S(?:\\s*\\S)*?):(\\d+):.*
SAp0%MAIN_PATH%SEP%main_base %ARGS
SAr0%MAIN_PATH
SAD0
SAw0gdb %main_base %ARGS
SAy0%MAIN_PATH
SAF0
SA10
SA40
SA50
SAI0
SAJ0
SAe0
SAi0
SAz0
SAf0gnatmake %<CY> %<D> -g %<D> %<FLAGS> -D %<REL_DEF_BIN_DIR> -o %<RELE_DEF_BIN_DIR>%<proj_base>%<EXE_EXT> %<REL_MAIN_FILE>
SAj0%<DEF_SRC_DIR>
SAA0GNU
SAg0gcc %<CY> -c -D %<REL_DEF_BIN_DIR> -o %<RELE_DEF_BIN_DIR>%<base>.o %D -g %D %FLAGS %file
SAk0%<DEF_SRC_DIR>
SAB0GNU
SAh0gcc %<CY> -c -gnatc %<FLAGS> %<file>
SAl0%<DEF_SRC_DIR>
SAC0GNU
SAp0%<DEF_BIN_DIR>%<SEP>%<proj_base>%<EXE_EXT> %<CY> %<ARGS>
SAr0%<DEF_BIN_DIR>
SAD0
SAw0gdb %<CYD> --args %<proj_base>%<EXE_EXT> %<ARGS>
SAy0%<DEF_BIN_DIR>
SAF0
SA10
SA40
SA50
SAI0
SAJ0
SAS0
SAU0PATH+=%<JGRASP_PATHS>%;\012PATH=+%;%<JGRASP_C_PATHS>\012CYGWIN=nodosfilewarning\012GCC_COLORS=\012-Run\012ADD_EXE_PATH==Y\012
