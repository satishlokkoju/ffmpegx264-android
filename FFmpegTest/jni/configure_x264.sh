#!/bin/bash
pushd `dirname $0`
. settings.sh

pushd x264

./configure --cross-prefix=arm-linux-androideabi- \
--enable-pic \
--host=arm-linux --enable-static --enable-debug --extra-ldflags="-nostdlib -lc -lm -ldl -lgcc "

popd;popd
