#!/usr/bin/env bash

echo "Start Delete Tenant"

while (( "$#" )); do 
  echo $1 
  shift 
done

echo "End Delete Tenant"
