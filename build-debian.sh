#!/bin/bash
JRE_URL=https://raspberry.local/java/OpenJDK21U-jre_x64_linux_hotspot_21.0.11_10.tar.gz
DEB_SHARE=debian/usr/share/subsonic-analyzer
BIN_DIR=debian/usr/bin
MAIN_BIN=$BIN_DIR/subsonic-analysis-tool
PY_BIN=$BIN_DIR/subsonic-analysis-service

sudo apt install -y wget tar
rm -rf debian/usr

root=$(pwd)

mkdir -p $DEB_SHARE
mkdir -p debian/usr/bin

mvn clean package

cd $DEB_SHARE
wget -O jre.tar.gz $JRE_URL
tar -zvxf jre.tar.gz
rm jre.tar.gz
mv jdk* jre

cd $root

echo "#!/usr/share/subsonic-analyzer/jre/bin/java -jar" > $MAIN_BIN
cat target/subsonic*.jar >> $MAIN_BIN
chmod +x $MAIN_BIN

mkdir $DEB_SHARE/analyzer
cp analyzer.py $DEB_SHARE/analyzer/

echo "#!/bin/bash
cd /usr/share/subsonic-analyzer/analyzer
source venv/bin/activate
uvicorn $* analyzer:api
" > $PY_BIN

chmod +x $PY_BIN
cd $root
dpkg-deb --root-owner-group --build debian subsonic-analyzer.deb