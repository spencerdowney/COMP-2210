#!/bin/sh

# Creates a desktop launcher for jGRASP.
# This must be run from the jGRASP "bin" directory.

# Follow links in execution path of this shell.
jgraspv_prg=$0
while [ -h "$jgraspv_prg" ]; do
   ls=`/bin/ls -ld "$jgraspv_prg"`
   # Link format in ls output is stuff-> target
   jgraspv_link=`$expr_cmd "$ls" : '.*-> \(.*\)$'`
   if $expr_cmd "$jgraspv_link" : '\/' > /dev/null 2>&1; then
      # Link is a full path.
      jgraspv_prg="$jgraspv_link"
   else
      # Link is relative to source path.
      jgraspv_prg=`dirname "$jgraspv_prg"`/$jgraspv_link
   fi
done

jgraspv_jgrasp_bin_dir=`dirname "$jgraspv_prg"`
cd $jgraspv_jgrasp_bin_dir
jgraspv_jgrasp_bin_dir=`pwd`

FILE="$jgraspv_jgrasp_bin_dir/jGRASP.desktop"
echo '[Desktop Entry]\nEncoding=UTF-8\nVersion=1.0' > $FILE
echo 'Name[en_US]=jGRASP\nGenericName=IDE' >> $FILE
echo "Exec=$jgraspv_jgrasp_bin_dir/jgrasp" >> $FILE
echo 'Terminal=false' >> $FILE
echo "Icon=$jgraspv_jgrasp_bin_dir/../data/gric48.png" >> $FILE
echo 'Type=Application\nCategories=Application;Development;IDE;' >> $FILE
echo 'Comment[en_US]=jGRASP IDE' >> $FILE
chmod 775 $FILE

if [ -d ~/Desktop ];
then
   cp $FILE ~/Desktop/
fi
