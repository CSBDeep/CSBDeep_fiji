#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_c8e2e06de7df_key $encrypted_c8e2e06de7df_iv
