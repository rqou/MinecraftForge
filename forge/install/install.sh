#!/bin/bash

echo "MinecraftForge Linux Setup Program"
echo 

pushd .. > /dev/null

rm -rf conf 
mkdir conf
cp -r forge/conf/* conf

if [ -f runtime/bin/fernflower.jar ];
then
   mv runtime/bin/fernflower.jar runtime/bin/fernflower.jar-backup
fi

./cleanup.sh
./decompile.sh

if [ -f runtime/bin/fernflower.jar-backup ];
then
   mv runtime/bin/fernflower.jar-backup runtime/bin/fernflower.jar
fi


pushd src > /dev/null
    find . -name *.java -exec sed -i 's/\r//g' \{\} \;
    find ../forge/ -name *.patch -exec sed -i 's/\r//g' \{\} \;

    if [ -f ../jars/bin/minecraft.jar ];
    then
        for i in `find ../forge/patches/minecraft/ -type f`
        do
            patch -p2 -i $i
        done
    fi


    if [ -f ../jars/minecraft_server.jar ];
    then
        for i in `find ../forge/patches/minecraft_server/ -type f`
        do
            patch -p2 -i $i
        done
    fi
popd > /dev/null

cp -r forge/src/* src

# Removed until MCP's UpdateNames Is fixed
#./updatemcp.sh
#./updatenames.sh
./updatemd5.sh