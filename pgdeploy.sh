#!/usr/bin/env bash

source boilerplate.sh
source common.sh
source services_deploy.sh
source hot_storage_shared.sh

path=$1
domain=$2
provided_app_name=$3
component=$4
build_number=$5
deploy_mode=$6 #also known as prefix
component_number=${7-}

startUp ${domain}
space=$(sed "s/.*${datacenter}-//" <<< "$domain")

echo "Starting to deploy "$component

if [ ${deploy_mode} == "dev" ]; then
    domain_tag=-${space}
    colour=green
else
    domain_tag=""
    colour=blue
fi

prefix=${deploy_mode} #todo: prefix is used throughout the property files - change its name to "deploy_mode"
log "component:" "${component}"
log "deploy_mode" "${deploy_mode}"
log "prefix" "${prefix}"
log "domain_tag" "${domain_tag}"
log "colour" "${colour}"

app_name=$(createAppName "${provided_app_name}" "${component}" "${domain_tag}" "${component_number}")
log "app_name" "${app_name}"
if [ ${provided_app_name} == "default" ]; then
    route=${component}${domain_tag}-blue-${deploy_mode}${component_number}
else
    route=${provided_app_name}
fi
log "route" ${route}

ls target

num_of_instances="-i $(getParam $out $component "instances")"
memory_limit="-m $(getParam $out $component "memory")" || memory_limit=""
disk_limit="-k $(getParam $out $component "disk")" || disk_limit=""
echo "Running: cf push ${app_name} -f ${cf_manifest} ${num_of_instances} ${disk_limit} ${memory_limit} -n ${route} -p ${path} --no-start -b ${cf_buildpack}"
cf push ${app_name} -f ${cf_manifest} ${num_of_instances} ${disk_limit} ${memory_limit} -n ${route} -p ${path} --no-start -b ${cf_buildpack}

echo "setEnv"
evaluateArgs ${out} set_env ${component} ${app_name}
setEnvFromSettings "${datacenter}" "${space}" "${component}" "${app_name}"

echo "Starting application.. Good luck!"
cf start ${app_name}
